package com.bankafrica.bankingapp.service;

import com.bankafrica.bankingapp.BaseTest;
import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.repository.BankAccountRepository;
import com.bankafrica.bankingapp.repository.TransactionRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests proving balance correctness under concurrency. Crucially these are
 * NOT {@code @Transactional} — the account is committed before the worker threads run,
 * so the pessimistic row lock in {@link BankingService} is genuinely exercised across
 * real, separate database transactions.
 */
class BankingServiceTransactionTest extends BaseTest {

    @Autowired
    private BankingService bankingService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @AfterEach
    void cleanUp() {
        // No rollback here (tests commit), so tidy up to keep tests independent.
        transactionRepository.deleteAll();
        bankAccountRepository.deleteAll();
    }

    @Test
    @DisplayName("Concurrent deposits never lose an update")
    void testConcurrentDeposits() throws InterruptedException {
        BankAccount account = bankingService.createAccount("Test Account", new BigDecimal("1000.00"));
        Long accountId = account.getId();

        int numOperations = 10;
        BigDecimal depositAmount = new BigDecimal("100.00");
        BigDecimal expected = account.getBalance()
                .add(depositAmount.multiply(BigDecimal.valueOf(numOperations)));

        runConcurrently(numOperations, () -> bankingService.deposit(accountId, depositAmount));

        assertEquals(0, expected.compareTo(bankingService.getAccount(accountId).getBalance()),
                "Every concurrent deposit must be reflected in the final balance");
    }

    @Test
    @DisplayName("Concurrent withdrawals never overdraw or lose an update")
    void testConcurrentWithdrawals() throws InterruptedException {
        BankAccount account = bankingService.createAccount("Test Account", new BigDecimal("10000.00"));
        Long accountId = account.getId();

        int numOperations = 10;
        BigDecimal withdrawalAmount = new BigDecimal("100.00");
        BigDecimal expected = account.getBalance()
                .subtract(withdrawalAmount.multiply(BigDecimal.valueOf(numOperations)));

        runConcurrently(numOperations, () -> bankingService.withdraw(accountId, withdrawalAmount));

        assertEquals(0, expected.compareTo(bankingService.getAccount(accountId).getBalance()),
                "Final balance must equal initial minus every withdrawal");
    }

    @Test
    @DisplayName("Concurrent over-withdrawal can never drive the balance negative")
    void testConcurrentWithdrawalsCannotOverdraw() throws InterruptedException {
        // Only enough for 5 of the 10 withdrawals; the rest must fail cleanly.
        BankAccount account = bankingService.createAccount("Test Account", new BigDecimal("500.00"));
        Long accountId = account.getId();

        int numOperations = 10;
        BigDecimal withdrawalAmount = new BigDecimal("100.00");
        AtomicInteger successes = new AtomicInteger();

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(numOperations);
        ExecutorService executor = Executors.newFixedThreadPool(numOperations);
        for (int i = 0; i < numOperations; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    bankingService.withdraw(accountId, withdrawalAmount);
                    successes.incrementAndGet();
                } catch (Exception ignored) {
                    // Insufficient-funds failures are expected for the surplus attempts.
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS), "operations did not finish in time");
        executor.shutdown();

        assertEquals(5, successes.get(), "exactly 5 withdrawals of R100 fit in R500");
        BigDecimal finalBalance = bankingService.getAccount(accountId).getBalance();
        assertEquals(0, BigDecimal.ZERO.compareTo(finalBalance), "balance must land exactly on zero");
        assertTrue(finalBalance.compareTo(BigDecimal.ZERO) >= 0, "balance must never go negative");
    }

    @Test
    @DisplayName("Withdrawal with insufficient funds leaves the balance unchanged")
    void testWithdrawalWithInsufficientFunds() {
        BankAccount account = bankingService.createAccount("Test Account", new BigDecimal("500.00"));
        Long accountId = account.getId();

        Exception exception = assertThrows(RuntimeException.class, () ->
                bankingService.withdraw(accountId, new BigDecimal("600.00")));
        assertTrue(exception.getMessage().contains("Insufficient funds"));

        assertEquals(0, new BigDecimal("500.00")
                .compareTo(bankingService.getAccount(accountId).getBalance()));
    }

    @Test
    @DisplayName("Deposit then withdrawal updates the balance and the ledger")
    void testDepositAndWithdrawalSequence() {
        BankAccount account = bankingService.createAccount("Test Account", new BigDecimal("1000.00"));
        Long accountId = account.getId();

        bankingService.deposit(accountId, new BigDecimal("500.00"));
        assertEquals(0, new BigDecimal("1500.00")
                .compareTo(bankingService.getAccount(accountId).getBalance()));

        bankingService.withdraw(accountId, new BigDecimal("700.00"));
        assertEquals(0, new BigDecimal("800.00")
                .compareTo(bankingService.getAccount(accountId).getBalance()));

        // Both movements are on the ledger, newest first.
        assertEquals(2, bankingService.getLedger(accountId).size());
    }

    @Test
    @DisplayName("A transfer moves money atomically and records both legs")
    void testTransferBetweenAccounts() {
        BankAccount from = bankingService.createAccount("Sender", new BigDecimal("1000.00"));
        BankAccount to = bankingService.createAccount("Receiver", new BigDecimal("200.00"));

        bankingService.transfer(from.getId(), to.getAccountNumber(), new BigDecimal("300.00"), "Rent");

        assertEquals(0, new BigDecimal("700.00")
                .compareTo(bankingService.getAccount(from.getId()).getBalance()));
        assertEquals(0, new BigDecimal("500.00")
                .compareTo(bankingService.getAccount(to.getId()).getBalance()));
        assertEquals(1, bankingService.getLedger(from.getId()).size());
        assertEquals(1, bankingService.getLedger(to.getId()).size());
    }

    @Test
    @DisplayName("Creating an account with a negative balance coerces to zero")
    void testCreateAccountWithNegativeBalance() {
        BankAccount account = bankingService.createAccount("Test Account", new BigDecimal("-100.00"));
        assertTrue(account.getBalance().compareTo(BigDecimal.ZERO) >= 0);
    }

    /** Runs {@code task} on {@code threads} threads released simultaneously. */
    private void runConcurrently(int threads, Runnable task) throws InterruptedException {
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicBoolean failed = new AtomicBoolean(false);
        List<Exception> errors = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    task.run();
                } catch (Exception e) {
                    failed.set(true);
                    synchronized (errors) {
                        errors.add(e);
                    }
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        assertTrue(done.await(20, TimeUnit.SECONDS), "operations did not finish in time");
        executor.shutdown();
        assertFalse(failed.get(), "no operation should fail: " + errors);
    }
}
