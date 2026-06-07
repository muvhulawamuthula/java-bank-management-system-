package com.bankafrica.bankingapp.controller;

import com.bankafrica.bankingapp.dto.AccountResponse;
import com.bankafrica.bankingapp.dto.AmountRequest;
import com.bankafrica.bankingapp.dto.PagedResponse;
import com.bankafrica.bankingapp.dto.SwiftMessageResponse;
import com.bankafrica.bankingapp.dto.TransactionResponse;
import com.bankafrica.bankingapp.dto.TransferRequest;
import com.bankafrica.bankingapp.exception.InvalidCredentialsException;
import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.Transaction;
import com.bankafrica.bankingapp.model.User;
import com.bankafrica.bankingapp.service.AuthService;
import com.bankafrica.bankingapp.service.BankingService;
import com.bankafrica.bankingapp.service.IdempotencyService;
import com.bankafrica.bankingapp.service.SwiftMessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

/**
 * Account operations always act on the <b>authenticated user's own</b> account, which
 * is resolved from the JWT — never from a client-supplied account id. This closes the
 * previous IDOR hole where any caller could deposit to or drain any account by id.
 *
 * <p>The money-moving endpoints (deposit, withdraw, transfer) honour an optional
 * {@code Idempotency-Key} header: send the same key on a retry and the original result is
 * replayed instead of moving money twice.
 */
@RestController
@RequestMapping("/api/account")
@Tag(name = "Account", description = "Balance, deposits, withdrawals, transfers, ledger and SWIFT")
public class BankingController {

    /** Upper bound on page size so a client can't ask for an unbounded ledger page. */
    private static final int MAX_PAGE_SIZE = 100;

    private final BankingService bankingService;
    private final AuthService authService;
    private final IdempotencyService idempotencyService;
    private final SwiftMessageService swiftMessageService;

    public BankingController(BankingService bankingService, AuthService authService,
                            IdempotencyService idempotencyService,
                            SwiftMessageService swiftMessageService) {
        this.bankingService = bankingService;
        this.authService = authService;
        this.idempotencyService = idempotencyService;
        this.swiftMessageService = swiftMessageService;
    }

    @GetMapping
    @Operation(summary = "Get the authenticated user's account snapshot")
    public ResponseEntity<AccountResponse> getAccount(@AuthenticationPrincipal UserDetails principal) {
        BankAccount account = bankingService.getAccount(currentAccountId(principal));
        return ResponseEntity.ok(AccountResponse.from(account));
    }

    @PostMapping("/deposit")
    @Operation(summary = "Deposit into the authenticated user's account (idempotent)")
    public ResponseEntity<AccountResponse> deposit(
            @AuthenticationPrincipal UserDetails principal,
            @Parameter(description = "Optional key making the deposit safe to retry exactly once")
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AmountRequest request) {
        Long accountId = currentAccountId(principal);
        AccountResponse response = idempotencyService.execute(
                idempotencyKey, accountId, "deposit", request,
                () -> AccountResponse.from(bankingService.deposit(accountId, request.amount())),
                AccountResponse.class);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Withdraw from the authenticated user's account (idempotent)")
    public ResponseEntity<AccountResponse> withdraw(
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody AmountRequest request) {
        Long accountId = currentAccountId(principal);
        AccountResponse response = idempotencyService.execute(
                idempotencyKey, accountId, "withdraw", request,
                () -> AccountResponse.from(bankingService.withdraw(accountId, request.amount())),
                AccountResponse.class);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer to another account by number (idempotent)")
    public ResponseEntity<AccountResponse> transfer(
            @AuthenticationPrincipal UserDetails principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {
        Long accountId = currentAccountId(principal);
        AccountResponse response = idempotencyService.execute(
                idempotencyKey, accountId, "transfer", request,
                () -> AccountResponse.from(bankingService.transfer(
                        accountId, request.toAccountNumber(), request.amount(), request.description())),
                AccountResponse.class);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Page through the account ledger, newest first")
    public ResponseEntity<PagedResponse<TransactionResponse>> transactions(
            @AuthenticationPrincipal UserDetails principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), MAX_PAGE_SIZE);
        Page<Transaction> ledger = bankingService.getLedger(
                currentAccountId(principal), PageRequest.of(safePage, safeSize));
        return ResponseEntity.ok(PagedResponse.from(ledger, TransactionResponse::from));
    }

    @GetMapping("/transactions/{id}/swift")
    @Operation(summary = "Generate the SWIFT MT103 message for one of your transfer transactions")
    public ResponseEntity<SwiftMessageResponse> swiftMessage(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long id) {
        Long accountId = currentAccountId(principal);
        BankAccount account = bankingService.getAccount(accountId);
        Transaction tx = bankingService.getTransaction(accountId, id);
        return ResponseEntity.ok(swiftMessageService.toMt103(tx, account));
    }

    private Long currentAccountId(UserDetails principal) {
        User user = authService.getUserByEmail(principal.getUsername());
        if (user == null || user.getBankAccount() == null) {
            throw new InvalidCredentialsException();
        }
        return user.getBankAccount().getId();
    }
}
