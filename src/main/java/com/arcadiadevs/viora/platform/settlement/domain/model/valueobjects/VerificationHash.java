package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * SHA-256 digest of the stored bytes of a certified dossier, as exactly 64 lowercase hexadecimal characters.
 *
 * @param value lowercase hexadecimal digest
 */
public record VerificationHash(String value) {

    public static final int LENGTH = 64;
    private static final Pattern LOWERCASE_HEX = Pattern.compile("[0-9a-f]{" + LENGTH + "}");

    public VerificationHash {
        if (value == null || !LOWERCASE_HEX.matcher(value).matches()) {
            throw new IllegalArgumentException("settlement.certification.hash.invalid");
        }
    }
}
