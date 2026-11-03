package com.muledetector.core.account;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    /** BUSINESS: merchants and employers. Like CUSTOMER it can never go below zero; only SYSTEM can. */
    public enum Type { CUSTOMER, BUSINESS, SYSTEM }
    public enum Status { ACTIVE, FROZEN }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_no", nullable = false, unique = true)
    private String accountNo;

    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type = Type.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    /** Cached balance in satang. The ledger is the source of truth; this is kept in sync in the same DB transaction. */
    @Column(name = "balance_satang", nullable = false)
    private long balanceSatang;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Account() {}

    public Account(String accountNo, String ownerName, Type type, OffsetDateTime createdAt) {
        this.type = type;
        this.createdAt = createdAt;
        this.accountNo = accountNo;
        this.ownerName = ownerName;
    }

    public boolean isSystem() { return type == Type.SYSTEM; }
    public boolean isActive() { return status == Status.ACTIVE; }

    public void debit(long amount) { this.balanceSatang -= amount; }
    public void credit(long amount) { this.balanceSatang += amount; }
    public void freeze() { this.status = Status.FROZEN; }
    public void unfreeze() { this.status = Status.ACTIVE; }

    public Long getId() { return id; }
    public String getAccountNo() { return accountNo; }
    public String getOwnerName() { return ownerName; }
    public Type getType() { return type; }
    public Status getStatus() { return status; }
    public long getBalanceSatang() { return balanceSatang; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
