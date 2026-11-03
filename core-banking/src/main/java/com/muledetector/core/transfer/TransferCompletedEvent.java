package com.muledetector.core.transfer;

import java.time.OffsetDateTime;

/**
 * Published after a transfer commits. Week 5: forward this to Kafka/Redpanda (topic "transactions")
 * so the rule engine and graph service can consume it.
 */
public record TransferCompletedEvent(
        Long transferId,
        String fromAccountNo,
        String toAccountNo,
        long amountSatang,
        Transfer.Kind kind,
        OffsetDateTime occurredAt
) {}
