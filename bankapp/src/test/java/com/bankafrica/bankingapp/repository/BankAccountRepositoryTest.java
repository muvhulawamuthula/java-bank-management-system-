package com.bankafrica.bankingapp.repository;

import com.bankafrica.bankingapp.model.BankAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the BankAccountRepository.
 * Uses @DataJpaTest to test the repository with an in-memory database.
 */
@DataJpaTest
@ActiveProfiles("test")
class BankAccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private BankAccount testAccount;
    private final String ACCOUNT_HOLDER_NAME = "John Doe";
    private final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");

    @BeforeEach
    void setUp() {
        // Create a test bank account
        testAccount = new BankAccount(ACCOUNT_HOLDER_NAME, INITIAL_BALANCE);
        
        // Save the account to the database
        entityManager.persist(testAccount);
        entityManager.flush();
    }

    @Test
    @DisplayName("Test find bank account by ID")
    void testFindById() {
        // Call the repository method
        Optional<BankAccount> foundAccount = bankAccountRepository.findById(testAccount.getId());
        
        // Verify the result
        assertTrue(foundAccount.isPresent());
        assertEquals(ACCOUNT_HOLDER_NAME, foundAccount.get().getAccountHolderName());
        assertEquals(INITIAL_BALANCE, foundAccount.get().getBalance());
        assertNotNull(foundAccount.get().getAccountNumber());
        assertNotNull(foundAccount.get().getCreatedAt());
        
        // Test with non-existent ID
        Optional<BankAccount> notFoundAccount = bankAccountRepository.findById(999L);
        assertFalse(notFoundAccount.isPresent());
    }

    @Test
    @DisplayName("Test save bank account")
    void testSaveBankAccount() {
        // Create a new bank account
        BankAccount newAccount = new BankAccount("Jane Smith", new BigDecimal("500.00"));
        
        // Save the account
        BankAccount savedAccount = bankAccountRepository.save(newAccount);
        
        // Verify the result
        assertNotNull(savedAccount.getId());
        assertEquals("Jane Smith", savedAccount.getAccountHolderName());
        assertEquals(new BigDecimal("500.00"), savedAccount.getBalance());
        assertNotNull(savedAccount.getAccountNumber());
        assertNotNull(savedAccount.getCreatedAt());
        
        // Verify the account is in the database
        Optional<BankAccount> foundAccount = bankAccountRepository.findById(savedAccount.getId());
        assertTrue(foundAccount.isPresent());
        assertEquals("Jane Smith", foundAccount.get().getAccountHolderName());
    }

    @Test
    @DisplayName("Test update bank account")
    void testUpdateBankAccount() {
        // Update the account
        testAccount.setAccountHolderName("John Smith");
        testAccount.setBalance(new BigDecimal("1500.00"));
        bankAccountRepository.save(testAccount);
        
        // Verify the update
        Optional<BankAccount> updatedAccount = bankAccountRepository.findById(testAccount.getId());
        assertTrue(updatedAccount.isPresent());
        assertEquals("John Smith", updatedAccount.get().getAccountHolderName());
        assertEquals(new BigDecimal("1500.00"), updatedAccount.get().getBalance());
    }

    @Test
    @DisplayName("Test delete bank account")
    void testDeleteBankAccount() {
        // Verify the account exists
        assertTrue(bankAccountRepository.existsById(testAccount.getId()));
        
        // Delete the account
        bankAccountRepository.deleteById(testAccount.getId());
        
        // Verify the account no longer exists
        assertFalse(bankAccountRepository.existsById(testAccount.getId()));
    }

    @Test
    @DisplayName("Test bank account deposit")
    void testBankAccountDeposit() {
        // Deposit amount
        BigDecimal depositAmount = new BigDecimal("500.00");
        BigDecimal expectedBalance = INITIAL_BALANCE.add(depositAmount);
        
        // Perform deposit
        testAccount.deposit(depositAmount);
        bankAccountRepository.save(testAccount);
        
        // Verify the deposit
        Optional<BankAccount> updatedAccount = bankAccountRepository.findById(testAccount.getId());
        assertTrue(updatedAccount.isPresent());
        assertEquals(expectedBalance, updatedAccount.get().getBalance());
    }

    @Test
    @DisplayName("Test bank account withdrawal")
    void testBankAccountWithdrawal() {
        // Withdrawal amount
        BigDecimal withdrawalAmount = new BigDecimal("300.00");
        BigDecimal expectedBalance = INITIAL_BALANCE.subtract(withdrawalAmount);
        
        // Perform withdrawal
        boolean result = testAccount.withdraw(withdrawalAmount);
        bankAccountRepository.save(testAccount);
        
        // Verify the withdrawal
        assertTrue(result);
        Optional<BankAccount> updatedAccount = bankAccountRepository.findById(testAccount.getId());
        assertTrue(updatedAccount.isPresent());
        assertEquals(expectedBalance, updatedAccount.get().getBalance());
    }

    @Test
    @DisplayName("Test bank account withdrawal with insufficient funds")
    void testBankAccountWithdrawalWithInsufficientFunds() {
        // Withdrawal amount greater than balance
        BigDecimal excessiveAmount = INITIAL_BALANCE.add(new BigDecimal("100.00"));
        
        // Attempt withdrawal
        boolean result = testAccount.withdraw(excessiveAmount);
        bankAccountRepository.save(testAccount);
        
        // Verify the withdrawal failed and balance is unchanged
        assertFalse(result);
        Optional<BankAccount> updatedAccount = bankAccountRepository.findById(testAccount.getId());
        assertTrue(updatedAccount.isPresent());
        assertEquals(INITIAL_BALANCE, updatedAccount.get().getBalance());
    }
}