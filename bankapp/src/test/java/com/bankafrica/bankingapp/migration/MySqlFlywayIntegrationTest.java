package com.bankafrica.bankingapp.migration;

import com.bankafrica.bankingapp.dto.AccountResponse;
import com.bankafrica.bankingapp.dto.AmountRequest;
import com.bankafrica.bankingapp.model.BankAccount;
import com.bankafrica.bankingapp.model.User;
import com.bankafrica.bankingapp.repository.BankAccountRepository;
import com.bankafrica.bankingapp.service.AuthService;
import com.bankafrica.bankingapp.service.BankingService;
import com.bankafrica.bankingapp.service.IdempotencyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The real-database integration test: spins up an actual MySQL 8 in Docker via Testcontainers,
 * lets Flyway build the schema from V1→V3 against it (Hibernate validation off), and exercises
 * the app end-to-end. Unlike {@code FlywayMigrationTest} (H2 in MySQL-compatibility mode), this
 * runs against the genuine engine the app uses in production — so MySQL-specific behaviour
 * (the {@code CHECK} constraint, {@code AUTO_INCREMENT}, the idempotency unique key) is verified
 * for real. Requires a Docker daemon; it is skipped automatically where none is available.
 */
@SpringBootTest
@Testcontainers
@EnabledIf("dockerAvailable")
@TestPropertySource(properties = {
        "spring.flyway.enabled=true",
        "spring.flyway.baseline-on-migrate=true",
        "spring.jpa.hibernate.ddl-auto=none",
        "app.jwt.secret=test-only-secret-change-me-please-0123456789-abcdef",
        "app.cors.allowed-origins=http://localhost:8080"
})
class MySqlFlywayIntegrationTest {

    @Container
    @ServiceConnection
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0");

    /**
     * Gate the whole class on a reachable, compatible Docker daemon. When Testcontainers can't
     * obtain one (no Docker, or a daemon/API-version mismatch with docker-java) the class is
     * skipped rather than failing — the same migrations are also covered against H2 in
     * MySQL-compatibility mode by {@link FlywayMigrationTest}.
     */
    static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (Throwable t) {
            return false;
        }
    }

    @Autowired
    private AuthService authService;
    @Autowired
    private BankingService bankingService;
    @Autowired
    private IdempotencyService idempotencyService;
    @Autowired
    private BankAccountRepository bankAccountRepository;

    @Test
    @DisplayName("Flyway builds V1–V3 on real MySQL and the app works end-to-end")
    void registrationAndIdempotencyAgainstRealMySql() {
        User user = authService.registerUser("Naledi", "M", "naledi@example.com",
                "9001015000099", "0712345678", "securepass", new BigDecimal("500.00"));
        BankAccount account = user.getBankAccount();
        assertNotNull(account.getId());
        assertEquals(10, account.getAccountNumber().length());

        // V3 idempotency table on real MySQL: same key deposits exactly once.
        Long accountId = account.getId();
        String key = "mysql-idem-key";
        AmountRequest request = new AmountRequest(new BigDecimal("100.00"));

        AccountResponse first = idempotencyService.execute(key, accountId, "deposit", request,
                () -> AccountResponse.from(bankingService.deposit(accountId, request.amount())),
                AccountResponse.class);
        AccountResponse replay = idempotencyService.execute(key, accountId, "deposit", request,
                () -> AccountResponse.from(bankingService.deposit(accountId, request.amount())),
                AccountResponse.class);

        assertEquals(0, new BigDecimal("600.00").compareTo(first.balance()));
        assertEquals(0, new BigDecimal("600.00").compareTo(replay.balance()));
        // Ground truth from the database: balance moved once, not twice.
        BigDecimal persisted = bankAccountRepository.findById(accountId).orElseThrow().getBalance();
        assertEquals(0, new BigDecimal("600.00").compareTo(persisted));
    }
}
