package com.bankafrica.bankingapp;

import com.bankafrica.bankingapp.config.TestConfig;
import com.bankafrica.bankingapp.util.TestDataFactory;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * Base class for integration tests. Every integration test shares this exact
 * configuration so Spring caches and reuses a single application context. That keeps
 * the in-memory H2 schema (created once via ddl-auto=create) stable for the whole run,
 * instead of multiple contexts dropping it out from under each other.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(locations = "classpath:application-test.properties")
@Import(TestConfig.class)
public abstract class BaseTest {

    @BeforeEach
    public void setup() {
    }
    
    protected TestDataFactory getTestDataFactory() {
        return new TestDataFactory();
    }
}