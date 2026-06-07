package com.bankafrica.bankingapp.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the credential endpoints are throttled. Runs in an isolated context with a tiny
 * bucket (capacity 3) so the limit is reached deterministically; the rest of the suite runs
 * with the limiter effectively disabled.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties",
        properties = {
                "app.ratelimit.auth.capacity=3",
                "app.ratelimit.auth.refill-seconds=60"
        })
class RateLimitingFilterTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private RateLimitingFilter rateLimitingFilter;

    @Test
    @DisplayName("Login is throttled with 429 once the per-IP bucket is exhausted")
    void loginIsRateLimited() throws Exception {
        rateLimitingFilter.reset(); // start from a full bucket regardless of context reuse
        String body = "{\"email\":\"nobody@example.com\",\"password\":\"whatever123\"}";

        // First 3 attempts pass the filter and reach the controller (401 invalid credentials).
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        // The 4th is rejected by the limiter before authentication, in the ApiError envelope.
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Too many")));
    }
}
