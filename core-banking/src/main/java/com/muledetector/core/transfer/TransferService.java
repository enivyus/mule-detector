package com.muledetector.core.transfer;

import com.muledetector.core.account.Account;
import com.muledetector.core.account.AccountRepository;
import com.muledetector.core.ledger.LedgerEntry;
import com.muledetector.core.ledger.LedgerEntryRepository;
import com.muledetector.core.web.ApiException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Moves money between accounts. Guarantees:
 *  1. Atomic: transfer row + DEBIT + CREDIT + cached balances commit together or not at all.
 *  2. No race conditions: both account rows are locked with SELECT ... FOR UPDATE.
 *  3. No deadlocks: rows are always locked in ascending id order.
 *  4. Idempotent: the same idempotency key never moves money twice.
 */
@Service
public class TransferService {

    private final AccountRepository accounts;
    private final TransferRepository transfers;
    private final LedgerEntryRepository ledger;
    private final ApplicationEventPublisher events;

    public TransferService(AccountRepository accounts, TransferRepository transfers,
                           LedgerEntryRepository ledger, ApplicationEventPublisher events) {
        this.accounts = accounts;
        this.transfers = transfers;
        this.ledger = ledger;
        this.events = events;
    }

    // Overloads call through `this`, which bypasses the proxy, so each needs its own @Transactional.
    @Transactional
    public Transfer transfer(String idempotencyKey, String fromAccountNo, String toAccountNo, long amountSatang) {
        return transfer(idempotencyKey, fromAccountNo, toAccountNo, amountSatang, null);
    }

    @Transactional
    public Transfer deposit(String idempotencyKey, String accountNo, long amountSatang) {
        return deposit(idempotencyKey, accountNo, amountSatang, null);
    }

    @Transactional
    public Transfer withdraw(String idempotencyKey, String accountNo, long amountSatang) {
        return withdraw(idempotencyKey, accountNo, amountSatang, null);
    }

    /** {@code occurredAt} overrides the booking time (simulation replay only); null means now. */
    @Transactional
    public Transfer transfer(String idempotencyKey, String fromAccountNo, String toAccountNo, long amountSatang,
                             OffsetDateTime occurredAt) {
        Long fromId = idOf(fromAccountNo);
        Long toId = idOf(toAccountNo);
        return execute(idempotencyKey, fromId, toId, amountSatang, Transfer.Kind.TRANSFER, occurredAt);
    }

    @Transactional
    public Transfer deposit(String idempotencyKey, String accountNo, long amountSatang, OffsetDateTime occurredAt) {
        return execute(idempotencyKey, vaultId(), idOf(accountNo), amountSatang, Transfer.Kind.DEPOSIT, occurredAt);
    }

    @Transactional
    public Transfer withdraw(String idempotencyKey, String accountNo, long amountSatang, OffsetDateTime occurredAt) {
        return execute(idempotencyKey, idOf(accountNo), vaultId(), amountSatang, Transfer.Kind.WITHDRAWAL, occurredAt);
    }

    private Transfer execute(String key, Long fromId, Long toId, long amount, Transfer.Kind kind, OffsetDateTime occurredAt) {
        if (key == null || key.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "IDEMPOTENCY_KEY_REQUIRED", "Idempotency-Key header is required");
        }
        if (amount <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be positive");
        }
        if (fromId.equals(toId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SAME_ACCOUNT", "Cannot transfer to the same account");
        }

        // Retry of a request we already processed: return the original result, move no money.
        var existing = transfers.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return replay(existing.get(), fromId, toId, amount, kind);
        }

        // Lock in a fixed order so two opposite transfers (A->B and B->A) can't deadlock.
        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);
        Account first = lock(firstId);
        Account second = lock(secondId);
        Account from = first.getId().equals(fromId) ? first : second;
        Account to = from == first ? second : first;

        // Re-check after acquiring locks: a concurrent request with the same key may have just committed.
        existing = transfers.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return replay(existing.get(), fromId, toId, amount, kind);
        }

        if (!from.isActive() || !to.isActive()) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "ACCOUNT_FROZEN", "Account is frozen");
        }
        if (!from.isSystem() && from.getBalanceSatang() < amount) {
            throw new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_FUNDS", "Insufficient funds");
        }

        OffsetDateTime at = occurredAt != null ? occurredAt : OffsetDateTime.now();
        Transfer t = transfers.save(new Transfer(key, fromId, toId, amount, kind, at));
        ledger.save(new LedgerEntry(t.getId(), fromId, LedgerEntry.Direction.DEBIT, amount, at));
        ledger.save(new LedgerEntry(t.getId(), toId, LedgerEntry.Direction.CREDIT, amount, at));
        from.debit(amount);
        to.credit(amount);

        events.publishEvent(new TransferCompletedEvent(
                t.getId(), from.getAccountNo(), to.getAccountNo(), amount, kind, t.getCreatedAt(),
                from.getType(), to.getType(), from.getCreatedAt(), to.getCreatedAt()));
        return t;
    }

    private Transfer replay(Transfer original, Long fromId, Long toId, long amount, Transfer.Kind kind) {
        if (!original.sameRequest(fromId, toId, amount, kind)) {
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_REUSED",
                    "Idempotency key was already used for a different request");
        }
        return original;
    }

    private Account lock(Long id) {
        return accounts.findByIdForUpdate(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "No account id " + id));
    }

    // Id-only lookups on purpose: loading the Account entity here would make the later
    // FOR UPDATE query hand back a stale cached copy and silently lose concurrent updates.
    private Long idOf(String accountNo) {
        return accounts.findIdByAccountNo(accountNo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "No account " + accountNo));
    }

    private Long vaultId() {
        return accounts.findIdByType(Account.Type.SYSTEM)
                .orElseThrow(() -> new IllegalStateException("System cash vault account is missing"));
    }
}
