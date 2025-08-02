package com.bankafrica.bankingapp.util;

import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Utility class for creating test data objects.
 * This helps maintain consistency across tests and makes tests more readable.
 */
public class TestDataFactory {

    /**
     * Creates a test User with random values.
     * 
     * @return A User object with test data
     */
    public static User createTestUser() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        User user = new User(
                "TestFirst" + uniqueId,
                "TestLast" + uniqueId,
                "test" + uniqueId + "@example.com",
                "ID" + uniqueId,
                "07" + uniqueId.substring(0, 8),
                "password" + uniqueId
        );
        return user;
    }
    
    /**
     * Creates a test User with specified values.
     * 
     * @param firstName First name
     * @param lastName Last name
     * @param email Email address
     * @param idNumber ID number
     * @param phoneNumber Phone number
     * @param password Password
     * @return A User object with the specified data
     */
    public static User createTestUser(String firstName, String lastName, String email, 
                                     String idNumber, String phoneNumber, String password) {
        return new User(firstName, lastName, email, idNumber, phoneNumber, password);
    }
    
    /**
     * Creates a test BankAccount with random values.
     * 
     * @return A BankAccount object with test data
     */
    public static BankAccount createTestBankAccount() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return new BankAccount("Test Account " + uniqueId, new BigDecimal("1000.00"));
    }
    
    /**
     * Creates a test BankAccount with specified values.
     * 
     * @param accountHolderName Account holder name
     * @param initialBalance Initial balance
     * @return A BankAccount object with the specified data
     */
    public static BankAccount createTestBankAccount(String accountHolderName, BigDecimal initialBalance) {
        return new BankAccount(accountHolderName, initialBalance);
    }
    
    /**
     * Creates a test User with an associated BankAccount.
     * 
     * @return A User object with an associated BankAccount
     */
    public static User createTestUserWithAccount() {
        User user = createTestUser();
        BankAccount account = createTestBankAccount(
                user.getFirstName() + " " + user.getLastName(), 
                new BigDecimal("500.00")
        );
        user.setBankAccount(account);
        return user;
    }
    
    /**
     * Creates a test User with an associated BankAccount with a specified balance.
     * 
     * @param balance The initial balance for the account
     * @return A User object with an associated BankAccount with the specified balance
     */
    public static User createTestUserWithAccount(BigDecimal balance) {
        User user = createTestUser();
        BankAccount account = createTestBankAccount(
                user.getFirstName() + " " + user.getLastName(), 
                balance
        );
        user.setBankAccount(account);
        return user;
    }
}