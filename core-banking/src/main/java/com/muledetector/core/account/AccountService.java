package com.muledetector.core.account;

import com.muledetector.core.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {

    private final AccountRepository accounts;
    private final FreezeAuditRepository audit;

    public AccountService(AccountRepository accounts, FreezeAuditRepository audit) {
        this.accounts = accounts;
        this.audit = audit;
    }

    /** {@code openedAt} overrides the opening time (simulation replay only); null means now. */
    @Transactional
    public Account open(String ownerName, Account.Type type, OffsetDateTime openedAt) {
        if (type == Account.Type.SYSTEM) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_ACCOUNT_TYPE", "SYSTEM accounts cannot be opened");
        }
        String accountNo;
        do {
            accountNo = String.format("%010d", ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
        } while (accounts.findByAccountNo(accountNo).isPresent());
        return accounts.save(new Account(accountNo, ownerName, type, openedAt != null ? openedAt : OffsetDateTime.now()));
    }

    @Transactional(readOnly = true)
    public Account get(String accountNo) {
        return accounts.findByAccountNo(accountNo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "No account " + accountNo));
    }

    /**
     * Records who changed the status and why, in the same transaction. Asking for the status the account already
     * has changes nothing and writes no audit row, so the log reads as a list of real decisions.
     */
    @Transactional
    public Account setFrozen(String accountNo, boolean frozen, String actor, String reason) {
        Account a = accounts.findIdByAccountNo(accountNo)
                .flatMap(accounts::findByIdForUpdate)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "No account " + accountNo));
        if (a.isActive() != frozen) return a;
        if (frozen) a.freeze(); else a.unfreeze();
        audit.save(new FreezeAudit(a.getId(), frozen ? FreezeAudit.Action.FREEZE : FreezeAudit.Action.UNFREEZE, actor, reason));
        return a;
    }

    /** Newest first. */
    @Transactional(readOnly = true)
    public List<FreezeAudit> auditOf(String accountNo) {
        return audit.findByAccountIdOrderByCreatedAtDescIdDesc(get(accountNo).getId());
    }
}
