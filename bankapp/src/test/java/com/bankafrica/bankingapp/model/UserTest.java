package com.bankafrica.bankingapp.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the User model class. Pure POJO test — no Spring context required.
 */
class UserTest {

    private User user;
    private final String FIRST_NAME = "John";
    private final String LAST_NAME = "Doe";
    private final String EMAIL = "john.doe@example.com";
    private final String ID_NUMBER = "9001015000000";
    private final String PHONE_NUMBER = "0712345678";
    private final String PASSWORD = "securepassword";

    @BeforeEach
    void setUp() {
        user = new User(FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD);
    }

    @Test
    @DisplayName("Test User constructor and getters")
    void testConstructorAndGetters() {
        assertEquals(FIRST_NAME, user.getFirstName());
        assertEquals(LAST_NAME, user.getLastName());
        assertEquals(EMAIL, user.getEmail());
        assertEquals(ID_NUMBER, user.getIdNumber());
        assertEquals(PHONE_NUMBER, user.getPhoneNumber());
        assertEquals(PASSWORD, user.getPassword());
        assertNotNull(user.getCreatedAt());
        assertNull(user.getBankAccount());
    }

    @Test
    @DisplayName("Test User setters")
    void testSetters() {
        String newFirstName = "Jane";
        user.setFirstName(newFirstName);
        assertEquals(newFirstName, user.getFirstName());

        String newLastName = "Smith";
        user.setLastName(newLastName);
        assertEquals(newLastName, user.getLastName());

        String newEmail = "jane.smith@example.com";
        user.setEmail(newEmail);
        assertEquals(newEmail, user.getEmail());

        String newIdNumber = "9101015000000";
        user.setIdNumber(newIdNumber);
        assertEquals(newIdNumber, user.getIdNumber());

        String newPhoneNumber = "0723456789";
        user.setPhoneNumber(newPhoneNumber);
        assertEquals(newPhoneNumber, user.getPhoneNumber());

        String newPassword = "newsecurepassword";
        user.setPassword(newPassword);
        assertEquals(newPassword, user.getPassword());

        LocalDateTime newCreatedAt = LocalDateTime.now().minusDays(1);
        user.setCreatedAt(newCreatedAt);
        assertEquals(newCreatedAt, user.getCreatedAt());
    }

    @Test
    @DisplayName("Test setting and getting bank account")
    void testBankAccount() {
        BankAccount bankAccount = new BankAccount(FIRST_NAME + " " + LAST_NAME, new BigDecimal("1000.00"));
        
        user.setBankAccount(bankAccount);
        
        assertNotNull(user.getBankAccount());
        assertEquals(bankAccount, user.getBankAccount());
        assertEquals(FIRST_NAME + " " + LAST_NAME, user.getBankAccount().getAccountHolderName());
        assertEquals(new BigDecimal("1000.00"), user.getBankAccount().getBalance());
    }

    @Test
    @DisplayName("Test toString method")
    void testToString() {
        String toString = user.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains(FIRST_NAME));
        assertTrue(toString.contains(LAST_NAME));
        assertTrue(toString.contains(EMAIL));
        assertTrue(toString.contains(ID_NUMBER));
        assertTrue(toString.contains(PHONE_NUMBER));
    }

    @Test
    @DisplayName("Test default constructor")
    void testDefaultConstructor() {
        User defaultUser = new User();
        
        assertNotNull(defaultUser.getCreatedAt());
        
        assertNull(defaultUser.getFirstName());
        assertNull(defaultUser.getLastName());
        assertNull(defaultUser.getEmail());
        assertNull(defaultUser.getIdNumber());
        assertNull(defaultUser.getPhoneNumber());
        assertNull(defaultUser.getPassword());
        assertNull(defaultUser.getBankAccount());
    }
}