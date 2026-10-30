package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.Duration;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

/** The SDK applies the timeout to each attempt, so every attempt together must fit the 20 s of the contract. */
class AssistantModelCallBudgetTest {

    private static final Duration CONTRACT_TOTAL = Duration.ofSeconds(20);

    @Test
    @DisplayName("Timeout per attempt times attempts stays within the 20 s budget, leaving room for the backoff")
    void attemptsFitTheBudget() throws Exception {
        var properties = new Properties();
        try (InputStream in = getClass().getResourceAsStream("/application.properties")) {
            properties.load(in);
        }
        var perAttempt = Duration.parse("PT" + properties.getProperty("spring.ai.openai.timeout").toUpperCase());
        int attempts = Integer.parseInt(properties.getProperty("spring.ai.openai.max-retries")) + 1;

        assertThat(perAttempt.multipliedBy(attempts)).isLessThan(CONTRACT_TOTAL);
    }
}
