package com.arcadiadevs.viora.platform.subscription.infrastructure.codes;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Invitation code cipher and generator unit tests")
class InvitationCodeCipherAndGeneratorTest {

    private static final String KEY = Base64.getEncoder().encodeToString(new byte[32]);
    private static final String OTHER_KEY = Base64.getEncoder().encodeToString("x".repeat(32).getBytes());

    @Test
    @DisplayName("A sealed code reads back only with its key, and the same code never seals the same twice")
    void sealsAndOpens() {
        var cipher = new AesGcmInvitationCodeCipher(KEY);

        var first = cipher.encrypt("COOP-4F9T");
        var second = cipher.encrypt("COOP-4F9T");

        assertThat(cipher.isAvailable()).isTrue();
        assertThat(first).doesNotContain("4F9T").isNotEqualTo(second);
        assertThat(cipher.decrypt(first)).isEqualTo("COOP-4F9T");
        assertThatThrownBy(() -> new AesGcmInvitationCodeCipher(OTHER_KEY).decrypt(first))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Without a key of 32 bytes in Base64 the cipher is unavailable instead of failing to start")
    void unavailableWithoutAValidKey() {
        for (var key : new String[]{"", "  ", "not base64!", Base64.getEncoder().encodeToString(new byte[16])}) {
            var cipher = new AesGcmInvitationCodeCipher(key);
            assertThat(cipher.isAvailable()).as(key).isFalse();
            assertThatThrownBy(() -> cipher.encrypt("COOP-4F9T"))
                    .hasMessage("subscription.invitation_code.cipher.unavailable");
        }
    }

    @Test
    @DisplayName("Drawn codes are valid cooperative codes without characters that can be mistaken")
    void drawsReadableCodes() {
        var generator = new SecureRandomInvitationCodeGenerator();
        var drawn = new HashSet<String>();

        for (var draw = 0; draw < 500; draw++) {
            var code = generator.next();
            assertThat(new CooperativeCode(code).value()).isEqualTo(code);
            assertThat(code.substring(5)).doesNotContain("0", "O", "1", "I", "L");
            drawn.add(code);
        }

        assertThat(drawn).hasSizeGreaterThan(400);
    }
}
