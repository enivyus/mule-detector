package com.muledetector.rules;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Streaming versions of the baseline rules (simulator/baseline_rules.py), fed one committed transfer at a
 * time in booking order. Given the same month of data they must flag exactly the same accounts as the batch
 * script; that parity is how we know the stream loses nothing and handles time correctly.
 *
 * Each account is judged as a receiver. SYSTEM (cash vault) and BUSINESS accounts are never judged.
 * Every rule fires at most once per account.
 */
@Component
public class RuleEngine {

    public static final String PASS_THROUGH = "pass_through";
    public static final String FAN_IN = "fan_in";
    public static final String NEW_ACCOUNT_BURST = "new_account_burst";

    static final Duration WINDOW = Duration.ofHours(2);
    static final long PASS_THROUGH_MIN_SATANG = 100_000;     // ignore incoming amounts under 1,000 THB
    static final int FAN_IN_SENDERS = 4;
    static final Duration NEW_ACCOUNT_AGE = Duration.ofDays(30);
    static final long BURST_SATANG_PER_DAY = 2_000_000;      // 20,000 THB in one calendar day
    static final ZoneId BANK_ZONE = ZoneId.of("Asia/Bangkok"); // "a day" and "night" are Bangkok time

    public record Alert(String accountNo, String rule, OffsetDateTime at, long transferId) {}

    private static final class PendingIn {
        final Instant at;
        final long amount;
        long forwarded;
        PendingIn(Instant at, long amount) { this.at = at; this.amount = amount; }
        boolean passedThrough() { return forwarded * 10 >= amount * 9; }  // >= 90%, in integers
    }

    private static final class State {
        final Set<String> rules = new HashSet<>();
        final Deque<PendingIn> pending = new ArrayDeque<>();
        Instant lastOutAt;          // outs booked in the same second as a later incoming still count for it
        long outAtLastOut;
        final Deque<Map.Entry<Instant, String>> senders = new ArrayDeque<>();
        final Map<LocalDate, Long> inflowByDay = new HashMap<>();
    }

    private final Map<String, State> accounts = new HashMap<>();
    private final List<Alert> alerts = new ArrayList<>();
    private long processed;

    public synchronized void onEvent(TransferEvent e) {
        processed++;
        Instant at = e.occurredAt().toInstant();
        if (!"SYSTEM".equals(e.fromAccountType())) {
            outgoing(e.fromAccountNo(), e, at);
        }
        if (!"SYSTEM".equals(e.toAccountType()) && !"BUSINESS".equals(e.toAccountType())) {
            incoming(e.toAccountNo(), e, at);
        }
    }

    private void outgoing(String accountNo, TransferEvent e, Instant at) {
        State s = state(accountNo);
        if (s.rules.contains(PASS_THROUGH)) return;
        if (at.equals(s.lastOutAt)) {
            s.outAtLastOut += e.amountSatang();
        } else {
            s.lastOutAt = at;
            s.outAtLastOut = e.amountSatang();
        }
        for (Iterator<PendingIn> it = s.pending.iterator(); it.hasNext(); ) {
            PendingIn p = it.next();
            if (at.isAfter(p.at.plus(WINDOW))) {
                it.remove();
                continue;
            }
            p.forwarded += e.amountSatang();
            if (p.passedThrough()) {
                raise(accountNo, s, PASS_THROUGH, e);
                s.pending.clear();
                return;
            }
        }
    }

    private void incoming(String accountNo, TransferEvent e, Instant at) {
        State s = state(accountNo);

        // pass_through: >= 90% of an incoming amount leaves within WINDOW (the cash vault counts as a source)
        if (!s.rules.contains(PASS_THROUGH) && e.amountSatang() >= PASS_THROUGH_MIN_SATANG) {
            PendingIn p = new PendingIn(at, e.amountSatang());
            if (at.equals(s.lastOutAt)) p.forwarded = s.outAtLastOut;
            if (p.passedThrough()) raise(accountNo, s, PASS_THROUGH, e);
            else s.pending.add(p);
        }
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

    private State state(String accountNo) {
        return accounts.computeIfAbsent(accountNo, k -> new State());
    }

    private void raise(String accountNo, State s, String rule, TransferEvent e) {
        s.rules.add(rule);
        alerts.add(new Alert(accountNo, rule, e.occurredAt(), e.transferId()));
    }

    public synchronized List<Alert> alerts() { return List.copyOf(alerts); }
    public synchronized long processed() { return processed; }

    /** State lives in memory only, so the listener rebuilds it from the start of the topic. */
    public synchronized void reset() {
        accounts.clear();
        alerts.clear();
        processed = 0;
    }
}
