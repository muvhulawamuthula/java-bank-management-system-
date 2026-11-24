package com.bankafrica.bankingapp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Test configuration class that sets up the test environment.
 * This class is used to configure beans specifically for testing.
 */
@TestConfiguration
@EnableTransactionManagement
@Profile("test")
public class TestConfig {
}