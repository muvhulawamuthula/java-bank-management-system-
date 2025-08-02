package com.bankafrica.bankingapp.service;

import com.bankafrica.bankingapp.BaseTest;
import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.User;
import com.bankafrica.bankingapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the AuthService class.
 */
@SpringBootTest
class AuthServiceTest extends BaseTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private final String FIRST_NAME = "John";
    private final String LAST_NAME = "Doe";
    private final String EMAIL = "john.doe@example.com";
    private final String ID_NUMBER = "9001015000000";
    private final String PHONE_NUMBER = "0712345678";
    private final String PASSWORD = "securepassword";
    private final BigDecimal INITIAL_DEPOSIT = new BigDecimal("500.00");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create a test user
        testUser = new User(FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD);
        BankAccount bankAccount = new BankAccount(FIRST_NAME + " " + LAST_NAME, INITIAL_DEPOSIT);
        testUser.setBankAccount(bankAccount);
    }

    @Test
    @DisplayName("Test successful user registration")
    void testRegisterUserSuccess() throws Exception {
        // Mock repository behavior
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByIdNumber(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Call the service method
        User registeredUser = authService.registerUser(
                FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD, INITIAL_DEPOSIT);

        // Verify the result
        assertNotNull(registeredUser);
        assertEquals(FIRST_NAME, registeredUser.getFirstName());
        assertEquals(LAST_NAME, registeredUser.getLastName());
        assertEquals(EMAIL, registeredUser.getEmail());
        assertEquals(ID_NUMBER, registeredUser.getIdNumber());
        assertEquals(PHONE_NUMBER, registeredUser.getPhoneNumber());
        assertEquals(PASSWORD, registeredUser.getPassword());
        
        // Verify bank account
        assertNotNull(registeredUser.getBankAccount());
        assertEquals(FIRST_NAME + " " + LAST_NAME, registeredUser.getBankAccount().getAccountHolderName());
        assertEquals(INITIAL_DEPOSIT, registeredUser.getBankAccount().getBalance());

        // Verify repository interactions
        verify(userRepository).existsByEmail(EMAIL);
        verify(userRepository).existsByIdNumber(ID_NUMBER);
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Test registration with existing email")
    void testRegisterUserWithExistingEmail() {
        // Mock repository behavior
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.registerUser(FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD, INITIAL_DEPOSIT);
        });

        assertEquals("Email already registered", exception.getMessage());

        // Verify repository interactions
        verify(userRepository).existsByEmail(EMAIL);
        verify(userRepository, never()).existsByIdNumber(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Test registration with existing ID number")
    void testRegisterUserWithExistingIdNumber() {
        // Mock repository behavior
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByIdNumber(ID_NUMBER)).thenReturn(true);

        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.registerUser(FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD, INITIAL_DEPOSIT);
        });

        assertEquals("ID number already registered", exception.getMessage());

        // Verify repository interactions
        verify(userRepository).existsByEmail(EMAIL);
        verify(userRepository).existsByIdNumber(ID_NUMBER);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Test registration with insufficient initial deposit")
    void testRegisterUserWithInsufficientDeposit() {
        // Mock repository behavior
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByIdNumber(anyString())).thenReturn(false);

        // Call the service method with insufficient deposit
        BigDecimal insufficientDeposit = new BigDecimal("50.00");
        Exception exception = assertThrows(Exception.class, () -> {
            authService.registerUser(FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD, insufficientDeposit);
        });

        assertEquals("Minimum initial deposit is R100.00", exception.getMessage());

        // Verify repository interactions
        verify(userRepository).existsByEmail(EMAIL);
        verify(userRepository).existsByIdNumber(ID_NUMBER);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Test successful user login")
    void testLoginUserSuccess() throws Exception {
        // Mock repository behavior
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

        // Call the service method
        User loggedInUser = authService.loginUser(EMAIL, PASSWORD);

        // Verify the result
        assertNotNull(loggedInUser);
        assertEquals(testUser, loggedInUser);

        // Verify repository interactions
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("Test login with non-existent email")
    void testLoginUserWithNonExistentEmail() {
        // Mock repository behavior
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Call the service method and verify exception
        Exception exception = assertThrows(Exception.class, () -> {
            authService.loginUser(EMAIL, PASSWORD);
        });

        assertEquals("Invalid email or password", exception.getMessage());

        // Verify repository interactions
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("Test login with incorrect password")
    void testLoginUserWithIncorrectPassword() {
        // Mock repository behavior
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

        // Call the service method with incorrect password
        String incorrectPassword = "wrongpassword";
        Exception exception = assertThrows(Exception.class, () -> {
            authService.loginUser(EMAIL, incorrectPassword);
        });

        assertEquals("Invalid email or password", exception.getMessage());

        // Verify repository interactions
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("Test get user by ID")
    void testGetUserById() {
        // Mock repository behavior
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        // Call the service method for existing user
        User foundUser = authService.getUserById(1L);
        
        // Verify the result
        assertNotNull(foundUser);
        assertEquals(testUser, foundUser);

        // Call the service method for non-existent user
        User notFoundUser = authService.getUserById(2L);
        
        // Verify the result
        assertNull(notFoundUser);

        // Verify repository interactions
        verify(userRepository).findById(1L);
        verify(userRepository).findById(2L);
    }

    @Test
    @DisplayName("Test get user by email")
    void testGetUserByEmail() {
        // Mock repository behavior
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Call the service method for existing user
        User foundUser = authService.getUserByEmail(EMAIL);
        
        // Verify the result
        assertNotNull(foundUser);
        assertEquals(testUser, foundUser);

        // Call the service method for non-existent user
        User notFoundUser = authService.getUserByEmail("nonexistent@example.com");
        
        // Verify the result
        assertNull(notFoundUser);

        // Verify repository interactions
        verify(userRepository).findByEmail(EMAIL);
        verify(userRepository).findByEmail("nonexistent@example.com");
    }
}