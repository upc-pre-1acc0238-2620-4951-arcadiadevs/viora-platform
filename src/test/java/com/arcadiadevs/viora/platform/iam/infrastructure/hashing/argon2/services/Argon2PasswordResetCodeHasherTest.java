package com.arcadiadevs.viora.platform.iam.infrastructure.hashing.argon2.services;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordResetCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Argon2PasswordResetCodeHasher Infrastructure Adapter Unit Tests")
class Argon2PasswordResetCodeHasherTest {

    private final Argon2PasswordResetCodeHasher hasher = new Argon2PasswordResetCodeHasher(new HashingServiceImpl());

    @Test
    @DisplayName("Hashes with a salt and verifies only the original code")
    void hashesWithSaltAndMatchesOriginalCode() {
        var code = new PasswordResetCode("482913");

        var first = hasher.hash(code);
        var second = hasher.hash(code);

        assertThat(first).startsWith("$argon2id$").doesNotContain("482913");
        assertThat(first).isNotEqualTo(second);
        assertThat(hasher.matches(code, first)).isTrue();
        assertThat(hasher.matches(new PasswordResetCode("000000"), first)).isFalse();
    }
}
