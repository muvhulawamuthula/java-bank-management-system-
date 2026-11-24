package com.bankafrica.bankingapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the BankAccount model class. Pure POJO test — no Spring context required.
 */
class BankAccountTest {

    private BankAccount bankAccount;
    private final String ACCOUNT_HOLDER_NAME = "Test User";
    private final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");

    @BeforeEach
    void setUp() {
        bankAccount = new BankAccount(ACCOUNT_HOLDER_NAME, INITIAL_BALANCE);
    }

    @Test
    @DisplayName("Test BankAccount constructor and getters")
    void testConstructorAndGetters() {
        assertEquals(ACCOUNT_HOLDER_NAME, bankAccount.getAccountHolderName());
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
        assertNotNull(bankAccount.getCreatedAt());
        assertNotNull(bankAccount.getAccountNumber());
        assertEquals(10, bankAccount.getAccountNumber().length());
    }

    @Test
    @DisplayName("Test BankAccount setters")
    void testSetters() {
        String newName = "New Test User";
        bankAccount.setAccountHolderName(newName);
        assertEquals(newName, bankAccount.getAccountHolderName());

        BigDecimal newBalance = new BigDecimal("2000.00");
        bankAccount.setBalance(newBalance);
        assertEquals(newBalance, bankAccount.getBalance());

        String newAccountNumber = "9876543210";
        bankAccount.setAccountNumber(newAccountNumber);
        assertEquals(newAccountNumber, bankAccount.getAccountNumber());
    }

    @Test
    @DisplayName("Test deposit with valid amount")
    void testDepositWithValidAmount() {
        BigDecimal depositAmount = new BigDecimal("500.00");
        BigDecimal expectedBalance = INITIAL_BALANCE.add(depositAmount);
        
        bankAccount.deposit(depositAmount);
        
        assertEquals(expectedBalance, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test deposit with zero amount")
    void testDepositWithZeroAmount() {
        BigDecimal depositAmount = BigDecimal.ZERO;
        
        bankAccount.deposit(depositAmount);
        
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test deposit with negative amount")
    void testDepositWithNegativeAmount() {
        BigDecimal depositAmount = new BigDecimal("-100.00");
        
        bankAccount.deposit(depositAmount);
        
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test deposit with null amount")
    void testDepositWithNullAmount() {
        bankAccount.deposit(null);
        
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test withdraw with valid amount")
    void testWithdrawWithValidAmount() {
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        BigDecimal expectedBalance = INITIAL_BALANCE.subtract(withdrawAmount);
        
        boolean result = bankAccount.withdraw(withdrawAmount);
        
        assertTrue(result);
        assertEquals(expectedBalance, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test withdraw with amount equal to balance")
    void testWithdrawWithAmountEqualToBalance() {
        boolean result = bankAccount.withdraw(INITIAL_BALANCE);

        assertTrue(result);
        // Compare by value: 0.00 and ZERO are numerically equal but differ in scale.
        assertEquals(0, BigDecimal.ZERO.compareTo(bankAccount.getBalance()));
    }

    @Test
    @DisplayName("Test withdraw with amount greater than balance")
    void testWithdrawWithAmountGreaterThanBalance() {
        BigDecimal withdrawAmount = INITIAL_BALANCE.add(new BigDecimal("100.00"));
        
        boolean result = bankAccount.withdraw(withdrawAmount);
        
        assertFalse(result);
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test withdraw with zero amount")
    void testWithdrawWithZeroAmount() {
        boolean result = bankAccount.withdraw(BigDecimal.ZERO);
        
        assertFalse(result);
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test withdraw with negative amount")
    void testWithdrawWithNegativeAmount() {
        BigDecimal withdrawAmount = new BigDecimal("-100.00");
        
        boolean result = bankAccount.withdraw(withdrawAmount);
        
        assertFalse(result);
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test withdraw with null amount")
    void testWithdrawWithNullAmount() {
        boolean result = bankAccount.withdraw(null);
        
        assertFalse(result);
        assertEquals(INITIAL_BALANCE, bankAccount.getBalance());
    }

    @Test
    @DisplayName("Test toString method")
    void testToString() {
        String toString = bankAccount.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains(ACCOUNT_HOLDER_NAME));
        assertTrue(toString.contains(INITIAL_BALANCE.toString()));
        assertTrue(toString.contains(bankAccount.getAccountNumber()));
    }
}