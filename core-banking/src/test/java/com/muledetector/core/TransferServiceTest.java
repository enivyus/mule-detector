package com.muledetector.core;

import com.muledetector.core.account.Account;
import com.muledetector.core.account.AccountRepository;
import com.muledetector.core.account.AccountService;
import com.muledetector.core.ledger.LedgerEntryRepository;
import com.muledetector.core.transfer.TransferRepository;
import com.muledetector.core.transfer.TransferService;
import com.muledetector.core.web.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class TransferServiceTest {

    @Autowired AccountService accountService;
    @Autowired TransferService transferService;
    @Autowired AccountRepository accounts;
    @Autowired TransferRepository transfers;
    @Autowired LedgerEntryRepository ledger;

    private Account funded(String name, long satang) {
        Account a = accountService.open(name, Account.Type.CUSTOMER, null);
        transferService.deposit(UUID.randomUUID().toString(), a.getAccountNo(), satang, null);
        return accounts.findById(a.getId()).orElseThrow();
    }

    private long balance(Account a) {
        return accounts.findById(a.getId()).orElseThrow().getBalanceSatang();
    }

    @Test
    void transferMovesMoneyAndWritesTwoLedgerRows() {
        Account a = funded("Alice", 10_000);
        Account b = funded("Bob", 1);

        transferService.transfer("t-basic", a.getAccountNo(), b.getAccountNo(), 2_500, null);

        assertThat(balance(a)).isEqualTo(7_500);
        assertThat(balance(b)).isEqualTo(2_501);
        assertThat(ledger.computeBalance(a.getId())).isEqualTo(7_500);
        assertThat(ledger.computeBalance(b.getId())).isEqualTo(2_501);
    }

    @Test
    void sameIdempotencyKeyNeverChargesTwice() {
        Account a = funded("Carol", 10_000);
        Account b = funded("Dave", 1);

        var first = transferService.transfer("t-retry", a.getAccountNo(), b.getAccountNo(), 1_000, null);
        var second = transferService.transfer("t-retry", a.getAccountNo(), b.getAccountNo(), 1_000, null);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(balance(a)).isEqualTo(9_000);
    }

    @Test
    void reusingKeyForDifferentRequestIsRejected() {
        Account a = funded("Erin", 10_000);
        Account b = funded("Frank", 1);
        transferService.transfer("t-reuse", a.getAccountNo(), b.getAccountNo(), 1_000, null);

        assertThatThrownBy(() -> transferService.transfer("t-reuse", a.getAccountNo(), b.getAccountNo(), 2_000, null))
                .isInstanceOf(ApiException.class).hasMessageContaining("different request");
    }

    @Test
    void insufficientFundsAndFrozenAccountsAreRejected() {
        Account a = funded("Grace", 500);
        Account b = funded("Heidi", 1);

        assertThatThrownBy(() -> transferService.transfer("t-nsf", a.getAccountNo(), b.getAccountNo(), 501, null))
                .isInstanceOf(ApiException.class).hasMessageContaining("Insufficient");

        accountService.setFrozen(b.getAccountNo(), true);
        assertThatThrownBy(() -> transferService.transfer("t-frozen", a.getAccountNo(), b.getAccountNo(), 100, null))
                .isInstanceOf(ApiException.class).hasMessageContaining("frozen");
        assertThat(balance(a)).isEqualTo(500);
    }

    @Test
    void rejectsHugeAmountsLongKeysAndVaultTransfers() {
        Account a = funded("Ivan", 10_000);
        String vault = "0000000000";

        assertThatThrownBy(() -> transferService.deposit(UUID.randomUUID().toString(), a.getAccountNo(), Long.MAX_VALUE, null))
                .isInstanceOf(ApiException.class).hasMessageContaining("at most");
        assertThatThrownBy(() -> transferService.deposit("k".repeat(65), a.getAccountNo(), 100, null))
                .isInstanceOf(ApiException.class).hasMessageContaining("at most 64");
        assertThatThrownBy(() -> transferService.transfer(UUID.randomUUID().toString(), vault, a.getAccountNo(), 100, null))
                .isInstanceOf(ApiException.class).hasMessageContaining("cash vault");
        assertThatThrownBy(() -> transferService.transfer(UUID.randomUUID().toString(), a.getAccountNo(), vault, 100, null))
                .isInstanceOf(ApiException.class).hasMessageContaining("cash vault");
        assertThat(balance(a)).isEqualTo(10_000);
    }

    /**
     * 1,000 random transfers between 10 accounts from 16 threads at once.
     * Money must be conserved, no balance may go negative, and the cached balance must match the ledger.
     */
    @Test
    void concurrentTransfersKeepBooksBalanced() throws Exception {
        int nAccounts = 10, nTransfers = 1_000;
        long start = 100_000;
        List<Account> pool = new ArrayList<>();
        for (int i = 0; i < nAccounts; i++) pool.add(funded("Concurrent-" + i, start));

        ExecutorService exec = Executors.newFixedThreadPool(16);
        CountDownLatch go = new CountDownLatch(1);
        AtomicInteger ok = new AtomicInteger(), rejected = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < nTransfers; i++) {
            futures.add(exec.submit(() -> {
                go.await();
                var rnd = ThreadLocalRandom.current();
                int from = rnd.nextInt(nAccounts), to;
                do { to = rnd.nextInt(nAccounts); } while (to == from);
                try {
                    transferService.transfer(UUID.randomUUID().toString(),
                            pool.get(from).getAccountNo(), pool.get(to).getAccountNo(), rnd.nextLong(1, 20_000), null);
                    ok.incrementAndGet();
                } catch (ApiException e) {
                    rejected.incrementAndGet(); // insufficient funds is a valid outcome
                }
                return null;
            }));
        }
        go.countDown();
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        exec.shutdown();

        long total = 0;
        for (Account a : pool) {
            long cached = balance(a);
            assertThat(cached).isGreaterThanOrEqualTo(0);
            assertThat(ledger.computeBalance(a.getId())).isEqualTo(cached);
            total += cached;
        }
        assertThat(total).isEqualTo(nAccounts * start);
        assertThat(ledger.globalImbalance()).isZero();
        assertThat(ok.get() + rejected.get()).isEqualTo(nTransfers);
        System.out.printf("concurrency test: %d completed, %d rejected, books balanced%n", ok.get(), rejected.get());
    }
}
