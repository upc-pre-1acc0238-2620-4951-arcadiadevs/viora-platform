package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * Value object representing a normalized, validated email address under RFC 5322.
 *
 * @param value the normalized lowercase email address
 */
public record EmailAddress(String value) {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Compact constructor validating non-null, valid syntax, and normalizing to lowercase.
     *
     * @throws IllegalArgumentException if email is null, blank, or invalid
     */
    public EmailAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("user.email.null_or_empty");
        }
        value = value.trim().toLowerCase();
        if (!EMAIL_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("user.email.invalid_format");
        }
    }
}
