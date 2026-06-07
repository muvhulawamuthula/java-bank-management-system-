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
 * End-to-end tests of the account endpoints. Operations act on the authenticated user's
 * own account (resolved from the JWT), so these also serve as regression tests for the
 * authentication and authorization rules.
 */
@Transactional
class BankingControllerTest extends BaseTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Authenticated user can view their account")
    void testGetOwnAccount() throws Exception {
        String token = register("a@example.com", "9001015000010").token;

        mockMvc.perform(get("/api/account").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(500.00)))
                .andExpect(jsonPath("$.accountNumber", matchesPattern("\\d{10}")));
    }

    @Test
    @DisplayName("Deposit increases the balance and appears in the ledger")
    void testDeposit() throws Exception {
        String token = register("b@example.com", "9001015000011").token;

        mockMvc.perform(post("/api/account/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 250.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(750.00)));

        mockMvc.perform(get("/api/account/transactions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // opening deposit + this deposit, in the paginated envelope
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.first", is(true)))
                .andExpect(jsonPath("$.last", is(true)));
    }

    @Test
    @DisplayName("Withdrawing more than the balance returns 422 and leaves the balance intact")
    void testWithdrawInsufficientFunds() throws Exception {
        String token = register("c@example.com", "9001015000012").token;

        mockMvc.perform(post("/api/account/withdraw")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 999999.00}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message", containsString("Insufficient funds")));

        mockMvc.perform(get("/api/account").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.balance", is(500.00)));
    }

    @Test
    @DisplayName("Transfer moves money to another account by number")
    void testTransfer() throws Exception {
        Registered sender = register("sender@example.com", "9001015000013");
        Registered receiver = register("receiver@example.com", "9001015000014");

        mockMvc.perform(post("/api/account/transfer")
                        .header("Authorization", "Bearer " + sender.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toAccountNumber\":\"" + receiver.accountNumber + "\",\"amount\":150.00,\"description\":\"Lunch\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(350.00)));

        mockMvc.perform(get("/api/account").header("Authorization", "Bearer " + receiver.token))
                .andExpect(jsonPath("$.balance", is(650.00)));
    }

    @Test
    @DisplayName("Transfer to a non-existent account returns 404")
    void testTransferToMissingAccount() throws Exception {
        String token = register("d@example.com", "9001015000015").token;

        mockMvc.perform(post("/api/account/transfer")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toAccountNumber\":\"0000000000\",\"amount\":10.00}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Account endpoints reject unauthenticated requests with 401")
    void testRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/account")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/account/deposit")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\":1.00}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("A negative deposit amount is rejected by validation")
    void testNegativeDepositRejected() throws Exception {
        String token = register("e@example.com", "9001015000016").token;

        mockMvc.perform(post("/api/account/deposit")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": -5.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount", not(emptyOrNullString())));
    }

    private record Registered(String token, String accountNumber) {}

    @Test
    @DisplayName("Same Idempotency-Key replays the first result instead of depositing twice")
    void testIdempotentDeposit() throws Exception {
        String token = register("idem@example.com", "9001015000020").token;
        String key = "deposit-key-123";

        // First deposit: 500 -> 600
        mockMvc.perform(post("/api/account/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(600.00)));

        // Retry with the same key + same body: replayed, balance NOT 700.
        mockMvc.perform(post("/api/account/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance", is(600.00)));

        // Ground truth: the account moved exactly once.
        mockMvc.perform(get("/api/account").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.balance", is(600.00)));
    }

    @Test
    @DisplayName("Reusing an Idempotency-Key with different parameters is a 409 conflict")
    void testIdempotencyKeyReuseConflict() throws Exception {
        String token = register("idem2@example.com", "9001015000021").token;
        String key = "reused-key-456";

        mockMvc.perform(post("/api/account/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 100.00}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/account/deposit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amount\": 999.00}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("different request parameters")));
    }

    @Test
    @DisplayName("A transfer's debit leg can be rendered as a SWIFT MT103 message")
    void testSwiftMt103ForTransfer() throws Exception {
        Registered sender = register("swiftsender@example.com", "9001015000022");
        Registered receiver = register("swiftreceiver@example.com", "9001015000023");

        mockMvc.perform(post("/api/account/transfer")
                        .header("Authorization", "Bearer " + sender.token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"toAccountNumber\":\"" + receiver.accountNumber
                                + "\",\"amount\":150.00,\"description\":\"Rent\"}"))
                .andExpect(status().isOk());

        // Newest ledger entry on the sender is the TRANSFER_OUT leg.
        String ledger = mockMvc.perform(get("/api/account/transactions")
                        .header("Authorization", "Bearer " + sender.token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        JsonNode top = objectMapper.readTree(ledger).get("content").get(0);
        long txId = top.get("id").asLong();

        mockMvc.perform(get("/api/account/transactions/" + txId + "/swift")
                        .header("Authorization", "Bearer " + sender.token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messageType", is("MT103")))
                .andExpect(jsonPath("$.message", containsString(":20:")))
                .andExpect(jsonPath("$.message", containsString(":32A:")))
                .andExpect(jsonPath("$.message", containsString("ZAR150,00")))
                .andExpect(jsonPath("$.message", containsString(":71A:SHA")))
                .andExpect(jsonPath("$.message", containsString(receiver.accountNumber)));
    }

    @Test
    @DisplayName("SWIFT generation is rejected for a non-transfer transaction")
    void testSwiftRejectedForDeposit() throws Exception {
        String token = register("swiftdep@example.com", "9001015000024").token;

        String ledger = mockMvc.perform(get("/api/account/transactions")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString();
        long openingTxId = objectMapper.readTree(ledger).get("content").get(0).get("id").asLong();

        mockMvc.perform(get("/api/account/transactions/" + openingTxId + "/swift")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("transfer")));
    }

    private Registered register(String email, String idNumber) throws Exception {
        Map<String, Object> body = Map.of(
                "firstName", "Test", "lastName", "User", "email", email,
                "idNumber", idNumber, "phoneNumber", "0712345678",
                "password", "securepassword", "initialDeposit", 500.00);
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = objectMapper.readTree(response);
        return new Registered(node.get("token").asText(), node.get("accountNumber").asText());
    }
}
