package com.muledetector.rules;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
public class AlertController {

    private final RuleEngine engine;
    private final ObjectProvider<AutoFreeze> autoFreeze;

    public AlertController(RuleEngine engine, ObjectProvider<AutoFreeze> autoFreeze) {
        this.engine = engine;
        this.autoFreeze = autoFreeze;
    }

    /** Accounts frozen automatically since this start (empty unless the autofreeze profile is on). */
    @GetMapping("/freezes")
    public List<AutoFreeze.Freeze> freezes() {
        AutoFreeze f = autoFreeze.getIfAvailable();
        return f == null ? List.of() : f.freezes();
    }

    @GetMapping("/alerts")
    public List<RuleEngine.Alert> alerts() {
        return engine.alerts();
    }

    /** Suspected mules (at least RuleEngine.SCORE_THRESHOLD different rules) and the rules each one broke. */
    @GetMapping("/suspects")
    public Map<String, Set<String>> suspects() {
        return engine.suspects();
    }

    @GetMapping("/stats")
    public Map<String, Long> stats() {
        List<RuleEngine.Alert> alerts = engine.alerts();
        return Map.of("processedEvents", engine.processed(),
                "alerts", (long) alerts.size(),
                "flaggedAccounts", alerts.stream().map(RuleEngine.Alert::accountNo).distinct().count(),
                "suspects", (long) engine.suspects().size());
    }
}
