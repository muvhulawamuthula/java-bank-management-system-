package com.bankafrica.bankingapp.controller;

import com.bankafrica.bankingapp.service.AuthService;
import com.bankafrica.bankingapp.service.BankingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for error handling in the controllers.
 * These tests verify that the controllers properly handle and respond to various error conditions.
 */
@WebMvcTest({AuthController.class, BankingController.class})
@ActiveProfiles("test")
public class ControllerErrorHandlingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private BankingService bankingService;

    @Test
    @DisplayName("Test registration with missing required fields")
    void testRegisterWithMissingFields() throws Exception {
        // Create request body with missing fields
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("firstName", "John");
        // Missing lastName, email, etc.

        // Perform request and verify response
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Required fields missing")));
    }

    @Test
    @DisplayName("Test registration with server error")
    void testRegisterWithServerError() throws Exception {
        // Mock service to throw unexpected exception
        when(authService.registerUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenThrow(new RuntimeException("Unexpected server error"));

        // Create complete request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("firstName", "John");
        requestBody.put("lastName", "Doe");
        requestBody.put("email", "john.doe@example.com");
        requestBody.put("idNumber", "9001015000000");
        requestBody.put("phoneNumber", "0712345678");
        requestBody.put("password", "password123");
        requestBody.put("initialDeposit", 500.00);

        // Perform request and verify response
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().string(containsString("Unexpected server error")));
    }

    @Test
    @DisplayName("Test login with missing fields")
    void testLoginWithMissingFields() throws Exception {
        // Create request body with missing fields
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", "john.doe@example.com");
        // Missing password

        // Perform request and verify response
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Email and password are required")));
    }

    @Test
    @DisplayName("Test deposit with invalid account ID format")
    void testDepositWithInvalidAccountIdFormat() throws Exception {
        // Create request body with invalid account ID
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", "invalid-id"); // Should be a number
        requestBody.put("amount", 500.00);

        // Perform request and verify response
        mockMvc.perform(post("/api/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid account ID format")));
    }

    @Test
    @DisplayName("Test deposit with invalid amount format")
    void testDepositWithInvalidAmountFormat() throws Exception {
        // Create request body with invalid amount
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", 1);
        requestBody.put("amount", "invalid-amount"); // Should be a number

        // Perform request and verify response
        mockMvc.perform(post("/api/deposit")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid amount format")));
    }

    @Test
    @DisplayName("Test withdrawal with missing fields")
    void testWithdrawWithMissingFields() throws Exception {
        // Create request body with missing fields
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", 1);
        // Missing amount

        // Perform request and verify response
        mockMvc.perform(post("/api/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Account ID and amount are required")));
    }

    @Test
    @DisplayName("Test withdrawal with server error")
    void testWithdrawWithServerError() throws Exception {
        // Mock service to throw unexpected exception
        when(bankingService.withdraw(anyLong(), any(BigDecimal.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        // Create complete request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("accountId", 1);
        requestBody.put("amount", 300.00);

        // Perform request and verify response
        mockMvc.perform(post("/api/withdraw")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value(containsString("Database connection error")));
    }

    @Test
    @DisplayName("Test get account with invalid ID path variable")
    void testGetAccountWithInvalidIdPathVariable() throws Exception {
        // Perform request with invalid ID format
        mockMvc.perform(get("/api/accounts/invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid account ID format")));
    }

    @Test
    @DisplayName("Test get profile with invalid ID path variable")
    void testGetProfileWithInvalidIdPathVariable() throws Exception {
        // Perform request with invalid ID format
        mockMvc.perform(get("/api/auth/profile/invalid-id"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid user ID format")));
    }

    @Test
    @DisplayName("Test create account with missing fields")
    void testCreateAccountWithMissingFields() throws Exception {
        // Create request body with missing fields
        Map<String, Object> requestBody = new HashMap<>();
        // Missing name and initialBalance

        // Perform request and verify response
        mockMvc.perform(post("/api/accounts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Account holder name is required")));
    }

    @Test
    @DisplayName("Test handling of unsupported HTTP method")
    void testUnsupportedHttpMethod() throws Exception {
        // Perform POST request on endpoint that doesn't support it
        mockMvc.perform(post("/api/accounts/1"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("Test handling of invalid JSON")
    void testInvalidJson() throws Exception {
        // Perform request with invalid JSON
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"invalid\": \"json\","))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Invalid JSON format")));
    }

    @Test
    @DisplayName("Test handling of unsupported media type")
    void testUnsupportedMediaType() throws Exception {
        // Perform request with unsupported media type
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.TEXT_PLAIN)
                .content("This is plain text"))
                .andExpect(status().isUnsupportedMediaType());
    }
}