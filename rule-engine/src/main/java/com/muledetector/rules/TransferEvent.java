package com.muledetector.rules;

import java.time.OffsetDateTime;

/** The fields of core-banking's TransferCompletedEvent that the rules read. Unknown fields are ignored. */
public record TransferEvent(
        long transferId,
        String fromAccountNo,
        String toAccountNo,
        long amountSatang,
        OffsetDateTime occurredAt,
        String fromAccountType,
        String toAccountType,
        OffsetDateTime toOpenedAt
) {
    boolean fromSystem() { return "SYSTEM".equals(fromAccountType); }
}
