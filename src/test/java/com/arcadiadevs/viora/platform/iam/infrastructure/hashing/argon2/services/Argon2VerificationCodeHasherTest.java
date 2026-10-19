package com.arcadiadevs.viora.platform.iam.infrastructure.hashing.argon2.services;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.VerificationCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Argon2VerificationCodeHasher Infrastructure Adapter Unit Tests")
class Argon2VerificationCodeHasherTest {

    private final Argon2VerificationCodeHasher hasher = new Argon2VerificationCodeHasher(new HashingServiceImpl());

    @Test
    @DisplayName("Hashes with a salt and verifies only the original code")
    void hashesWithSaltAndMatchesOriginalCode() {
        var code = new VerificationCode("482913");

        var first = hasher.hash(code);
        var second = hasher.hash(code);

        assertThat(first).startsWith("$argon2id$").doesNotContain("482913");
        assertThat(first).isNotEqualTo(second);
        assertThat(hasher.matches(code, first)).isTrue();
        assertThat(hasher.matches(new VerificationCode("000000"), first)).isFalse();
    }
}
