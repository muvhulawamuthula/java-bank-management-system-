package com.bankafrica.bankingapp.util;

import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class TestDataFactory {

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
    
    public static User createTestUser(String firstName, String lastName, String email, 
                                     String idNumber, String phoneNumber, String password) {
        return new User(firstName, lastName, email, idNumber, phoneNumber, password);
    }
    
    public static BankAccount createTestBankAccount() {
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return new BankAccount("Test Account " + uniqueId, new BigDecimal("1000.00"));
    }
    
    public static BankAccount createTestBankAccount(String accountHolderName, BigDecimal initialBalance) {
        return new BankAccount(accountHolderName, initialBalance);
    }
    
    public static User createTestUserWithAccount() {
        User user = createTestUser();
        BankAccount account = createTestBankAccount(
                user.getFirstName() + " " + user.getLastName(), 
                new BigDecimal("500.00")
        );
        user.setBankAccount(account);
        return user;
    }
    
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