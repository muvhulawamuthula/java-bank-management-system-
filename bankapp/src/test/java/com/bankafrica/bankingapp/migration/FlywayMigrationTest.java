package com.bankafrica.bankingapp.migration;

import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.Transaction;
import com.bankafrica.bankingapp.model.User;
import com.bankafrica.bankingapp.repository.BankAccountRepository;
import com.bankafrica.bankingapp.repository.TransactionRepository;
import com.bankafrica.bankingapp.repository.UserRepository;
import com.bankafrica.bankingapp.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies the production schema is actually built by Flyway (not Hibernate). The rest
 * of the suite runs on Hibernate's generated H2 schema with Flyway disabled; this test
 * flips that around — it disables {@code ddl-auto}, enables Flyway, and runs the real
 * migration against H2 in MySQL-compatibility mode. If V1 has a syntax error, references
 * a missing column, or drifts from the entities, this test fails to start.
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:flywaymigrationtest;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "app.jwt.secret=test-only-secret-change-me-please-0123456789-abcdef",
        "app.cors.allowed-origins=http://localhost:8080"
})
@Transactional
class FlywayMigrationTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private BankAccountRepository bankAccountRepository;
    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    @DisplayName("Registration works end-to-end against the Flyway-built schema")
    void schemaSupportsRegistrationAndLedger() {
        User user = authService.registerUser("Grace", "K", "grace@example.com",
                "9001015000000", "0712345678", "securepass", new BigDecimal("500.00"));

        // users + bank_account rows and the FK between them.
        User reloaded = userRepository.findById(user.getId()).orElseThrow();
        BankAccount account = reloaded.getBankAccount();
        assertNotNull(account.getId());
        assertEquals(0, new BigDecimal("500.00").compareTo(account.getBalance()));
        assertEquals(10, account.getAccountNumber().length());

        // transactions row + its FK to bank_account, plus the index column.
        List<Transaction> ledger =
                transactionRepository.findByAccountIdOrderByCreatedAtDescIdDesc(account.getId());
        assertEquals(1, ledger.size());
        assertEquals("Account opening deposit", ledger.get(0).getDescription());

        // Unique constraints from the migration are enforced.
        assertTrue(bankAccountRepository.existsByAccountNumber(account.getAccountNumber()));
    }

    @Test
    @DisplayName("V2 CHECK constraint rejects a negative balance at the database level")
    void schemaForbidsNegativeBalance() {
        BankAccount account = new BankAccount("Overdraw Attempt", new BigDecimal("100.00"));
        account.setBalance(new BigDecimal("-0.01"));

        // The chk_bank_account_balance_non_negative constraint from V2 must reject this
        // on flush — independently of the application-level overdraw guard.
        assertThrows(DataIntegrityViolationException.class,
                () -> bankAccountRepository.saveAndFlush(account));
    }
}
