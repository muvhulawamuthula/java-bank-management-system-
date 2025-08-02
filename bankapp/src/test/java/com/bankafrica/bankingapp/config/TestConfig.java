package com.bankafrica.bankingapp.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
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
    
    /**
     * This method can be used to provide any test-specific beans.
     * For example, you could provide mock implementations of services or repositories.
     */
    // Example of a test-specific bean:
    // @Bean
    // public SomeService mockSomeService() {
    //     return new MockSomeService();
    // }
}