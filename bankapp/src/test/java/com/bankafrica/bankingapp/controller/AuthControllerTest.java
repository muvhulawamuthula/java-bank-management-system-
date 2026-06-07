package com.bankafrica.bankingapp.controller;

import com.bankafrica.bankingapp.BaseTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end tests of the auth endpoints through the real Spring Security filter chain.
 * Each test runs in a transaction that rolls back, so registrations don't leak.
 */
@Transactional
class AuthControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    private Map<String, Object> validRegistration(String email, String idNumber) {
        return Map.of(
                "firstName", "John",
                "lastName", "Doe",
                "email", email,
                "idNumber", idNumber,
                "phoneNumber", "0712345678",
                "password", "securepassword",
                "initialDeposit", 500.00);
    }

    @Test
    @DisplayName("Register returns 201 with a token and a funded account")
    void testRegisterSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistration("john@example.com", "9001015000000"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())))
                .andExpect(jsonPath("$.email", is("john@example.com")))
                .andExpect(jsonPath("$.accountNumber", matchesPattern("\\d{10}")))
                .andExpect(jsonPath("$.balance", is(500.00)));
    }

    @Test
    @DisplayName("Register with an invalid body returns 400 with field errors")
    void testRegisterValidationFailure() throws Exception {
        Map<String, Object> bad = Map.of(
                "firstName", "",
                "lastName", "Doe",
                "email", "not-an-email",
                "idNumber", "123",
                "phoneNumber", "0712345678",
                "password", "short",
                "initialDeposit", 10.00);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Validation failed")))
                .andExpect(jsonPath("$.fieldErrors.email", not(emptyOrNullString())))
                .andExpect(jsonPath("$.fieldErrors.password", not(emptyOrNullString())))
                .andExpect(jsonPath("$.fieldErrors.initialDeposit", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("Registering a duplicate email returns 409")
    void testRegisterDuplicateEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistration("dup@example.com", "9001015000000"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistration("dup@example.com", "9001015000001"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Email already registered")));
    }

    @Test
    @DisplayName("Login returns a token; wrong password returns 401")
    void testLogin() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistration("login@example.com", "9001015000002"))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@example.com\",\"password\":\"securepassword\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(emptyOrNullString())));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@example.com\",\"password\":\"wrongpassword\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message", is("Invalid email or password")));
    }

    @Test
    @DisplayName("/me requires a token and returns the caller's profile")
    void testMeEndpoint() throws Exception {
        String token = registerAndGetToken("me@example.com", "9001015000003");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("me@example.com")))
                .andExpect(jsonPath("$.accountNumber", matchesPattern("\\d{10}")));

        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndGetToken(String email, String idNumber) throws Exception {
        String body = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRegistration(email, idNumber))))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(body);
        return node.get("token").asText();
    }
}
