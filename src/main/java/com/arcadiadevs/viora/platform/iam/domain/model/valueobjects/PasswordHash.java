package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

/**
 * Value object encapsulating a cryptographically derived password hash.
 *
 * @param value the encoded hash string
 */
public record PasswordHash(String value) {

    /**
     * Compact constructor validating that the password hash is non-null and non-blank.
     *
     * @throws IllegalArgumentException if the hash is null or blank
     */
    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("user.password_hash.null_or_empty");
        }
        value = value.trim();
    }
}
