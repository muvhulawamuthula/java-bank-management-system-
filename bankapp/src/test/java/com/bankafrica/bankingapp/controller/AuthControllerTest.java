package com.bankafrica.bankingapp.controller;

import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.User;
import com.bankafrica.bankingapp.service.AuthService;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the AuthController.
 * Uses @WebMvcTest to test the controller's REST endpoints.
 */
@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    private User testUser;
    private BankAccount testAccount;
    private final String FIRST_NAME = "John";
    private final String LAST_NAME = "Doe";
    private final String EMAIL = "john.doe@example.com";
    private final String ID_NUMBER = "9001015000000";
    private final String PHONE_NUMBER = "0712345678";
    private final String PASSWORD = "securepassword";
    private final BigDecimal INITIAL_DEPOSIT = new BigDecimal("500.00");

    @BeforeEach
    void setUp() {
        // Create a test user with bank account
        testUser = new User(FIRST_NAME, LAST_NAME, EMAIL, ID_NUMBER, PHONE_NUMBER, PASSWORD);
        testUser.setId(1L);
        
        testAccount = new BankAccount(FIRST_NAME + " " + LAST_NAME, INITIAL_DEPOSIT);
        testAccount.setId(1L);
        testAccount.setAccountNumber("1234567890");
        
        testUser.setBankAccount(testAccount);
    }

    @Test
    @DisplayName("Test successful user registration")
    void testRegisterUserSuccess() throws Exception {
        // Mock service behavior
        when(authService.registerUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenReturn(testUser);

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("firstName", FIRST_NAME);
        requestBody.put("lastName", LAST_NAME);
        requestBody.put("email", EMAIL);
        requestBody.put("idNumber", ID_NUMBER);
        requestBody.put("phoneNumber", PHONE_NUMBER);
        requestBody.put("password", PASSWORD);
        requestBody.put("initialDeposit", INITIAL_DEPOSIT);

        // Perform request and verify response
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is("Registration successful")))
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.email", is(EMAIL)));
    }

    @Test
    @DisplayName("Test registration with existing email")
    void testRegisterUserWithExistingEmail() throws Exception {
        // Mock service behavior
        when(authService.registerUser(
                anyString(), anyString(), anyString(), anyString(), anyString(), anyString(), any(BigDecimal.class)))
                .thenThrow(new Exception("Email already registered"));

        // Create request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("firstName", FIRST_NAME);
        requestBody.put("lastName", LAST_NAME);
        requestBody.put("email", EMAIL);
        requestBody.put("idNumber", ID_NUMBER);
        requestBody.put("phoneNumber", PHONE_NUMBER);
        requestBody.put("password", PASSWORD);
        requestBody.put("initialDeposit", INITIAL_DEPOSIT);

        // Perform request and verify response
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Email already registered"));
    }

    @Test
    @DisplayName("Test successful user login")
    void testLoginUserSuccess() throws Exception {
        // Mock service behavior
        when(authService.loginUser(EMAIL, PASSWORD)).thenReturn(testUser);

        // Create request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", EMAIL);
        requestBody.put("password", PASSWORD);

        // Perform request and verify response
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.firstName", is(FIRST_NAME)))
                .andExpect(jsonPath("$.lastName", is(LAST_NAME)))
                .andExpect(jsonPath("$.email", is(EMAIL)))
                .andExpect(jsonPath("$.accountId", is(1)))
                .andExpect(jsonPath("$.accountNumber", is("1234567890")))
                .andExpect(jsonPath("$.balance", is(500.00)));
    }

    @Test
    @DisplayName("Test login with invalid credentials")
    void testLoginUserWithInvalidCredentials() throws Exception {
        // Mock service behavior
        when(authService.loginUser(anyString(), anyString()))
                .thenThrow(new Exception("Invalid email or password"));

        // Create request body
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("email", EMAIL);
        requestBody.put("password", "wrongpassword");

        // Perform request and verify response
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid email or password"));
    }

    @Test
    @DisplayName("Test get user profile")
    void testGetUserProfile() throws Exception {
        // Mock service behavior
        when(authService.getUserById(1L)).thenReturn(testUser);

        // Perform request and verify response
        mockMvc.perform(get("/api/auth/profile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(1)))
                .andExpect(jsonPath("$.firstName", is(FIRST_NAME)))
                .andExpect(jsonPath("$.lastName", is(LAST_NAME)))
                .andExpect(jsonPath("$.email", is(EMAIL)))
                .andExpect(jsonPath("$.phoneNumber", is(PHONE_NUMBER)))
                .andExpect(jsonPath("$.accountNumber", is("1234567890")))
                .andExpect(jsonPath("$.balance", is(500.00)));
    }

    @Test
    @DisplayName("Test get non-existent user profile")
    void testGetNonExistentUserProfile() throws Exception {
        // Mock service behavior
        when(authService.getUserById(999L)).thenReturn(null);

        // Perform request and verify response
        mockMvc.perform(get("/api/auth/profile/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Test get user profile with error")
    void testGetUserProfileWithError() throws Exception {
        // Mock service behavior
        when(authService.getUserById(1L)).thenThrow(new RuntimeException("Database error"));

        // Perform request and verify response
        mockMvc.perform(get("/api/auth/profile/1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("Error retrieving profile: Database error")));
    }
}