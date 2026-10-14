package com.arcadiadevs.viora.platform.iam.infrastructure.email.brevo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("BrevoTransactionalEmailAdapter Infrastructure Adapter Unit Tests")
class BrevoTransactionalEmailAdapterTest {

    @Test
    @DisplayName("Should return silently when API key is blank in local/test environment")
    void shouldReturnSilentlyWhenApiKeyIsBlank() {
        var adapter = new BrevoTransactionalEmailAdapter("", "sender@viora.ag", "Viora", "https://viora.ag/reset?token=", "Subject");

        assertThatCode(() -> adapter.sendPasswordResetEmail("user@viora.ag", "sample-token"))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Should initialize with default fallback values when arguments are null")
    void shouldInitializeWithDefaultFallbackValues() {
        var adapter = new BrevoTransactionalEmailAdapter(null, null, null, null, null);

        assertThatCode(() -> adapter.sendPasswordResetEmail("user@viora.ag", "sample-token"))
                .doesNotThrowAnyException();
    }
}
