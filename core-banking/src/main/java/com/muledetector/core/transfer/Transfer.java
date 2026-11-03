package com.muledetector.core.transfer;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "transfers")
public class Transfer {

    public enum Kind { TRANSFER, DEPOSIT, WITHDRAWAL }
    public enum Status { COMPLETED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "from_account_id", nullable = false)
    private Long fromAccountId;

    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;

    @Column(name = "amount_satang", nullable = false)
    private long amountSatang;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Kind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    protected Transfer() {}

    public Transfer(String idempotencyKey, Long fromAccountId, Long toAccountId, long amountSatang, Kind kind) {
        this.idempotencyKey = idempotencyKey;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amountSatang = amountSatang;
        this.kind = kind;
        this.status = Status.COMPLETED;
    }

    /** True if a retried request carries the same payload as the original. */
    public boolean sameRequest(Long from, Long to, long amount, Kind kind) {
        return fromAccountId.equals(from) && toAccountId.equals(to) && amountSatang == amount && this.kind == kind;
    }

    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public long getAmountSatang() { return amountSatang; }
    public Kind getKind() { return kind; }
    public Status getStatus() { return status; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
