package com.muledetector.rules;

import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static com.muledetector.rules.RuleEngine.*;
import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    static final String VAULT = "0000000000";
    static final OffsetDateTime OLD = OffsetDateTime.parse("2020-01-01T00:00:00+07:00");
    static final OffsetDateTime T0 = OffsetDateTime.parse("2026-10-10T12:00:00+07:00");

    final RuleEngine engine = new RuleEngine();
    long nextId = 1;

    void transfer(String from, String to, long satang, OffsetDateTime at) {
        transfer(from, to, satang, at, OLD, "CUSTOMER");
    }

    void transfer(String from, String to, long satang, OffsetDateTime at, OffsetDateTime toOpenedAt, String toType) {
        engine.onEvent(new TransferEvent(nextId++, from, to, satang, at,
                VAULT.equals(from) ? "SYSTEM" : "CUSTOMER", VAULT.equals(to) ? "SYSTEM" : toType, toOpenedAt));
    }

    List<String> rulesFor(String account) {
        return engine.alerts().stream().filter(a -> a.accountNo().equals(account)).map(Alert::rule).toList();
    }

    @Test
    void passThroughAtNinetyPercentWithinTwoHours() {
        transfer("V1", "M", 1_000_000, T0);
        transfer("M", "X", 500_000, T0.plusMinutes(10));
        transfer("M", VAULT, 400_000, T0.plusHours(2));  // withdrawal, exactly at the window edge
        assertThat(rulesFor("M")).containsExactly(PASS_THROUGH);
    }

    @Test
    void noPassThroughBelowNinetyPercentOrAfterWindow() {
        transfer("V1", "A", 1_000_000, T0);
        transfer("A", "X", 899_999, T0.plusMinutes(5));
        transfer("V1", "B", 1_000_000, T0);
        transfer("B", "X", 1_000_000, T0.plusHours(2).plusSeconds(1));
        assertThat(rulesFor("A")).isEmpty();
        assertThat(rulesFor("B")).isEmpty();
    }

    @Test
    void passThroughCountsOutgoingBookedInTheSameSecondBeforeTheIncoming() {
        transfer(VAULT, "M", 5_000_000, OLD);           // existing balance
        transfer("M", "X", 1_000_000, T0);
        transfer("V1", "M", 1_000_000, T0);
        assertThat(rulesFor("M")).containsExactly(PASS_THROUGH);
    }

    @Test
    void smallIncomingAmountsNeverPassThrough() {
        transfer("V1", "M", 99_999, T0);
        transfer("M", "X", 99_999, T0.plusMinutes(1));
        assertThat(rulesFor("M")).isEmpty();
    }

    @Test
    void fanInNeedsFourDistinctSendersWithinTwoHours() {
        transfer("S1", "M", 100, T0);
        transfer("S2", "M", 100, T0.plusMinutes(30));
        transfer("S2", "M", 100, T0.plusMinutes(40));
        transfer(VAULT, "M", 100, T0.plusMinutes(50));  // own cash deposit: not a sender
        transfer("S3", "M", 100, T0.plusHours(1));
        assertThat(rulesFor("M")).isEmpty();
        transfer("S4", "M", 100, T0.plusHours(2));      // S1 is exactly 2h earlier: still inside
        assertThat(rulesFor("M")).containsExactly(FAN_IN);
    }

    @Test
    void fanInWindowSlides() {
        transfer("S1", "M", 100, T0);
        transfer("S2", "M", 100, T0.plusMinutes(1));
        transfer("S3", "M", 100, T0.plusMinutes(2));
        transfer("S4", "M", 100, T0.plusHours(2).plusSeconds(1));  // S1 has left the window
        assertThat(rulesFor("M")).isEmpty();
    }

    @Test
    void newAccountBurstUsesBangkokCalendarDays() {
        OffsetDateTime opened = OffsetDateTime.parse("2026-10-01T00:00:00+07:00");
        OffsetDateTime lateEvening = OffsetDateTime.parse("2026-10-05T23:30:00+07:00");
        // 15,000 THB on each side of Bangkok midnight: two days, no burst (in UTC both fall on 5 Oct)
        transfer("S1", "N", 1_500_000, lateEvening, opened, "CUSTOMER");
        transfer("S2", "N", 1_500_000, lateEvening.plusHours(1), opened, "CUSTOMER");
        assertThat(rulesFor("N")).isEmpty();
        transfer("S3", "N", 500_000, lateEvening.plusHours(2), opened, "CUSTOMER");
        // 00:30 and 01:30 are also two night-time transfers
        assertThat(rulesFor("N")).containsExactlyInAnyOrder(NEW_ACCOUNT_BURST, ODD_HOURS);
    }

    @Test
    void fanOutToTwoPeopleWithinAnHour() {
        transfer("V1", "M", 1_000_000, T0);
        transfer("M", "P1", 300_000, T0.plusMinutes(10));
        transfer("M", "SHOP", 100_000, T0.plusMinutes(20), OLD, "BUSINESS");  // paying a shop is not fanning out
        transfer("M", VAULT, 100_000, T0.plusMinutes(30));                      // nor is a cash withdrawal
        assertThat(rulesFor("M")).isEmpty();
        transfer("M", "P2", 300_000, T0.plusHours(1));                          // exactly at the window edge
        assertThat(rulesFor("M")).containsExactly(FAN_OUT);
    }

    @Test
    void fanOutCountsTransfersBookedInTheSameSecondBeforeTheIncoming() {
        transfer(VAULT, "M", 5_000_000, OLD);           // existing balance
        transfer("M", "P1", 300_000, T0);
        transfer("M", "P2", 300_000, T0);
        transfer("V1", "M", 1_000_000, T0);
        assertThat(rulesFor("M")).contains(FAN_OUT);
    }

    @Test
    void noFanOutAfterTheWindow() {
        transfer("V1", "M", 1_000_000, T0);
        transfer("M", "P1", 300_000, T0.plusMinutes(10));
        transfer("M", "P2", 300_000, T0.plusHours(1).plusSeconds(1));
        assertThat(rulesFor("M")).isEmpty();
    }

    @Test
    void oddHoursNeedsTwoNightTransfersInOrOut() {
        OffsetDateTime night = OffsetDateTime.parse("2026-10-10T02:00:00+07:00");
        transfer("V1", "M", 1_000, night);
        transfer("M", "X", 500, night.plusHours(3));  // 05:00 is morning
        assertThat(rulesFor("M")).isEmpty();
        transfer("M", "X", 500, night.plusHours(2).plusMinutes(59));
        assertThat(rulesFor("M")).containsExactly(ODD_HOURS);
    }

    @Test
    void suspectsNeedAtLeastTwoDifferentRules() {
        transfer("V1", "ONE", 1_000_000, T0);
        transfer("ONE", "X", 950_000, T0.plusMinutes(5));                      // pass_through only
        OffsetDateTime night = OffsetDateTime.parse("2026-10-10T01:00:00+07:00");
        transfer("V2", "TWO", 1_000_000, night);
        transfer("TWO", "X", 950_000, night.plusMinutes(5));                  // pass_through + odd_hours
        assertThat(engine.suspects()).containsOnlyKeys("TWO");
        assertThat(engine.suspects().get("TWO")).containsExactlyInAnyOrder(PASS_THROUGH, ODD_HOURS);
    }

    @Test
    void newAccountBurstIgnoresOldAccountsAndOwnDeposits() {
        OffsetDateTime opened = T0.minusDays(30);  // exactly 30 days old: no longer new
        transfer("S1", "O", 5_000_000, T0, opened, "CUSTOMER");
        OffsetDateTime fresh = T0.minusDays(3);
        transfer(VAULT, "F", 5_000_000, T0, fresh, "CUSTOMER");
        assertThat(rulesFor("O")).isEmpty();
        assertThat(rulesFor("F")).isEmpty();
    }

    @Test
    void businessAccountsAreNeverJudged() {
        for (int i = 1; i <= 5; i++) transfer("S" + i, "SHOP", 3_000_000, T0.plusMinutes(i), T0.minusDays(1), "BUSINESS");
        assertThat(rulesFor("SHOP")).isEmpty();
        assertThat(engine.processed()).isEqualTo(5);
    }
}
