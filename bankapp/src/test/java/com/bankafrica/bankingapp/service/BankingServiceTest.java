package com.bankafrica.bankingapp.service;

import com.bankafrica.bankingapp.exception.AccountNotFoundException;
import com.bankafrica.bankingapp.exception.InsufficientFundsException;
import com.bankafrica.bankingapp.exception.InvalidRequestException;
import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.repository.BankAccountRepository;
import com.bankafrica.bankingapp.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link BankingService}. Verifies that balance mutations load the
 * account under a write lock ({@code findByIdForUpdate}) and that every successful
 * movement is written to the transaction ledger.
 */
class BankingServiceTest {

    @Mock
    private BankAccountRepository bankAccountRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private BankingService bankingService;

    private BankAccount testAccount;
    private final String ACCOUNT_HOLDER_NAME = "John Doe";
    private final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");
    private final Long ACCOUNT_ID = 1L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        bankingService = new BankingService(bankAccountRepository, transactionRepository);

        testAccount = new BankAccount(ACCOUNT_HOLDER_NAME, INITIAL_BALANCE);
        testAccount.setId(ACCOUNT_ID);
    }

    @Test
    @DisplayName("Test get account by ID")
    void testGetAccount() {
        when(bankAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
        when(bankAccountRepository.findById(2L)).thenReturn(Optional.empty());

        BankAccount foundAccount = bankingService.getAccount(ACCOUNT_ID);
        assertNotNull(foundAccount);
        assertEquals(testAccount, foundAccount);

        assertThrows(AccountNotFoundException.class, () -> bankingService.getAccount(2L));

        verify(bankAccountRepository).findById(ACCOUNT_ID);
        verify(bankAccountRepository).findById(2L);
    }

    @Test
    @DisplayName("Test successful deposit records a ledger entry")
    void testDepositSuccess() {
        when(bankAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal depositAmount = new BigDecimal("500.00");
        BankAccount updatedAccount = bankingService.deposit(ACCOUNT_ID, depositAmount);

        assertEquals(INITIAL_BALANCE.add(depositAmount), updatedAccount.getBalance());
        verify(bankAccountRepository).findByIdForUpdate(ACCOUNT_ID);
        verify(bankAccountRepository).save(any(BankAccount.class));
        verify(transactionRepository).save(any());
    }

    @Test
    @DisplayName("Test deposit with non-existent account")
    void testDepositWithNonExistentAccount() {
        when(bankAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.empty());

        Exception exception = assertThrows(AccountNotFoundException.class, () ->
                bankingService.deposit(ACCOUNT_ID, new BigDecimal("500.00")));

        assertEquals("Account not found with ID: " + ACCOUNT_ID, exception.getMessage());
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test deposit with negative amount is rejected before any DB access")
    void testDepositWithNegativeAmount() {
        Exception exception = assertThrows(InvalidRequestException.class, () ->
                bankingService.deposit(ACCOUNT_ID, new BigDecimal("-100.00")));

        assertEquals("Deposit amount must be positive", exception.getMessage());
        verify(bankAccountRepository, never()).findByIdForUpdate(any());
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test deposit with zero amount is rejected")
    void testDepositWithZeroAmount() {
        Exception exception = assertThrows(InvalidRequestException.class, () ->
                bankingService.deposit(ACCOUNT_ID, BigDecimal.ZERO));

        assertEquals("Deposit amount must be positive", exception.getMessage());
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test successful withdrawal records a ledger entry")
    void testWithdrawSuccess() {
        when(bankAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(i -> i.getArgument(0));

        BigDecimal withdrawAmount = new BigDecimal("300.00");
        BankAccount updatedAccount = bankingService.withdraw(ACCOUNT_ID, withdrawAmount);

        assertEquals(INITIAL_BALANCE.subtract(withdrawAmount), updatedAccount.getBalance());
        verify(bankAccountRepository).findByIdForUpdate(ACCOUNT_ID);
        verify(bankAccountRepository).save(any(BankAccount.class));
        verify(transactionRepository).save(any());
    }

    @Test
    @DisplayName("Test withdrawal with non-existent account")
    void testWithdrawWithNonExistentAccount() {
        when(bankAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.empty());

        Exception exception = assertThrows(AccountNotFoundException.class, () ->
                bankingService.withdraw(ACCOUNT_ID, new BigDecimal("300.00")));

        assertEquals("Account not found with ID: " + ACCOUNT_ID, exception.getMessage());
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test withdrawal with negative amount is rejected")
    void testWithdrawWithNegativeAmount() {
        Exception exception = assertThrows(InvalidRequestException.class, () ->
                bankingService.withdraw(ACCOUNT_ID, new BigDecimal("-100.00")));

        assertEquals("Withdrawal amount must be positive", exception.getMessage());
        verify(bankAccountRepository, never()).findByIdForUpdate(any());
    }

    @Test
    @DisplayName("Test withdrawal with zero amount is rejected")
    void testWithdrawWithZeroAmount() {
        Exception exception = assertThrows(InvalidRequestException.class, () ->
                bankingService.withdraw(ACCOUNT_ID, BigDecimal.ZERO));

        assertEquals("Withdrawal amount must be positive", exception.getMessage());
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test withdrawal with insufficient funds")
    void testWithdrawWithInsufficientFunds() {
        when(bankAccountRepository.findByIdForUpdate(ACCOUNT_ID)).thenReturn(Optional.of(testAccount));

        BigDecimal excessiveAmount = INITIAL_BALANCE.add(new BigDecimal("100.00"));
        Exception exception = assertThrows(InsufficientFundsException.class, () ->
                bankingService.withdraw(ACCOUNT_ID, excessiveAmount));

        assertTrue(exception.getMessage().startsWith("Insufficient funds. Current balance: R"));
        verify(bankAccountRepository, never()).save(any(BankAccount.class));
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Test create account")
    void testCreateAccount() {
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount saved = invocation.getArgument(0);
            saved.setId(ACCOUNT_ID);
            return saved;
        });

        BankAccount createdAccount = bankingService.createAccount(ACCOUNT_HOLDER_NAME, INITIAL_BALANCE);

        assertNotNull(createdAccount);
        assertEquals(ACCOUNT_HOLDER_NAME, createdAccount.getAccountHolderName());
        assertEquals(INITIAL_BALANCE, createdAccount.getBalance());
        assertEquals(ACCOUNT_ID, createdAccount.getId());
        verify(bankAccountRepository).save(any(BankAccount.class));
    }

    @Test
    @DisplayName("Test create account with null balance")
    void testCreateAccountWithNullBalance() {
        when(bankAccountRepository.save(any(BankAccount.class))).thenAnswer(invocation -> {
            BankAccount saved = invocation.getArgument(0);
            saved.setId(ACCOUNT_ID);
            return saved;
        });

        BankAccount createdAccount = bankingService.createAccount(ACCOUNT_HOLDER_NAME, null);

        assertNotNull(createdAccount);
        assertEquals(BigDecimal.ZERO, createdAccount.getBalance());
        verify(bankAccountRepository).save(any(BankAccount.class));
    }
}
