package com.bankafrica.bankingapp.controller;

import com.bankafrica.bankingapp.BaseTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies that failures come back as the uniform {@link com.bankafrica.bankingapp.exception.ApiError}
 * JSON envelope rather than stack traces or HTML.
 */
@Transactional
class ControllerErrorHandlingTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Malformed JSON body returns a 400 ApiError")
    void testMalformedBody() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.message", is("Malformed or missing request body")))
                .andExpect(jsonPath("$.path", is("/api/auth/login")))
                .andExpect(jsonPath("$.timestamp", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("Validation failure returns 400 with per-field errors")
    void testValidationEnvelope() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"password\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.fieldErrors.email", not(emptyOrNullString())))
                .andExpect(jsonPath("$.fieldErrors.password", not(emptyOrNullString())));
    }

    @Test
    @DisplayName("Protected endpoints return a 401 ApiError when unauthenticated")
    void testUnauthenticatedEnvelope() throws Exception {
        mockMvc.perform(post("/api/account/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\":10.00}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.message", is("Authentication required")));
    }
}
