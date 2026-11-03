package com.muledetector.rules;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Streaming versions of the batch rules in simulator/scored_rules.py (which builds on baseline_rules.py),
 * fed one committed transfer at a time in booking order. Given the same month of data they must flag
 * exactly the same accounts as the batch script; that parity is how we know the stream loses nothing and
 * handles time correctly.
 *
 * Each personal account is judged. SYSTEM (cash vault) and BUSINESS accounts are never judged, and every
 * rule fires at most once per account. An account with at least {@link #SCORE_THRESHOLD} different rules
 * is a suspected mule: one rule alone is a common coincidence, two independent signals rarely are.
 */
@Component
public class RuleEngine {

    public static final String PASS_THROUGH = "pass_through";
    public static final String FAN_IN = "fan_in";
    public static final String NEW_ACCOUNT_BURST = "new_account_burst";
    public static final String FAN_OUT = "fan_out";
    public static final String ODD_HOURS = "odd_hours";
    public static final int SCORE_THRESHOLD = 2;

    static final Duration WINDOW = Duration.ofHours(2);
    static final long LARGE_INCOMING_SATANG = 100_000;       // pass_through and fan_out ignore anything under 1,000 THB
    static final int FAN_IN_SENDERS = 4;
    static final Duration NEW_ACCOUNT_AGE = Duration.ofDays(30);
    static final long BURST_SATANG_PER_DAY = 2_000_000;      // 20,000 THB in one calendar day
    static final Duration FAN_OUT_WINDOW = Duration.ofHours(1);
    static final int FAN_OUT_RECIPIENTS = 2;
    static final int NIGHT_END_HOUR = 5;                     // 00:00-04:59
    static final int NIGHT_TRANSFERS = 2;
    static final ZoneId BANK_ZONE = ZoneId.of("Asia/Bangkok"); // "a day" and "night" are Bangkok time

    public record Alert(String accountNo, String rule, OffsetDateTime at, long transferId) {}

    /** An incoming amount still inside its window, waiting to see where the money goes. */
    private static final class Pending {
        final Instant at;
        final long amount;
        long forwarded;
        final Set<String> recipients = new HashSet<>();
        Pending(Instant at, long amount) { this.at = at; this.amount = amount; }
        boolean passedThrough() { return forwarded * 10 >= amount * 9; }  // >= 90%, in integers
    }

    private static final class State {
        final Set<String> rules = new LinkedHashSet<>();
        final Deque<Pending> passThrough = new ArrayDeque<>();
        final Deque<Pending> fanOut = new ArrayDeque<>();
        // Outgoing booked in the same second as a later incoming still counts for it (the batch rules compare <=)
        Instant lastOutAt;
        long outAtLastOut;
        final Set<String> recipientsAtLastOut = new HashSet<>();
        final Deque<Map.Entry<Instant, String>> senders = new ArrayDeque<>();
        final Map<LocalDate, Long> inflowByDay = new HashMap<>();
        int nightTransfers;
    }

    private final Map<String, State> accounts = new HashMap<>();
    private final List<Alert> alerts = new ArrayList<>();
    private long processed;

    public synchronized void onEvent(TransferEvent e) {
        processed++;
        Instant at = e.occurredAt().toInstant();
        if (judged(e.fromAccountType())) outgoing(e.fromAccountNo(), e, at);
        if (judged(e.toAccountType())) incoming(e.toAccountNo(), e, at);
    }

    private static boolean judged(String accountType) {
        return !"SYSTEM".equals(accountType) && !"BUSINESS".equals(accountType);
    }

    private void outgoing(String accountNo, TransferEvent e, Instant at) {
        State s = state(accountNo);
        boolean toPerson = judged(e.toAccountType());
        if (!at.equals(s.lastOutAt)) {
            s.lastOutAt = at;
            s.outAtLastOut = 0;
            s.recipientsAtLastOut.clear();
        }
        s.outAtLastOut += e.amountSatang();
        if (toPerson) s.recipientsAtLastOut.add(e.toAccountNo());

        if (!s.rules.contains(PASS_THROUGH)) {
            for (Iterator<Pending> it = s.passThrough.iterator(); it.hasNext(); ) {
                Pending p = it.next();
                if (at.isAfter(p.at.plus(WINDOW))) { it.remove(); continue; }
                p.forwarded += e.amountSatang();
                if (p.passedThrough()) { raise(accountNo, s, PASS_THROUGH, e); s.passThrough.clear(); break; }
            }
        }
        if (toPerson && !s.rules.contains(FAN_OUT)) {
            for (Iterator<Pending> it = s.fanOut.iterator(); it.hasNext(); ) {
                Pending p = it.next();
                if (at.isAfter(p.at.plus(FAN_OUT_WINDOW))) { it.remove(); continue; }
                p.recipients.add(e.toAccountNo());
                if (p.recipients.size() >= FAN_OUT_RECIPIENTS) { raise(accountNo, s, FAN_OUT, e); s.fanOut.clear(); break; }
            }
        }
        countNight(accountNo, s, e);
    }

    private void incoming(String accountNo, TransferEvent e, Instant at) {
        State s = state(accountNo);
        // Expired entries can no longer fire; drop them here too, or an account that only receives keeps them forever
        s.passThrough.removeIf(p -> at.isAfter(p.at.plus(WINDOW)));
        s.fanOut.removeIf(p -> at.isAfter(p.at.plus(FAN_OUT_WINDOW)));
        boolean sameSecondAsLastOut = at.equals(s.lastOutAt);

        if (e.amountSatang() >= LARGE_INCOMING_SATANG) {
            // pass_through: >= 90% of an incoming amount leaves within WINDOW (the cash vault counts as a source)
            if (!s.rules.contains(PASS_THROUGH)) {
                Pending p = new Pending(at, e.amountSatang());
                if (sameSecondAsLastOut) p.forwarded = s.outAtLastOut;
                if (p.passedThrough()) raise(accountNo, s, PASS_THROUGH, e);
                else s.passThrough.add(p);
            }
            // fan_out: followed within FAN_OUT_WINDOW by transfers to >= FAN_OUT_RECIPIENTS different people
            if (!s.rules.contains(FAN_OUT)) {
                Pending p = new Pending(at, e.amountSatang());
                if (sameSecondAsLastOut) p.recipients.addAll(s.recipientsAtLastOut);
                if (p.recipients.size() >= FAN_OUT_RECIPIENTS) raise(accountNo, s, FAN_OUT, e);
                else s.fanOut.add(p);
            }
        }
        countNight(accountNo, s, e);
        if (e.fromSystem()) return;  // the account's own cash deposits are not transfers from other people

        // fan_in: >= FAN_IN_SENDERS distinct senders within any WINDOW
        if (!s.rules.contains(FAN_IN)) {
            s.senders.addLast(Map.entry(at, e.fromAccountNo()));
            while (s.senders.getFirst().getKey().isBefore(at.minus(WINDOW))) s.senders.removeFirst();
            if (s.senders.stream().map(Map.Entry::getValue).distinct().count() >= FAN_IN_SENDERS) {
                raise(accountNo, s, FAN_IN, e);
            }
        }

        // new_account_burst: younger than NEW_ACCOUNT_AGE and >= BURST_SATANG_PER_DAY received in one Bangkok day
        if (!s.rules.contains(NEW_ACCOUNT_BURST) && e.toOpenedAt() != null
                && Duration.between(e.toOpenedAt().toInstant(), at).compareTo(NEW_ACCOUNT_AGE) < 0) {
            LocalDate day = e.occurredAt().atZoneSameInstant(BANK_ZONE).toLocalDate();
            if (s.inflowByDay.merge(day, e.amountSatang(), Long::sum) >= BURST_SATANG_PER_DAY) {
                raise(accountNo, s, NEW_ACCOUNT_BURST, e);
            }
        }
    }

    /** odd_hours: >= NIGHT_TRANSFERS transfers in or out between 00:00 and 05:00. */
    private void countNight(String accountNo, State s, TransferEvent e) {
        if (s.rules.contains(ODD_HOURS) || e.occurredAt().atZoneSameInstant(BANK_ZONE).getHour() >= NIGHT_END_HOUR) return;
        if (++s.nightTransfers >= NIGHT_TRANSFERS) raise(accountNo, s, ODD_HOURS, e);
    }

    private State state(String accountNo) {
        return accounts.computeIfAbsent(accountNo, k -> new State());
    }

    private void raise(String accountNo, State s, String rule, TransferEvent e) {
        s.rules.add(rule);
        alerts.add(new Alert(accountNo, rule, e.occurredAt(), e.transferId()));
    }

    public synchronized List<Alert> alerts() { return List.copyOf(alerts); }
    public synchronized long processed() { return processed; }

    /** Suspected mules: account number to the rules it broke, for accounts at or above the score threshold. */
    public synchronized Map<String, Set<String>> suspects() {
        Map<String, Set<String>> out = new TreeMap<>();
        accounts.forEach((no, s) -> { if (s.rules.size() >= SCORE_THRESHOLD) out.put(no, Set.copyOf(s.rules)); });
        return out;
    }

    /** State lives in memory only, so the listener rebuilds it from the start of the topic. */
    public synchronized void reset() {
        accounts.clear();
        alerts.clear();
        processed = 0;
    }
}
