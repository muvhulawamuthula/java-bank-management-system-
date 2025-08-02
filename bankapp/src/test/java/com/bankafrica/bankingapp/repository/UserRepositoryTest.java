package com.bankafrica.bankingapp.repository;

import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.User;
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
 * Integration tests for the UserRepository.
 * Uses @DataJpaTest to test the repository with an in-memory database.
 */
@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

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
        // Create a test user
        testUser = new User(FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD);
        
        // Create a bank account for the user
        BankAccount bankAccount = new BankAccount(FIRST_NAME + " " + LAST_NAME, new BigDecimal("1000.00"));
        testUser.setBankAccount(bankAccount);
        
        // Save the user to the database
        entityManager.persist(testUser);
        entityManager.flush();
    }

    @Test
    @DisplayName("Test find user by email")
    void testFindByEmail() {
        // Call the repository method
        Optional<User> foundUser = userRepository.findByEmail(EMAIL);
        
        // Verify the result
        assertTrue(foundUser.isPresent());
        assertEquals(FIRST_NAME, foundUser.get().getFirstName());
        assertEquals(LAST_NAME, foundUser.get().getLastName());
        assertEquals(EMAIL, foundUser.get().getEmail());
        assertEquals(ID_NUMBER, foundUser.get().getIdNumber());
        assertEquals(PHONE_NUMBER, foundUser.get().getPhoneNumber());
        assertEquals(PASSWORD, foundUser.get().getPassword());
        
        // Verify bank account
        assertNotNull(foundUser.get().getBankAccount());
        assertEquals(FIRST_NAME + " " + LAST_NAME, foundUser.get().getBankAccount().getAccountHolderName());
        assertEquals(new BigDecimal("1000.00"), foundUser.get().getBankAccount().getBalance());
        
        // Test with non-existent email
        Optional<User> notFoundUser = userRepository.findByEmail("nonexistent@example.com");
        assertFalse(notFoundUser.isPresent());
    }

    @Test
    @DisplayName("Test find user by ID number")
    void testFindByIdNumber() {
        // Call the repository method
        Optional<User> foundUser = userRepository.findByIdNumber(ID_NUMBER);
        
        // Verify the result
        assertTrue(foundUser.isPresent());
        assertEquals(FIRST_NAME, foundUser.get().getFirstName());
        assertEquals(LAST_NAME, foundUser.get().getLastName());
        assertEquals(EMAIL, foundUser.get().getEmail());
        assertEquals(ID_NUMBER, foundUser.get().getIdNumber());
        
        // Test with non-existent ID number
        Optional<User> notFoundUser = userRepository.findByIdNumber("9901015000000");
        assertFalse(notFoundUser.isPresent());
    }

    @Test
    @DisplayName("Test exists by email")
    void testExistsByEmail() {
        // Call the repository method
        boolean exists = userRepository.existsByEmail(EMAIL);
        
        // Verify the result
        assertTrue(exists);
        
        // Test with non-existent email
        boolean notExists = userRepository.existsByEmail("nonexistent@example.com");
        assertFalse(notExists);
    }

    @Test
    @DisplayName("Test exists by ID number")
    void testExistsByIdNumber() {
        // Call the repository method
        boolean exists = userRepository.existsByIdNumber(ID_NUMBER);
        
        // Verify the result
        assertTrue(exists);
        
        // Test with non-existent ID number
        boolean notExists = userRepository.existsByIdNumber("9901015000000");
        assertFalse(notExists);
    }

    @Test
    @DisplayName("Test save user")
    void testSaveUser() {
        // Create a new user
        User newUser = new User("Jane", "Smith", "jane.smith@example.com", "9101015000000", "0723456789", "password123");
        BankAccount bankAccount = new BankAccount("Jane Smith", new BigDecimal("500.00"));
        newUser.setBankAccount(bankAccount);
        
        // Save the user
        User savedUser = userRepository.save(newUser);
        
        // Verify the result
        assertNotNull(savedUser.getId());
        assertEquals("Jane", savedUser.getFirstName());
        assertEquals("Smith", savedUser.getLastName());
        assertEquals("jane.smith@example.com", savedUser.getEmail());
        assertEquals("9101015000000", savedUser.getIdNumber());
        assertEquals("0723456789", savedUser.getPhoneNumber());
        assertEquals("password123", savedUser.getPassword());
        
        // Verify bank account
        assertNotNull(savedUser.getBankAccount());
        assertEquals("Jane Smith", savedUser.getBankAccount().getAccountHolderName());
        assertEquals(new BigDecimal("500.00"), savedUser.getBankAccount().getBalance());
        
        // Verify the user is in the database
        Optional<User> foundUser = userRepository.findById(savedUser.getId());
        assertTrue(foundUser.isPresent());
        assertEquals("jane.smith@example.com", foundUser.get().getEmail());
    }

    @Test
    @DisplayName("Test delete user")
    void testDeleteUser() {
        // Verify the user exists
        assertTrue(userRepository.existsById(testUser.getId()));
        
        // Delete the user
        userRepository.deleteById(testUser.getId());
        
        // Verify the user no longer exists
        assertFalse(userRepository.existsById(testUser.getId()));
    }
}