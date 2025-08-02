package com.bankafrica.bankingapp.service;

import com.bankafrica.bankingapp.BaseTest;
import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.repository.BankAccountRepository;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for the BankingService class.
 */
@SpringBootTest
class BankingServiceTest extends BaseTest {

    @Mock
    private BankAccountRepository bankAccountRepository;

    @InjectMocks
    private BankingService bankingService;

    private BankAccount testAccount;
    private final String ACCOUNT_HOLDER_NAME = "John Doe";
    private final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");
    private final Long ACCOUNT_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Create a test account
        testAccount = new BankAccount(ACCOUNT_HOLDER_NAME, INITIAL_BALANCE);
        testAccount.setId(ACCOUNT_ID);
    }

    @Test
    @DisplayName("Test get account by ID")
    void testGetAccount() {
        // Mock repository behavior
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
        when(bankAccountRepository.findById(2L)).thenReturn(Optional.empty());

        // Call the service method for existing account
        BankAccount foundAccount = bankingService.getAccount(ACCOUNT_ID);
        
        // Verify the result
        assertNotNull(foundAccount);
        assertEquals(testAccount, foundAccount);
        assertEquals(ACCOUNT_HOLDER_NAME, foundAccount.getAccountHolderName());
        assertEquals(INITIAL_BALANCE, foundAccount.getBalance());

        // Call the service method for non-existent account
        BankAccount notFoundAccount = bankingService.getAccount(2L);
        
        // Verify the result
        assertNull(notFoundAccount);

        // Verify repository interactions
        verify(bankAccountRepository).findById(ACCOUNT_ID);
        verify(bankAccountRepository).findById(2L);
    }

    @Test
    @DisplayName("Test successful deposit")
    void testDepositSuccess() {
        // Mock repository behavior
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call the service method
        BigDecimal depositAmount = new BigDecimal("500.00");
        BankAccount updatedAccount = bankingService.deposit(ACCOUNT_ID, depositAmount);

        // Verify the result
        assertNotNull(updatedAccount);
        assertEquals(INITIAL_BALANCE.add(depositAmount), updatedAccount.getBalance());

        // Verify repository interactions
        verify(bankAccountRepository).findById(ACCOUNT_ID);
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test deposit with non-existent account")
    void testDepositWithNonExistentAccount() {
        // Mock repository behavior
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        // Call the service method and verify exception
        BigDecimal depositAmount = new BigDecimal("500.00");
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bankingService.deposit(ACCOUNT_ID, depositAmount);
        });

        assertEquals("Account not found with ID: " + ACCOUNT_ID, exception.getMessage());

        // Verify repository interactions
        verify(bankAccountRepository).findById(ACCOUNT_ID);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test deposit with negative amount")
    void testDepositWithNegativeAmount() {
        // Mock repository behavior
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));

        // Call the service method and verify exception
        BigDecimal negativeAmount = new BigDecimal("-100.00");
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bankingService.deposit(ACCOUNT_ID, negativeAmount);
        });

        assertEquals("Deposit amount must be positive", exception.getMessage());

        // Verify repository interactions
        verify(bankAccountRepository).findById(ACCOUNT_ID);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test deposit with zero amount")
    void testDepositWithZeroAmount() {
        // Mock repository behavior
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));

        // Call the service method and verify exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bankingService.deposit(ACCOUNT_ID, BigDecimal.ZERO);
        });

        assertEquals("Deposit amount must be positive", exception.getMessage());

        // Verify repository interactions
        verify(bankAccountRepository).findById(ACCOUNT_ID);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test successful withdrawal")
    void testWithdrawSuccess() {
        // Mock repository behavior
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Call the service method
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        BankAccount updatedAccount = bankingService.withdraw(ACCOUNT_ID, withdrawAmount);

        // Verify the result
        assertNotNull(updatedAccount);
        assertEquals(INITIAL_BALANCE.subtract(withdrawAmount), updatedAccount.getBalance());

        // Verify repository interactions
        verify(bankAccountRepository).findById(ACCOUNT_ID);
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test withdrawal with non-existent account")
    void testWithdrawWithNonExistentAccount() {
        // Mock repository behavior
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

        // Call the service method and verify exception
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bankingService.withdraw(ACCOUNT_ID, withdrawAmount);
        });

        assertEquals("Account not found with ID: " + ACCOUNT_ID, exception.getMessage());

        // Verify repository interactions
        verify(bankAccountRepository).findById(ACCOUNT_ID);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test withdrawal with negative amount")
    void testWithdrawWithNegativeAmount() {
        // Mock repository behavior
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));

        // Call the service method and verify exception
        BigDecimal negativeAmount = new BigDecimal("-100.00");
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bankingService.withdraw(ACCOUNT_ID, negativeAmount);
        });

        assertEquals("Withdrawal amount must be positive", exception.getMessage());

        // Verify repository interactions
        verify(bankAccountRepository).findById(ACCOUNT_ID);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test withdrawal with zero amount")
    void testWithdrawWithZeroAmount() {
        // Mock repository behavior
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));

        // Call the service method and verify exception
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bankingService.withdraw(ACCOUNT_ID, BigDecimal.ZERO);
        });

        assertEquals("Withdrawal amount must be positive", exception.getMessage());

        // Verify repository interactions
        verify(bankAccountRepository).findById(ACCOUNT_ID);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test withdrawal with insufficient funds")
    void testWithdrawWithInsufficientFunds() {
        // Mock repository behavior
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));

        // Call the service method and verify exception
        BigDecimal excessiveAmount = INITIAL_BALANCE.add(new BigDecimal("100.00"));
        Exception exception = assertThrows(RuntimeException.class, () -> {
            bankingService.withdraw(ACCOUNT_ID, excessiveAmount);
        });

        assertTrue(exception.getMessage().startsWith("Insufficient funds. Current balance: $"));

        // Verify repository interactions
        verify(bankAccountRepository).findById(ACCOUNT_ID);
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test create account")
    void testCreateAccount() {
        // Mock repository behavior
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount savedAccount = invocation.getArgument(0);
            savedAccount.setId(ACCOUNT_ID);
            return savedAccount;
        });

        // Call the service method
        BankAccount createdAccount = bankingService.createAccount(ACCOUNT_HOLDER_NAME, INITIAL_BALANCE);

        // Verify the result
        assertNotNull(createdAccount);
        assertEquals(ACCOUNT_HOLDER_NAME, createdAccount.getAccountHolderName());
        assertEquals(INITIAL_BALANCE, createdAccount.getBalance());
        assertEquals(ACCOUNT_ID, createdAccount.getId());

        // Verify repository interactions
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test create account with null balance")
    void testCreateAccountWithNullBalance() {
        // Mock repository behavior
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount savedAccount = invocation.getArgument(0);
            savedAccount.setId(ACCOUNT_ID);
            return savedAccount;
        });

        // Call the service method
        BankAccount createdAccount = bankingService.createAccount(ACCOUNT_HOLDER_NAME, null);

        // Verify the result
        assertNotNull(createdAccount);
        assertEquals(ACCOUNT_HOLDER_NAME, createdAccount.getAccountHolderName());
        assertEquals(BigDecimal.ZERO, createdAccount.getBalance());
        assertEquals(ACCOUNT_ID, createdAccount.getId());

        // Verify repository interactions
        verify(bankAccountRepository).save(any(BankAccount.class));
    }
}