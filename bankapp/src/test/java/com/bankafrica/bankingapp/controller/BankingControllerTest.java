package com.bankafrica.bankingapp.controller;

import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.service.BankingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the BankingController.
 * Uses @WebMvcTest to test the controller's REST endpoints.
 */
@WebMvcTest(BankingController.class)
@ActiveProfiles("test")
class BankingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BankingService bankingService;

    private BankAccount testAccount;
    private final String ACCOUNT_HOLDER_NAME = "John Doe";
    private final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");
    private final Long ACCOUNT_ID = 1L;

    @BeforeEach
    void setUp() {
        // Create a test bank account
        testAccount = new BankAccount(ACCOUNT_HOLDER_NAME, INITIAL_BALANCE);
        testAccount.setId(ACCOUNT_ID);
        testAccount.setAccountNumber("1234567890");
    }

    @Test
    @DisplayName("Test get account by ID")
    void testGetAccount() throws Exception {
        // Mock service behavior
        when(bankingService.getAccount(ACCOUNT_ID)).thenReturn(testAccount);

        // Perform request and verify response
        mockMvc.perform(get("/api/accounts/{accountId}", ACCOUNT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.accountHolderName", is(ACCOUNT_HOLDER_NAME)))
                .andExpect(jsonPath("$.balance", is(1000.00)))
                .andExpect(jsonPath("$.accountNumber", is("1234567890")));
    }

    @Test
    @DisplayName("Test get non-existent account")
    void testGetNonExistentAccount() throws Exception {
        // Mock service behavior
        when(bankingService.getAccount(999L)).thenReturn(null);

        // Perform request and verify response
        mockMvc.perform(get("/api/accounts/{accountId}", 999))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Test get account with error")
    void testGetAccountWithError() throws Exception {
        // Mock service behavior
        when(bankingService.getAccount(ACCOUNT_ID)).thenThrow(new RuntimeException("Database error"));

        // Perform request and verify response
        mockMvc.perform(get("/api/accounts/{accountId}", ACCOUNT_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", containsString("Error retrieving account: Database error")));
    }

    @Test
    @DisplayName("Test successful deposit")
    void testDepositSuccess() throws Exception {
        // Mock service behavior
        BigDecimal depositAmount = new BigDecimal("500.00");
        BigDecimal newBalance = INITIAL_BALANCE.add(depositAmount);
        
        BankAccount updatedAccount = new BankAccount(ACCOUNT_HOLDER_NAME, newBalance);
        updatedAccount.setId(ACCOUNT_ID);
        
        when(bankingService.deposit(ACCOUNT_ID, depositAmount)).thenReturn(updatedAccount);

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", ACCOUNT_ID);
        requestBody.put("amount", depositAmount);

        // Perform request and verify response
        mockMvc.perform(post("/api/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Deposit successful")))
                .andExpect(jsonPath("$.newBalance", is(1500.00)))
                .andExpect(jsonPath("$.accountId", is(1)));
    }

    @Test
    @DisplayName("Test deposit with negative amount")
    void testDepositWithNegativeAmount() throws Exception {
        // Create request body with negative amount
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", ACCOUNT_ID);
        requestBody.put("amount", -100.00);

        // Perform request and verify response
        mockMvc.perform(post("/api/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Amount must be greater than 0")));
    }

    @Test
    @DisplayName("Test deposit with error")
    void testDepositWithError() throws Exception {
        // Mock service behavior
        when(bankingService.deposit(anyLong(), any(BigDecimal.class)))
                .thenThrow(new RuntimeException("Account not found"));

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", ACCOUNT_ID);
        requestBody.put("amount", 500.00);

        // Perform request and verify response
        mockMvc.perform(post("/api/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Deposit failed: Account not found")));
    }

    @Test
    @DisplayName("Test successful withdrawal")
    void testWithdrawSuccess() throws Exception {
        // Mock service behavior
        BigDecimal withdrawAmount = new BigDecimal("300.00");
        BigDecimal newBalance = INITIAL_BALANCE.subtract(withdrawAmount);
        
        BankAccount updatedAccount = new BankAccount(ACCOUNT_HOLDER_NAME, newBalance);
        updatedAccount.setId(ACCOUNT_ID);
        
        when(bankingService.withdraw(ACCOUNT_ID, withdrawAmount)).thenReturn(updatedAccount);

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", ACCOUNT_ID);
        requestBody.put("amount", withdrawAmount);

        // Perform request and verify response
        mockMvc.perform(post("/api/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Withdrawal successful")))
                .andExpect(jsonPath("$.newBalance", is(700.00)))
                .andExpect(jsonPath("$.accountId", is(1)));
    }

    @Test
    @DisplayName("Test withdrawal with negative amount")
    void testWithdrawWithNegativeAmount() throws Exception {
        // Create request body with negative amount
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", ACCOUNT_ID);
        requestBody.put("amount", -100.00);

        // Perform request and verify response
        mockMvc.perform(post("/api/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Amount must be greater than 0")));
    }

    @Test
    @DisplayName("Test withdrawal with error")
    void testWithdrawWithError() throws Exception {
        // Mock service behavior
        when(bankingService.withdraw(anyLong(), any(BigDecimal.class)))
                .thenThrow(new RuntimeException("Insufficient funds"));

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", ACCOUNT_ID);
        requestBody.put("amount", 300.00);

        // Perform request and verify response
        mockMvc.perform(post("/api/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Withdrawal failed: Insufficient funds")));
    }

    @Test
    @DisplayName("Test create account")
    void testCreateAccount() throws Exception {
        // Mock service behavior
        when(bankingService.createAccount(ACCOUNT_HOLDER_NAME, INITIAL_BALANCE)).thenReturn(testAccount);

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", ACCOUNT_HOLDER_NAME);
        requestBody.put("initialBalance", INITIAL_BALANCE);

        // Perform request and verify response
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.accountHolderName", is(ACCOUNT_HOLDER_NAME)))
                .andExpect(jsonPath("$.balance", is(1000.00)))
                .andExpect(jsonPath("$.accountNumber", is("1234567890")));
    }

    @Test
    @DisplayName("Test create account with error")
    void testCreateAccountWithError() throws Exception {
        // Mock service behavior
        when(bankingService.createAccount(any(), any()))
                .thenThrow(new RuntimeException("Invalid account data"));

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("name", ACCOUNT_HOLDER_NAME);
        requestBody.put("initialBalance", INITIAL_BALANCE);

        // Perform request and verify response
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Account creation failed: Invalid account data")));
    }
}