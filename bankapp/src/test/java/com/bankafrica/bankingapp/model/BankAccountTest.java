package com.bankafrica.bankingapp.model;

import com.bankafrica.bankingapp.BaseTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the BankAccount model class.
 */
class BankAccountTest extends BaseTest {

    private BankAccount bankAccount;
    private final String ACCOUNT_HOLDER_NAME = "Test User";
    private final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");

    @BeforeEach
    void setUp() {
        // Create a fresh bank account before each test
        bankAccount = new BankAccount(ACCOUNT_HOLDER_NAME, INITIAL_BALANCE);
    }

    @Test
    @DisplayName("Test BankAccount constructor and getters")
    void testConstructorAndGetters() {
        // Verify the constructor sets the properties correctly
        assertEquals(ACCOUNT_HOLDER_NAME, bankAccount.getAccountHolderName());
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
        assertNotNull(bankAccount.getCreatedAt());
        assertNotNull(bankAccount.getAccountNumber());
        assertEquals(10, bankAccount.getAccountNumber().length());
    }

    @Test
    @DisplayName("Test BankAccount setters")
    void testSetters() {
        // Test setting a new account holder name
        String newName = "New Test User";
        bankAccount.setAccountHolderName(newName);
        assertEquals(newName, bankAccount.getAccountHolderName());

        // Test setting a new balance
        BigDecimal newBalance = new BigDecimal("2000.00");
        bankAccount.setBalance(newBalance);
        assertEquals(newBalance, bankAccount.getBalance());

        // Test setting a new account number
        String newAccountNumber = "9876543210";
        bankAccount.setAccountNumber(newAccountNumber);
        assertEquals(newAccountNumber, bankAccount.getAccountNumber());
    }

    @Test
    @DisplayName("Test deposit with valid amount")
    void testDepositWithValidAmount() {
        // Test depositing a positive amount
        BigDecimal depositAmount = new BigDecimal("500.00");
        BigDecimal expectedBalance = INITIAL_BALANCE.add(depositAmount);
        
        bankAccount.deposit(depositAmount);
        
        assertEquals(expectedBalance, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test deposit with zero amount")
    void testDepositWithZeroAmount() {
        // Test depositing zero - should not change the balance
        BigDecimal depositAmount = BigDecimal.ZERO;
        
        bankAccount.deposit(depositAmount);
        
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test deposit with negative amount")
    void testDepositWithNegativeAmount() {
        // Test depositing a negative amount - should not change the balance
        BigDecimal depositAmount = new BigDecimal("-100.00");
        
        bankAccount.deposit(depositAmount);
        
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test deposit with null amount")
    void testDepositWithNullAmount() {
        // Test depositing null - should not change the balance
        bankAccount.deposit(null);
        
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test withdraw with valid amount")
    void testWithdrawWithValidAmount() {
        // Test withdrawing a positive amount less than the balance
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        BigDecimal expectedBalance = INITIAL_BALANCE.subtract(withdrawAmount);
        
        boolean result = bankAccount.withdraw(withdrawAmount);
        
        assertTrue(result);
        assertEquals(expectedBalance, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test withdraw with amount equal to balance")
    void testWithdrawWithAmountEqualToBalance() {
        // Test withdrawing an amount equal to the balance
        boolean result = bankAccount.withdraw(INITIAL_BALANCE);
        
        assertTrue(result);
        assertEquals(BigDecimal.ZERO, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test withdraw with amount greater than balance")
    void testWithdrawWithAmountGreaterThanBalance() {
        // Test withdrawing an amount greater than the balance
        BigDecimal withdrawAmount = INITIAL_BALANCE.add(new BigDecimal("100.00"));
        
        boolean result = bankAccount.withdraw(withdrawAmount);
        
        assertFalse(result);
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test withdraw with zero amount")
    void testWithdrawWithZeroAmount() {
        // Test withdrawing zero - should not change the balance and return false
        boolean result = bankAccount.withdraw(BigDecimal.ZERO);
        
        assertFalse(result);
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test withdraw with negative amount")
    void testWithdrawWithNegativeAmount() {
        // Test withdrawing a negative amount - should not change the balance and return false
        BigDecimal withdrawAmount = new BigDecimal("-100.00");
        
        boolean result = bankAccount.withdraw(withdrawAmount);
        
        assertFalse(result);
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test withdraw with null amount")
    void testWithdrawWithNullAmount() {
        // Test withdrawing null - should not change the balance and return false
        boolean result = bankAccount.withdraw(null);
        
        assertFalse(result);
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test toString method")
    void testToString() {
        // Test that toString returns a non-null string containing key information
        String toString = bankAccount.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains(ACCOUNT_HOLDER_NAME));
        assertTrue(toString.contains(INITIAL_BALANCE.toString()));
        assertTrue(toString.contains(bankAccount.getAccountNumber()));
    }
}