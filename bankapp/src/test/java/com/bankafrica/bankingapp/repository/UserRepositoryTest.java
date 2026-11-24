package com.bankafrica.bankingapp.repository;

import com.bankafrica.bankingapp.BaseTest;
import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the UserRepository, running against the shared in-memory
 * database with each test rolled back.
 */
@Transactional
class UserRepositoryTest extends BaseTest {

    @Autowired
    private UserRepository userRepository;

    private User testUser;
    private final String FIRST_NAME = "John";
    private final String LAST_NAME = "Doe";
    private final String EMAIL = "john.doe@example.com";
    private final String ID_NUMBER = "9001015000000";
    private final String PHONE_NUMBER = "0712345678";
    private final String PASSWORD = "securepassword";

    @BeforeEach
    void setUp() {
        testUser = new User(FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD);
        
        BankAccount bankAccount = new BankAccount(FIRST_NAME + " " + LAST_NAME, new BigDecimal("1000.00"));
        testUser.setBankAccount(bankAccount);
        
        userRepository.saveAndFlush(testUser);
    }

    @Test
    @DisplayName("Test find user by email")
    void testFindByEmail() {
        Optional<User> foundUser = userRepository.findByEmail(EMAIL);
        
        assertTrue(foundUser.isPresent());
        assertEquals(FIRST_NAME, foundUser.get().getFirstName());
        assertEquals(LAST_NAME, foundUser.get().getLastName());
        assertEquals(EMAIL, foundUser.get().getEmail());
        assertEquals(ID_NUMBER, foundUser.get().getIdNumber());
        assertEquals(PHONE_NUMBER, foundUser.get().getPhoneNumber());
        assertEquals(PASSWORD, foundUser.get().getPassword());
        
        assertNotNull(foundUser.get().getBankAccount());
        assertEquals(FIRST_NAME + " " + LAST_NAME, foundUser.get().getBankAccount().getAccountHolderName());
        assertEquals(new BigDecimal("1000.00"), foundUser.get().getBankAccount().getBalance());
        
        Optional<User> notFoundUser = userRepository.findByEmail("nonexistent@example.com");
        assertFalse(notFoundUser.isPresent());
    }

    @Test
    @DisplayName("Test find user by ID number")
    void testFindByIdNumber() {
        Optional<User> foundUser = userRepository.findByIdNumber(ID_NUMBER);
        
        assertTrue(foundUser.isPresent());
        assertEquals(FIRST_NAME, foundUser.get().getFirstName());
        assertEquals(LAST_NAME, foundUser.get().getLastName());
        assertEquals(EMAIL, foundUser.get().getEmail());
        assertEquals(ID_NUMBER, foundUser.get().getIdNumber());
        
        Optional<User> notFoundUser = userRepository.findByIdNumber("9901015000000");
        assertFalse(notFoundUser.isPresent());
    }

    @Test
    @DisplayName("Test exists by email")
    void testExistsByEmail() {
        boolean exists = userRepository.existsByEmail(EMAIL);
        
        assertTrue(exists);
        
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");
        assertFalse(notExists);
    }

    @Test
    @DisplayName("Test exists by ID number")
    void testExistsByIdNumber() {
        boolean exists = userRepository.existsByIdNumber(ID_NUMBER);
        
        assertTrue(exists);
        
        boolean notExists = userRepository.existsByIdNumber("9901015000000");
        assertFalse(notExists);
    }

    @Test
    @DisplayName("Test save user")
    void testSaveUser() {
        User newUser = new User("Jane", "Smith", "jane.smith@example.com", "9101015000000", "0723456789", "password123");
        BankAccount bankAccount = new BankAccount("Jane Smith", new BigDecimal("500.00"));
        newUser.setBankAccount(bankAccount);
        
        User savedUser = userRepository.save(newUser);
        
        assertNotNull(savedUser.getId());
        assertEquals("Jane", savedUser.getFirstName());
        assertEquals("Smith", savedUser.getLastName());
        assertEquals("jane.smith@example.com", savedUser.getEmail());
        assertEquals("9101015000000", savedUser.getIdNumber());
        assertEquals("0723456789", savedUser.getPhoneNumber());
        assertEquals("password123", savedUser.getPassword());
        
        assertNotNull(savedUser.getBankAccount());
        assertEquals("Jane Smith", savedUser.getBankAccount().getAccountHolderName());
        assertEquals(new BigDecimal("500.00"), savedUser.getBankAccount().getBalance());
        
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals("jane.smith@example.com", foundUser.get().getEmail());
    }

    @Test
    @DisplayName("Test delete user")
    void testDeleteUser() {
        assertTrue(userRepository.existsById(testUser.getId()));
        
        userRepository.deleteById(testUser.getId());
        
        assertFalse(userRepository.existsById(testUser.getId()));
    }
}