package com.muledetector.core.web;

import com.muledetector.core.account.Account;
import com.muledetector.core.account.AccountService;
import com.muledetector.core.account.FreezeAudit;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Who may call what is decided in SecurityConfig, by URL. */
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
    public record FreezeRequest(@NotBlank @Size(max = 500) String reason) {}
    public record AuditView(String action, String actor, String reason, OffsetDateTime at) {
        static AuditView of(FreezeAudit f) {
            return new AuditView(f.getAction().name(), f.getActor(), f.getReason(), f.getCreatedAt());
        }
    }
    public record Me(String username, List<String> roles) {}
    public record AccountView(String accountNo, String ownerName, String type, String status, long balanceSatang) {
        static AccountView of(Account a) {
            return new AccountView(a.getAccountNo(), a.getOwnerName(), a.getType().name(), a.getStatus().name(),
                    a.getBalanceSatang());
        }
    }

    /** The caller's own name and roles: the dashboard checks a login with it and hides what the role cannot do. */
    @GetMapping("/me")
    public Me me(Authentication auth) {
        return new Me(auth.getName(), auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).map(r -> r.replaceFirst("^ROLE_", "")).toList());
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

    /** Investigators and the rule engine freeze; only investigators unfreeze. The actor is the logged-in user. */
    @PostMapping("/admin/accounts/{accountNo}/freeze")
    public AccountView freeze(@PathVariable String accountNo, @Valid @RequestBody FreezeRequest req, Authentication auth) {
        return AccountView.of(accountService.setFrozen(accountNo, true, auth.getName(), req.reason()));
    }

    @PostMapping("/admin/accounts/{accountNo}/unfreeze")
    public AccountView unfreeze(@PathVariable String accountNo, @Valid @RequestBody FreezeRequest req, Authentication auth) {
        return AccountView.of(accountService.setFrozen(accountNo, false, auth.getName(), req.reason()));
    }

    @GetMapping("/admin/accounts/{accountNo}/audit")
    public List<AuditView> audit(@PathVariable String accountNo) {
        return accountService.auditOf(accountNo).stream().map(AuditView::of).toList();
    }
}
