package com.arcadiadevs.viora.platform.iam.infrastructure.tokens.opaque;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SecureRandomTokenGenerator Infrastructure Adapter Unit Tests")
class SecureRandomTokenGeneratorTest {

    private final SecureRandomTokenGenerator generator = new SecureRandomTokenGenerator();

    @Test
    @DisplayName("Generates distinct 256-bit URL-safe tokens and hashes them deterministically")
    void generatesAndHashes() {
        var first = generator.generate();
        var second = generator.generate();

        assertThat(first).isNotEqualTo(second).hasSize(43).matches("[A-Za-z0-9_-]+");
        assertThat(generator.hash(first).value()).hasSize(64);
        assertThat(generator.hash(first)).isEqualTo(generator.hash(first)).isNotEqualTo(generator.hash(second));
    }
}
