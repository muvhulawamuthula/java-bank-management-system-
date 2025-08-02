package com.bankafrica.bankingapp.service;

import com.bankafrica.bankingapp.BaseTest;
import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.repository.BankAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for transaction consistency in the BankingService.
 * These tests verify that account balances remain consistent during concurrent operations.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class BankingServiceTransactionTest extends BaseTest {

    @Autowired
    private BankingService bankingService;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Test
    @DisplayName("Test concurrent deposits to the same account")
    void testConcurrentDeposits() throws InterruptedException {
        // Create a test account with initial balance
        BankAccount account = bankingService.createAccount("Test Account", new BigDecimal("1000.00"));
        Long accountId = account.getId();

        // Number of concurrent deposit operations
        int numOperations = 10;
        BigDecimal depositAmount = new BigDecimal("100.00");
        BigDecimal expectedFinalBalance = account.getBalance().add(depositAmount.multiply(new BigDecimal(numOperations)));

        // Use CountDownLatch to synchronize threads
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numOperations);
        AtomicBoolean failed = new AtomicBoolean(false);
        List<Exception> exceptions = new ArrayList<>();

        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(numOperations);

        // Submit deposit tasks
        for (int i = 0; i < numOperations; i++) {
            executor.submit(() -> {
                try {
                    // Wait for signal to start
                    latch.await();
                    
                    // Perform deposit
                    bankingService.deposit(accountId, depositAmount);
                } catch (Exception e) {
                    failed.set(true);
                    exceptions.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        latch.countDown();
        
        // Wait for all operations to complete
        completionLatch.await();
        
        // Shutdown executor
        executor.shutdown();

        // Check for failures
        assertFalse(failed.get(), "One or more deposit operations failed: " + exceptions);

        // Refresh account from database
        BankAccount updatedAccount = bankingService.getAccount(accountId);
        
        // Verify final balance
        assertEquals(expectedFinalBalance, updatedAccount.getBalance(), 
                "Final balance should be initial balance + (deposit amount * number of operations)");
    }

    @Test
    @DisplayName("Test concurrent withdrawals from the same account")
    void testConcurrentWithdrawals() throws InterruptedException {
        // Create a test account with sufficient initial balance
        BigDecimal initialBalance = new BigDecimal("10000.00");
        BankAccount account = bankingService.createAccount("Test Account", initialBalance);
        Long accountId = account.getId();

        // Number of concurrent withdrawal operations
        int numOperations = 10;
        BigDecimal withdrawalAmount = new BigDecimal("100.00");
        BigDecimal expectedFinalBalance = account.getBalance().subtract(withdrawalAmount.multiply(new BigDecimal(numOperations)));

        // Use CountDownLatch to synchronize threads
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch completionLatch = new CountDownLatch(numOperations);
        AtomicBoolean failed = new AtomicBoolean(false);
        List<Exception> exceptions = new ArrayList<>();

        // Create thread pool
        ExecutorService executor = Executors.newFixedThreadPool(numOperations);

        // Submit withdrawal tasks
        for (int i = 0; i < numOperations; i++) {
            executor.submit(() -> {
                try {
                    // Wait for signal to start
                    latch.await();
                    
                    // Perform withdrawal
                    bankingService.withdraw(accountId, withdrawalAmount);
                } catch (Exception e) {
                    failed.set(true);
                    exceptions.add(e);
                } finally {
                    completionLatch.countDown();
                }
            });
        }

        // Start all threads simultaneously
        latch.countDown();
        
        // Wait for all operations to complete
        completionLatch.await();
        
        // Shutdown executor
        executor.shutdown();

        // Check for failures
        assertFalse(failed.get(), "One or more withdrawal operations failed: " + exceptions);

        // Refresh account from database
        BankAccount updatedAccount = bankingService.getAccount(accountId);
        
        // Verify final balance
        assertEquals(expectedFinalBalance, updatedAccount.getBalance(), 
                "Final balance should be initial balance - (withdrawal amount * number of operations)");
    }

    @Test
    @DisplayName("Test withdrawal with insufficient funds")
    void testWithdrawalWithInsufficientFunds() {
        // Create a test account with initial balance
        BankAccount account = bankingService.createAccount("Test Account", new BigDecimal("500.00"));
        Long accountId = account.getId();

        // Attempt to withdraw more than the balance
        BigDecimal withdrawalAmount = new BigDecimal("600.00");
        
        // Verify that an exception is thrown
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bankingService.withdraw(accountId, withdrawalAmount);
        });
        
        assertTrue(exception.getMessage().contains("Insufficient funds"));
        
        // Verify that the balance remains unchanged
        BankAccount updatedAccount = bankingService.getAccount(accountId);
        assertEquals(new BigDecimal("500.00"), updatedAccount.getBalance());
    }

    @Test
    @DisplayName("Test deposit and withdrawal in sequence")
    void testDepositAndWithdrawalSequence() {
        // Create a test account with initial balance
        BankAccount account = bankingService.createAccount("Test Account", new BigDecimal("1000.00"));
        Long accountId = account.getId();

        // Perform deposit
        BigDecimal depositAmount = new BigDecimal("500.00");
        bankingService.deposit(accountId, depositAmount);
        
        // Verify balance after deposit
        BankAccount accountAfterDeposit = bankingService.getAccount(accountId);
        assertEquals(new BigDecimal("1500.00"), accountAfterDeposit.getBalance());
        
        // Perform withdrawal
        BigDecimal withdrawalAmount = new BigDecimal("700.00");
        bankingService.withdraw(accountId, withdrawalAmount);
        
        // Verify balance after withdrawal
        BankAccount accountAfterWithdrawal = bankingService.getAccount(accountId);
        assertEquals(new BigDecimal("800.00"), accountAfterWithdrawal.getBalance());
    }

    @Test
    @DisplayName("Test creating account with negative initial balance")
    void testCreateAccountWithNegativeBalance() {
        // Attempt to create an account with negative initial balance
        BigDecimal negativeBalance = new BigDecimal("-100.00");
        
        // Verify that the account is created with zero balance instead of negative
        BankAccount account = bankingService.createAccount("Test Account", negativeBalance);
        
        // The service should handle this by setting the balance to zero or a minimum value
        assertTrue(account.getBalance().compareTo(BigDecimal.ZERO) >= 0, 
                "Account balance should not be negative");
    }
}