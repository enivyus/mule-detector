package com.muledetector.core.account;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/** One freeze or unfreeze that changed an account's status. Rows are only ever inserted. */
@Entity
@Table(name = "freeze_audit")
public class FreezeAudit {

    public enum Action { FREEZE, UNFREEZE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Action action;

    @Column(nullable = false, updatable = false)
    private String actor;

    @Column(nullable = false, updatable = false)
    private String reason;

    /** Wall-clock time, also under the sim profile: it records when a person or the engine acted. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected FreezeAudit() {}

    public FreezeAudit(Long accountId, Action action, String actor, String reason) {
        this.accountId = accountId;
        this.action = action;
        this.actor = actor;
        this.reason = reason;
        this.createdAt = OffsetDateTime.now();
    }

    public Action getAction() { return action; }
    public String getActor() { return actor; }
    public String getReason() { return reason; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
