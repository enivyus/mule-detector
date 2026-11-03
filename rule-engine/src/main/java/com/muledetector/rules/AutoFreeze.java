package com.muledetector.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Freezes an account in core-banking the moment it reaches RuleEngine.FREEZE_THRESHOLD rules, so money still
 * in it cannot be cashed out. Off by default (profile "autofreeze"): during a replay a frozen account would make
 * later simulated transfers fail and the bank would no longer match the CSV.
 */
@Component
@Profile("autofreeze")
public class AutoFreeze {

    public record Freeze(String accountNo, OffsetDateTime at, boolean ok) {}

    private static final Logger log = LoggerFactory.getLogger(AutoFreeze.class);

    private final RestClient bank;
    private final List<Freeze> freezes = new ArrayList<>();

    public AutoFreeze(RestClient.Builder http, @Value("${mulebank.core-banking-url}") String coreBankingUrl) {
        this.bank = http.baseUrl(coreBankingUrl).build();
    }

    /** Never throws: an exception would make Kafka redeliver the record and the engine count it twice. */
    public synchronized void freeze(String accountNo, OffsetDateTime at) {
        boolean ok = true;
        try {
            bank.post().uri("/api/admin/accounts/{no}/freeze", accountNo).retrieve().toBodilessEntity();
            log.warn("Auto-froze account {} (reached {} rules)", accountNo, RuleEngine.FREEZE_THRESHOLD);
        } catch (RuntimeException e) {
            ok = false;
            log.error("Could not freeze account {}; it stays on the review queue", accountNo, e);
        }
        freezes.add(new Freeze(accountNo, at, ok));
    }

    public synchronized List<Freeze> freezes() { return List.copyOf(freezes); }
}
