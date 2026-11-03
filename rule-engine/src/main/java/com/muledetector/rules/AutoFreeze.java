package com.muledetector.rules;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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

    /** Logs in as core-banking's "rule-engine" user, whose role may freeze but never unfreeze. */
    public AutoFreeze(RestClient.Builder http, @Value("${mulebank.core-banking-url}") String coreBankingUrl,
                      @Value("${mulebank.core-banking-password}") String password) {
        this.bank = http.baseUrl(coreBankingUrl).defaultHeaders(h -> h.setBasicAuth("rule-engine", password)).build();
    }

    /** Never throws: an exception would make Kafka redeliver the record and the engine count it twice. */
    public synchronized void freeze(String accountNo, OffsetDateTime at, Collection<String> rules) {
        boolean ok = true;
        try {
            // The reason lands in core-banking's audit log, so an investigator sees why without asking this service
            String reason = "Auto-freeze: " + String.join(", ", new TreeSet<>(rules));
            bank.post().uri("/api/admin/accounts/{no}/freeze", accountNo).body(Map.of("reason", reason))
                    .retrieve().toBodilessEntity();
            log.warn("Auto-froze account {} (reached {} rules)", accountNo, RuleEngine.FREEZE_THRESHOLD);
        } catch (RuntimeException e) {
            ok = false;
            log.error("Could not freeze account {}; it stays on the review queue", accountNo, e);
        }
        freezes.add(new Freeze(accountNo, at, ok));
    }

    public synchronized List<Freeze> freezes() { return List.copyOf(freezes); }
}
