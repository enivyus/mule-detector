package com.muledetector.core.web;

import com.muledetector.core.account.Account;
import com.muledetector.core.account.AccountService;
import com.muledetector.core.ledger.LedgerEntry;
import com.muledetector.core.ledger.LedgerEntryRepository;
import com.muledetector.core.transfer.Transfer;
import com.muledetector.core.transfer.TransferService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BankingController {

    private final AccountService accountService;
    private final TransferService transferService;
    private final LedgerEntryRepository ledger;

    public BankingController(AccountService accountService, TransferService transferService, LedgerEntryRepository ledger) {
        this.accountService = accountService;
        this.transferService = transferService;
        this.ledger = ledger;
    }

    public record OpenAccountRequest(@NotBlank String ownerName) {}
    public record AmountRequest(@Positive long amountSatang) {}
    public record TransferRequest(@NotBlank String fromAccountNo, @NotBlank String toAccountNo, @Positive long amountSatang) {}
    public record AccountView(String accountNo, String ownerName, String status, long balanceSatang) {
        static AccountView of(Account a) {
            return new AccountView(a.getAccountNo(), a.getOwnerName(), a.getStatus().name(), a.getBalanceSatang());
        }
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView open(@Valid @RequestBody OpenAccountRequest req) {
        return AccountView.of(accountService.open(req.ownerName()));
    }

    @GetMapping("/accounts/{accountNo}")
    public AccountView get(@PathVariable String accountNo) {
        return AccountView.of(accountService.get(accountNo));
    }

    /** Newest first. shortcut: offset paging shifts rows when new entries arrive; switch to a cursor (before id) if clients page live accounts. */
    @GetMapping("/accounts/{accountNo}/statement")
    public Page<LedgerEntry> statement(@PathVariable String accountNo,
                                       @RequestParam(defaultValue = "0") int page,
                                       @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > 200) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PAGE", "page must be >= 0 and size between 1 and 200");
        }
        return ledger.findByAccountIdOrderByCreatedAtDescIdDesc(accountService.get(accountNo).getId(), PageRequest.of(page, size));
    }

    @PostMapping("/accounts/{accountNo}/deposit")
    public Transfer deposit(@RequestHeader("Idempotency-Key") String key, @PathVariable String accountNo,
                            @Valid @RequestBody AmountRequest req) {
        return transferService.deposit(key, accountNo, req.amountSatang());
    }

    @PostMapping("/accounts/{accountNo}/withdraw")
    public Transfer withdraw(@RequestHeader("Idempotency-Key") String key, @PathVariable String accountNo,
                             @Valid @RequestBody AmountRequest req) {
        return transferService.withdraw(key, accountNo, req.amountSatang());
    }

    @PostMapping("/transfers")
    public Transfer transfer(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody TransferRequest req) {
        return transferService.transfer(key, req.fromAccountNo(), req.toAccountNo(), req.amountSatang());
    }

    /** Admin: freeze / unfreeze. Later called by the risk-scoring service when an account is flagged. */
    @PostMapping("/admin/accounts/{accountNo}/freeze")
    public AccountView freeze(@PathVariable String accountNo) {
        return AccountView.of(accountService.setFrozen(accountNo, true));
    }

    @PostMapping("/admin/accounts/{accountNo}/unfreeze")
    public AccountView unfreeze(@PathVariable String accountNo) {
        return AccountView.of(accountService.setFrozen(accountNo, false));
    }
}
