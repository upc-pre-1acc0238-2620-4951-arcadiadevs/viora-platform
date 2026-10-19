package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value object representing the SHA-256 hash of an opaque token, such as a refresh token or the token of a password
 * reset link, as 64 lowercase hexadecimal characters.
 *
 * <p>The domain stores and looks up only this hash, never the plain token. Having its own type keeps a plain token
 * from being passed where a hash is expected.</p>
 *
 * @param value the hexadecimal hash
 */
public record TokenHash(String value) {

    private static final Pattern FORMAT = Pattern.compile("[0-9a-f]{64}");

    /**
     * Compact constructor validating the hash format.
     *
     * @throws IllegalArgumentException if the value is missing or is not 64 hexadecimal characters
     */
    public TokenHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("user.token_hash.null_or_empty");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("user.token_hash.invalid");
        }
    }

    /**
     * Compares with another hash in constant time, so the comparison does not leak how many characters match.
     *
     * @param other the hash to compare with
     * @return true if both hashes are equal; false otherwise, including when {@code other} is null
     */
    public boolean matches(TokenHash other) {
        return other != null && MessageDigest.isEqual(
                value.getBytes(StandardCharsets.US_ASCII), other.value.getBytes(StandardCharsets.US_ASCII));
    }
}
