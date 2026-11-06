package com.arcadiadevs.viora.platform.subscription.infrastructure.codes.aesgcm;

import com.arcadiadevs.viora.platform.subscription.application.ports.InvitationCodeCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * {@link InvitationCodeCipher} using AES-256-GCM with the key in {@code viora.subscription.code-encryption-key}, 32
 * bytes in Base64. Each code gets a random nonce, stored with it. Without a valid key the cipher is unavailable and
 * codes cannot be issued, but everything else keeps working.
 */
@Component
public class AesGcmInvitationCodeCipher implements InvitationCodeCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_BYTES = 32;
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final Logger log = LoggerFactory.getLogger(AesGcmInvitationCodeCipher.class);

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    /**
     * Creates the cipher.
     *
     * @param encodedKey the key in Base64; blank or invalid leaves the cipher unavailable
     */
    public AesGcmInvitationCodeCipher(@Value("${viora.subscription.code-encryption-key:}") String encodedKey) {
        this.key = keyOf(encodedKey);
    }

    @Override
    public boolean isAvailable() {
        return key != null;
    }

    @Override
    public String encrypt(String code) {
        requireKey();
        try {
            var nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            var sealed = cipher.doFinal(code.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(nonce.length + sealed.length).put(nonce).put(sealed).array());
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("subscription.invitation_code.cipher.failed", ex);
        }
    }

    @Override
    public String decrypt(String sealedCode) {
        requireKey();
        try {
            var bytes = Base64.getDecoder().decode(sealedCode);
            var cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, bytes, 0, NONCE_BYTES));
            var code = cipher.doFinal(bytes, NONCE_BYTES, bytes.length - NONCE_BYTES);
            return new String(code, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("subscription.invitation_code.cipher.failed", ex);
        }
    }

    private void requireKey() {
        if (key == null) {
            throw new IllegalStateException("subscription.invitation_code.cipher.unavailable");
        }
    }

    private static SecretKeySpec keyOf(String encodedKey) {
        if (encodedKey == null || encodedKey.isBlank()) {
            log.warn("No code encryption key is configured: invitation codes cannot be issued.");
            return null;
        }
        try {
            var bytes = Base64.getDecoder().decode(encodedKey.trim());
            if (bytes.length == KEY_BYTES) {
                return new SecretKeySpec(bytes, "AES");
            }
        } catch (IllegalArgumentException ignored) {
            // Reported below, without the value.
        }
        log.warn("The code encryption key is not 32 bytes in Base64: invitation codes cannot be issued.");
        return null;
    }
}
