package com.muledetector.core.transfer;

import java.time.OffsetDateTime;

import com.muledetector.core.account.Account;

/**
 * Published inside the transfer's transaction; {@link TransferEventPublisher} forwards it to
 * Redpanda topic "transactions" after commit. Carries account type and opening time so the
 * rule engine can judge each side without calling back into the bank.
 */
public record TransferCompletedEvent(
        Long transferId,
        String fromAccountNo,
        String toAccountNo,
        long amountSatang,
        Transfer.Kind kind,
        OffsetDateTime occurredAt,
        Account.Type fromAccountType,
        Account.Type toAccountType,
        OffsetDateTime fromOpenedAt,
        OffsetDateTime toOpenedAt
) {}
