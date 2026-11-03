package com.muledetector.core.ledger;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    public enum Direction { DEBIT, CREDIT }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_id", nullable = false)
    private Long transferId;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Direction direction;

    @Column(name = "amount_satang", nullable = false)
    private long amountSatang;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected LedgerEntry() {}

    public LedgerEntry(Long transferId, Long accountId, Direction direction, long amountSatang, OffsetDateTime createdAt) {
        this.createdAt = createdAt;
        this.transferId = transferId;
        this.accountId = accountId;
        this.direction = direction;
        this.amountSatang = amountSatang;
    }

    public Long getId() { return id; }
    public Long getTransferId() { return transferId; }
    public Long getAccountId() { return accountId; }
    public Direction getDirection() { return direction; }
    public long getAmountSatang() { return amountSatang; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
