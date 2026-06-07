package com.bankafrica.bankingapp.service;

import com.bankafrica.bankingapp.exception.AccountNotFoundException;
import com.bankafrica.bankingapp.exception.InsufficientFundsException;
import com.bankafrica.bankingapp.exception.InvalidRequestException;
import com.bankafrica.bankingapp.exception.TransactionNotFoundException;
import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.Transaction;
import com.bankafrica.bankingapp.model.TransactionType;
import com.bankafrica.bankingapp.repository.BankAccountRepository;
import com.bankafrica.bankingapp.repository.TransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * All money movement runs through here. Two invariants are enforced:
 *
 * <ol>
 *   <li><b>Correctness under concurrency</b> — every balance mutation loads the
 *       account with a {@code SELECT ... FOR UPDATE} row lock
 *       ({@link BankAccountRepository#findByIdForUpdate}), so concurrent operations
 *       on the same account are serialised and can never lose an update or overdraw.</li>
 *   <li><b>Auditability</b> — every deposit, withdrawal and transfer leg writes an
 *       immutable {@link Transaction} row recording the amount and the resulting
 *       balance, within the same database transaction as the balance change.</li>
 * </ol>
 */
@Service
public class BankingService {

    private final BankAccountRepository bankAccountRepository;
    private final TransactionRepository transactionRepository;

    public BankingService(BankAccountRepository bankAccountRepository,
                          TransactionRepository transactionRepository) {
        this.bankAccountRepository = bankAccountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public BankAccount getAccount(Long accountId) {
        return bankAccountRepository.findById(accountId)
                .orElseThrow(() -> AccountNotFoundException.withId(accountId));
    }

    @Transactional(readOnly = true)
    public List<Transaction> getLedger(Long accountId) {
        if (!bankAccountRepository.existsById(accountId)) {
            throw AccountNotFoundException.withId(accountId);
        }
        return transactionRepository.findByAccountIdOrderByCreatedAtDescIdDesc(accountId);
    }

    /** One page of the account's ledger, most recent first. */
    @Transactional(readOnly = true)
    public Page<Transaction> getLedger(Long accountId, Pageable pageable) {
        if (!bankAccountRepository.existsById(accountId)) {
            throw AccountNotFoundException.withId(accountId);
        }
        return transactionRepository.findByAccountIdOrderByCreatedAtDescIdDesc(accountId, pageable);
    }

    /** Loads one of the account's own transactions, or 404 if it isn't theirs / doesn't exist. */
    @Transactional(readOnly = true)
    public Transaction getTransaction(Long accountId, Long transactionId) {
        return transactionRepository.findByIdAndAccountId(transactionId, accountId)
                .orElseThrow(() -> new TransactionNotFoundException(transactionId));
    }

    @Transactional
    public BankAccount deposit(Long accountId, BigDecimal amount) {
        requirePositive(amount, "Deposit amount must be positive");
        BankAccount account = lockById(accountId);

        account.setBalance(account.getBalance().add(amount));
        BankAccount saved = bankAccountRepository.save(account);
        record(saved, TransactionType.DEPOSIT, amount, "Deposit", null);
        return saved;
    }

    @Transactional
    public BankAccount withdraw(Long accountId, BigDecimal amount) {
        requirePositive(amount, "Withdrawal amount must be positive");
        BankAccount account = lockById(accountId);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(account.getBalance());
        }
        account.setBalance(account.getBalance().subtract(amount));
        BankAccount saved = bankAccountRepository.save(account);
        record(saved, TransactionType.WITHDRAWAL, amount, "Withdrawal", null);
        return saved;
    }

    /**
     * Atomically moves {@code amount} from the source account to the account identified
     * by {@code toAccountNumber}. Both accounts are locked in a deterministic order (by
     * id) to prevent deadlocks between opposing transfers. Returns the updated source.
     */
    @Transactional
    public BankAccount transfer(Long fromAccountId, String toAccountNumber,
                                BigDecimal amount, String description) {
        requirePositive(amount, "Transfer amount must be positive");

        BankAccount destinationPreview = bankAccountRepository.findByAccountNumber(toAccountNumber)
                .orElseThrow(() -> AccountNotFoundException.withNumber(toAccountNumber));
        if (destinationPreview.getId().equals(fromAccountId)) {
            throw new InvalidRequestException("Cannot transfer to the same account");
        }

        // Lock in ascending-id order so two opposing transfers can't deadlock.
        Long firstId = Math.min(fromAccountId, destinationPreview.getId());
        Long secondId = Math.max(fromAccountId, destinationPreview.getId());
        BankAccount first = lockById(firstId);
        BankAccount second = lockById(secondId);

        BankAccount from = first.getId().equals(fromAccountId) ? first : second;
        BankAccount to = from == first ? second : first;

        if (from.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(from.getBalance());
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        BankAccount savedFrom = bankAccountRepository.save(from);
        BankAccount savedTo = bankAccountRepository.save(to);

        String note = (description == null || description.isBlank()) ? "Transfer" : description;
        record(savedFrom, TransactionType.TRANSFER_OUT, amount, note, savedTo.getAccountNumber());
        record(savedTo, TransactionType.TRANSFER_IN, amount, note, savedFrom.getAccountNumber());
        return savedFrom;
    }

    /**
     * Opens a standalone account. Used by tests and administrative tooling; customer
     * accounts are created through registration. A negative or null balance is coerced
     * to zero — an account can never start in the red.
     */
    @Transactional
    public BankAccount createAccount(String accountHolderName, BigDecimal initialBalance) {
        BankAccount account = new BankAccount();
        account.setAccountHolderName(accountHolderName);
        BigDecimal opening = (initialBalance != null && initialBalance.compareTo(BigDecimal.ZERO) > 0)
                ? initialBalance : BigDecimal.ZERO;
        account.setBalance(opening);
        return bankAccountRepository.save(account);
    }

    private BankAccount lockById(Long accountId) {
        return bankAccountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> AccountNotFoundException.withId(accountId));
    }

    private void record(BankAccount account, TransactionType type, BigDecimal amount,
                        String description, String counterpartyAccountNumber) {
        transactionRepository.save(new Transaction(
                account, type, amount, account.getBalance(), description, counterpartyAccountNumber));
    }

    private void requirePositive(BigDecimal amount, String message) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestException(message);
        }
    }
}
