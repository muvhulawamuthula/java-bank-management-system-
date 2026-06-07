package com.bankafrica.bankingapp.service;

import com.bankafrica.bankingapp.model.User;
import com.bankafrica.bankingapp.repository.BankAccountRepository;
import com.bankafrica.bankingapp.repository.TransactionRepository;
import com.bankafrica.bankingapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;

/**
 * Tests for input validation and edge cases in the AuthService.
 */
public class AuthServiceValidationTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private AuthService authService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        authService = new AuthService(userRepository, bankAccountRepository,
                transactionRepository, passwordEncoder);

        // Allow registration to proceed past the uniqueness checks (no existing users).
        lenient().when(userRepository.existsByEmail(anyString())).thenReturn(false);
        lenient().when(userRepository.existsByIdNumber(anyString())).thenReturn(false);
        lenient().when(bankAccountRepository.existsByAccountNumber(anyString())).thenReturn(false);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Test registration with invalid first name")
    void testRegisterUserWithInvalidFirstName(String firstName) {
        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.registerUser(
                firstName,
                "Doe",
                "john.doe@example.com",
                "9001015000000",
                "0712345678",
                "password123",
                new BigDecimal("500.00")
            );
        });
        
        assertTrue(exception.getMessage().contains("First name cannot be empty"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Test registration with invalid last name")
    void testRegisterUserWithInvalidLastName(String lastName) {
        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.registerUser(
                "John",
                lastName,
                "john.doe@example.com",
                "9001015000000",
                "0712345678",
                "password123",
                new BigDecimal("500.00")
            );
        });
        
        assertTrue(exception.getMessage().contains("Last name cannot be empty"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "invalidemail", "email@", "@domain.com"})
    @DisplayName("Test registration with invalid email")
    void testRegisterUserWithInvalidEmail(String email) {
        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.registerUser(
                "John",
                "Doe",
                email,
                "9001015000000",
                "0712345678",
                "password123",
                new BigDecimal("500.00")
            );
        });
        
        assertTrue(exception.getMessage().contains("Invalid email format"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "12345", "abcdefghij"})
    @DisplayName("Test registration with invalid ID number")
    void testRegisterUserWithInvalidIdNumber(String idNumber) {
        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.registerUser(
                "John",
                "Doe",
                "john.doe@example.com",
                idNumber,
                "0712345678",
                "password123",
                new BigDecimal("500.00")
            );
        });
        
        assertTrue(exception.getMessage().contains("Invalid ID number format"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "12345", "abcdefghij"})
    @DisplayName("Test registration with invalid phone number")
    void testRegisterUserWithInvalidPhoneNumber(String phoneNumber) {
        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.registerUser(
                "John",
                "Doe",
                "john.doe@example.com",
                "9001015000000",
                phoneNumber,
                "password123",
                new BigDecimal("500.00")
            );
        });
        
        assertTrue(exception.getMessage().contains("Invalid phone number format"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "123", "weak"})
    @DisplayName("Test registration with invalid password")
    void testRegisterUserWithInvalidPassword(String password) {
        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.registerUser(
                "John",
                "Doe",
                "john.doe@example.com",
                "9001015000000",
                "0712345678",
                password,
                new BigDecimal("500.00")
            );
        });
        
        assertTrue(exception.getMessage().contains("Password must be at least 6 characters"));
    }

    @Test
    @DisplayName("Test registration with null initial deposit")
    void testRegisterUserWithNullInitialDeposit() {
        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.registerUser(
                "John",
                "Doe",
                "john.doe@example.com",
                "9001015000000",
                "0712345678",
                "password123",
                null
            );
        });
        
        assertTrue(exception.getMessage().contains("Initial deposit cannot be null"));
    }

    @Test
    @DisplayName("Test registration with negative initial deposit")
    void testRegisterUserWithNegativeInitialDeposit() {
        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.registerUser(
                "John",
                "Doe",
                "john.doe@example.com",
                "9001015000000",
                "0712345678",
                "password123",
                new BigDecimal("-100.00")
            );
        });
        
        assertTrue(exception.getMessage().contains("Initial deposit must be positive"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "invalidemail", "email@", "@domain.com"})
    @DisplayName("Test login with invalid email")
    void testLoginWithInvalidEmail(String email) {
        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.loginUser(email, "password123");
        });
        
        assertTrue(exception.getMessage().contains("Invalid email format"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Test login with invalid password")
    void testLoginWithInvalidPassword(String password) {
        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.loginUser("john.doe@example.com", password);
        });
        
        assertTrue(exception.getMessage().contains("Password cannot be empty"));
    }

    @Test
    @DisplayName("Test get user by invalid ID")
    void testGetUserByInvalidId() {
        // Call the service method with invalid ID
        User user = authService.getUserById(-1L);
        
        // Verify that null is returned
        assertNull(user);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n", "invalidemail", "email@", "@domain.com"})
    @DisplayName("Test get user by invalid email")
    void testGetUserByInvalidEmail(String email) {
        // Call the service method with invalid email
        User user = authService.getUserByEmail(email);
        
        // Verify that null is returned
        assertNull(user);
    }
}