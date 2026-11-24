package com.bankafrica.bankingapp.repository;

import com.bankafrica.bankingapp.BaseTest;
import com.bankafrica.bankingapp.model.BankAccount;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the BankAccountRepository, running against the shared
 * in-memory database with each test rolled back.
 */
@Transactional
class BankAccountRepositoryTest extends BaseTest {

    @Autowired
    private BankAccountRepository bankAccountRepository;

    private BankAccount testAccount;
    private final String ACCOUNT_HOLDER_NAME = "John Doe";
    private final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");

    @BeforeEach
    void setUp() {
        testAccount = new BankAccount(ACCOUNT_HOLDER_NAME, INITIAL_BALANCE);
        
        bankAccountRepository.saveAndFlush(testAccount);
    }

    @Test
    @DisplayName("Test find bank account by ID")
    void testFindById() {
        Optional<BankAccount> foundAccount = bankAccountRepository.findById(testAccount.getId());
        
        assertTrue(foundAccount.isPresent());
        assertEquals(ACCOUNT_HOLDER_NAME, foundAccount.get().getAccountHolderName());
        assertEquals(INITIAL_BALANCE, foundAccount.get().getBalance());
        assertNotNull(foundAccount.get().getAccountNumber());
        assertNotNull(foundAccount.get().getCreatedAt());
        
        Optional<BankAccount> notFoundAccount = bankAccountRepository.findById(999L);
        assertFalse(notFoundAccount.isPresent());
    }

    @Test
    @DisplayName("Test save bank account")
    void testSaveBankAccount() {
        BankAccount newAccount = new BankAccount("Jane Smith", new BigDecimal("500.00"));
        
        BankAccount savedAccount = bankAccountRepository.save(newAccount);
        
        assertNotNull(savedAccount.getId());
        assertEquals("Jane Smith", savedAccount.getAccountHolderName());
        assertEquals(new BigDecimal("500.00"), savedAccount.getBalance());
        assertNotNull(savedAccount.getAccountNumber());
        assertNotNull(savedAccount.getCreatedAt());
        
        Optional<BankAccount> foundAccount = bankAccountRepository.findById(savedAccount.getId());
        assertTrue(foundAccount.isPresent());
        assertEquals("Jane Smith", foundAccount.get().getAccountHolderName());
    }

    @Test
    @DisplayName("Test update bank account")
    void testUpdateBankAccount() {
        testAccount.setAccountHolderName("John Smith");
        testAccount.setBalance(new BigDecimal("1500.00"));
        bankAccountRepository.save(testAccount);
        
        Optional<BankAccount> updatedAccount = bankAccountRepository.findById(testAccount.getId());
        assertTrue(updatedAccount.isPresent());
        assertEquals("John Smith", updatedAccount.get().getAccountHolderName());
        assertEquals(new BigDecimal("1500.00"), updatedAccount.get().getBalance());
    }

    @Test
    @DisplayName("Test delete bank account")
    void testDeleteBankAccount() {
        assertTrue(bankAccountRepository.existsById(testAccount.getId()));
        
        bankAccountRepository.deleteById(testAccount.getId());
        
        assertFalse(bankAccountRepository.existsById(testAccount.getId()));
    }

    @Test
    @DisplayName("Test bank account deposit")
    void testBankAccountDeposit() {
        BigDecimal depositAmount = new BigDecimal("500.00");
        BigDecimal expectedBalance = INITIAL_BALANCE.add(depositAmount);
        
        testAccount.deposit(depositAmount);
        bankAccountRepository.save(testAccount);
        
        Optional<BankAccount> updatedAccount = bankAccountRepository.findById(testAccount.getId());
        assertTrue(updatedAccount.isPresent());
        assertEquals(expectedBalance, updatedAccount.get().getBalance());
    }

    @Test
    @DisplayName("Test bank account withdrawal")
    void testBankAccountWithdrawal() {
        BigDecimal withdrawalAmount = new BigDecimal("300.00");
        BigDecimal expectedBalance = INITIAL_BALANCE.subtract(withdrawalAmount);
        
        boolean result = testAccount.withdraw(withdrawalAmount);
        bankAccountRepository.save(testAccount);
        
        assertTrue(result);
        Optional<BankAccount> updatedAccount = bankAccountRepository.findById(testAccount.getId());
        assertTrue(updatedAccount.isPresent());
        assertEquals(expectedBalance, updatedAccount.get().getBalance());
    }

    @Test
    @DisplayName("Test bank account withdrawal with insufficient funds")
    void testBankAccountWithdrawalWithInsufficientFunds() {
        BigDecimal excessiveAmount = INITIAL_BALANCE.add(new BigDecimal("100.00"));
        
        boolean result = testAccount.withdraw(excessiveAmount);
        bankAccountRepository.save(testAccount);
        
        assertFalse(result);
        Optional<BankAccount> updatedAccount = bankAccountRepository.findById(testAccount.getId());
        assertTrue(updatedAccount.isPresent());
        assertEquals(INITIAL_BALANCE, updatedAccount.get().getBalance());
    }
}