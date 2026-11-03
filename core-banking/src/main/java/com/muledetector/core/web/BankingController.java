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
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.core.env.Environment;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/api")
public class BankingController {

    /** Replays historical data with its original timestamps. Honored only under the "sim" profile. */
    static final String SIM_TIME = "X-Simulated-Time";

    private final AccountService accountService;
    private final TransferService transferService;
    private final LedgerEntryRepository ledger;
    private final Environment env;

    public BankingController(AccountService accountService, TransferService transferService, LedgerEntryRepository ledger,
                             Environment env) {
        this.accountService = accountService;
        this.transferService = transferService;
        this.ledger = ledger;
        this.env = env;
    }

    private OffsetDateTime simTime(String requested) {
        if (requested == null) return null;
        if (!env.matchesProfiles("sim")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "SIM_TIME_DISABLED", SIM_TIME + " is only accepted with the sim profile");
        }
        try {
            return OffsetDateTime.parse(requested);
        } catch (DateTimeParseException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SIM_TIME", SIM_TIME + " must be ISO-8601 with an offset");
        }
    }

    /** {@code type} is CUSTOMER (default) or BUSINESS. */
    public record OpenAccountRequest(@NotBlank @Size(max = 100) String ownerName, Account.Type type) {}
    public record AmountRequest(@Positive long amountSatang) {}
    public record TransferRequest(@NotBlank String fromAccountNo, @NotBlank String toAccountNo, @Positive long amountSatang) {}
    public record AccountView(String accountNo, String ownerName, String type, String status, long balanceSatang) {
        static AccountView of(Account a) {
            return new AccountView(a.getAccountNo(), a.getOwnerName(), a.getType().name(), a.getStatus().name(),
                    a.getBalanceSatang());
        }
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView open(@Valid @RequestBody OpenAccountRequest req,
                            @RequestHeader(value = SIM_TIME, required = false) String at) {
        Account.Type type = req.type() != null ? req.type() : Account.Type.CUSTOMER;
        return AccountView.of(accountService.open(req.ownerName(), type, simTime(at)));
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
                            @Valid @RequestBody AmountRequest req,
                            @RequestHeader(value = SIM_TIME, required = false) String at) {
        return transferService.deposit(key, accountNo, req.amountSatang(), simTime(at));
    }

    @PostMapping("/accounts/{accountNo}/withdraw")
    public Transfer withdraw(@RequestHeader("Idempotency-Key") String key, @PathVariable String accountNo,
                             @Valid @RequestBody AmountRequest req,
                             @RequestHeader(value = SIM_TIME, required = false) String at) {
        return transferService.withdraw(key, accountNo, req.amountSatang(), simTime(at));
    }

    @PostMapping("/transfers")
    public Transfer transfer(@RequestHeader("Idempotency-Key") String key, @Valid @RequestBody TransferRequest req,
                             @RequestHeader(value = SIM_TIME, required = false) String at) {
        return transferService.transfer(key, req.fromAccountNo(), req.toAccountNo(), req.amountSatang(), simTime(at));
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
