package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.regex.Pattern;

/**
 * Value object holding the SHA-256 fingerprint of a cooperative invitation code, as 64 lowercase hex characters.
 *
 * @param value the hex fingerprint
 */
public record CodeFingerprint(String value) {

    private static final Pattern HEX_SHA_256 = Pattern.compile("^[0-9a-f]{64}$");

    /**
     * Validates the fingerprint.
     *
     * @throws IllegalArgumentException if it is null or not 64 lowercase hex characters
     */
    public CodeFingerprint {
        if (value == null || !HEX_SHA_256.matcher(value).matches()) {
            throw new IllegalArgumentException("subscription.invitation_code.fingerprint.invalid");
        }
    }

    /**
     * Computes the fingerprint of a code.
     *
     * @param code the normalized code
     * @return the SHA-256 fingerprint of the code
     * @throws IllegalArgumentException if the code is null
     * @throws IllegalStateException    if the platform has no SHA-256 implementation
     */
    public static CodeFingerprint of(CooperativeCode code) {
        if (code == null) {
            throw new IllegalArgumentException("subscription.invitation_code.blank");
        }
        try {
            var digest = MessageDigest.getInstance("SHA-256").digest(code.value().getBytes(StandardCharsets.UTF_8));
            return new CodeFingerprint(HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("subscription.invitation_code.fingerprint.unavailable", exception);
        }
    }
}
