package com.bankafrica.bankingapp.service;

import com.bankafrica.bankingapp.exception.DuplicateResourceException;
import com.bankafrica.bankingapp.exception.InvalidCredentialsException;
import com.bankafrica.bankingapp.exception.InvalidRequestException;
import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.User;
import com.bankafrica.bankingapp.repository.BankAccountRepository;
import com.bankafrica.bankingapp.repository.TransactionRepository;
import com.bankafrica.bankingapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}. Uses a real {@link BCryptPasswordEncoder} so the
 * hashing path is genuinely exercised, with the repositories mocked.
 */
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
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
        authService = new AuthService(userRepository, bankAccountRepository,
                transactionRepository, passwordEncoder);

        // Stored password is a BCrypt hash, exactly as it would be in the database.
        testUser = new User(FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER,
                passwordEncoder.encode(PASSWORD));
        BankAccount bankAccount = new BankAccount(FIRST_NAME + " " + LAST_NAME, INITIAL_DEPOSIT);
        testUser.setBankAccount(bankAccount);
    }

    @Test
    @DisplayName("Test successful user registration")
    void testRegisterUserSuccess() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByIdNumber(anyString())).thenReturn(false);
        when(bankAccountRepository.existsByAccountNumber(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User registeredUser = authService.registerUser(
                FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD, INITIAL_DEPOSIT);

        assertNotNull(registeredUser);
        assertEquals(FIRST_NAME, registeredUser.getFirstName());
        assertEquals(LAST_NAME, registeredUser.getLastName());
        assertEquals(EMAIL, registeredUser.getEmail());
        assertEquals(ID_NUMBER, registeredUser.getIdNumber());
        assertEquals(PHONE_NUMBER, registeredUser.getPhoneNumber());

        // Password must be stored hashed, never in plaintext.
        assertNotEquals(PASSWORD, registeredUser.getPassword());
        assertTrue(passwordEncoder.matches(PASSWORD, registeredUser.getPassword()));

        assertNotNull(registeredUser.getBankAccount());
        assertEquals(FIRST_NAME + " " + LAST_NAME, registeredUser.getBankAccount().getAccountHolderName());
        assertEquals(INITIAL_DEPOSIT, registeredUser.getBankAccount().getBalance());

        verify(userRepository).existsByEmail(EMAIL);
        verify(userRepository).existsByIdNumber(ID_NUMBER);
        verify(userRepository).save(any(User.class));
        // Opening balance is recorded on the ledger.
        verify(transactionRepository).save(any());
    }

    @Test
    @DisplayName("Test registration with existing email")
    void testRegisterUserWithExistingEmail() {
        when(userRepository.existsByEmail(EMAIL)).thenReturn(true);

        Exception exception = assertThrows(DuplicateResourceException.class, () ->
                authService.registerUser(FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD, INITIAL_DEPOSIT));

        assertEquals("Email already registered", exception.getMessage());
        verify(userRepository).existsByEmail(EMAIL);
        verify(userRepository, never()).existsByIdNumber(anyString());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Test registration with existing ID number")
    void testRegisterUserWithExistingIdNumber() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByIdNumber(ID_NUMBER)).thenReturn(true);

        Exception exception = assertThrows(DuplicateResourceException.class, () ->
                authService.registerUser(FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD, INITIAL_DEPOSIT));

        assertEquals("ID number already registered", exception.getMessage());
        verify(userRepository).existsByEmail(EMAIL);
        verify(userRepository).existsByIdNumber(ID_NUMBER);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Test registration with insufficient initial deposit")
    void testRegisterUserWithInsufficientDeposit() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByIdNumber(anyString())).thenReturn(false);

        BigDecimal insufficientDeposit = new BigDecimal("50.00");
        Exception exception = assertThrows(InvalidRequestException.class, () ->
                authService.registerUser(FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD, insufficientDeposit));

        assertEquals("Minimum initial deposit is R100.00", exception.getMessage());
        verify(userRepository).existsByEmail(EMAIL);
        verify(userRepository).existsByIdNumber(ID_NUMBER);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("Test successful user login")
    void testLoginUserSuccess() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

        User loggedInUser = authService.loginUser(EMAIL, PASSWORD);

        assertNotNull(loggedInUser);
        assertEquals(testUser, loggedInUser);
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("Test login with non-existent email")
    void testLoginUserWithNonExistentEmail() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        Exception exception = assertThrows(InvalidCredentialsException.class, () ->
                authService.loginUser(EMAIL, PASSWORD));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("Test login with incorrect password")
    void testLoginUserWithIncorrectPassword() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));

        Exception exception = assertThrows(InvalidCredentialsException.class, () ->
                authService.loginUser(EMAIL, "wrongpassword"));

        assertEquals("Invalid email or password", exception.getMessage());
        verify(userRepository).findByEmail(EMAIL);
    }

    @Test
    @DisplayName("Test get user by ID")
    void testGetUserById() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertEquals(testUser, authService.getUserById(1L));
        assertNull(authService.getUserById(2L));

        verify(userRepository).findById(1L);
        verify(userRepository).findById(2L);
    }

    @Test
    @DisplayName("Test get user by email")
    void testGetUserByEmail() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(testUser));
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        assertEquals(testUser, authService.getUserByEmail(EMAIL));
        assertNull(authService.getUserByEmail("nonexistent@example.com"));

        verify(userRepository).findByEmail(EMAIL);
        verify(userRepository).findByEmail("nonexistent@example.com");
    }
}
