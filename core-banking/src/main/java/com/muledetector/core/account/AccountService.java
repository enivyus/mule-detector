package com.muledetector.core.account;

import com.muledetector.core.web.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

@Service
public class AccountService {

    private final AccountRepository accounts;

    public AccountService(AccountRepository accounts) {
        this.accounts = accounts;
    }

    @Transactional
    public Account open(String ownerName) {
        String accountNo;
        do {
            accountNo = String.format("%010d", ThreadLocalRandom.current().nextLong(1_000_000_000L, 9_999_999_999L));
        } while (accounts.findByAccountNo(accountNo).isPresent());
        return accounts.save(new Account(accountNo, ownerName));
    }

    @Transactional(readOnly = true)
    public Account get(String accountNo) {
        return accounts.findByAccountNo(accountNo)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "No account " + accountNo));
    }

    @Transactional
    public Account setFrozen(String accountNo, boolean frozen) {
        Account a = accounts.findIdByAccountNo(accountNo)
                .flatMap(accounts::findByIdForUpdate)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "No account " + accountNo));
        if (frozen) a.freeze(); else a.unfreeze();
        return a;
    }
}
