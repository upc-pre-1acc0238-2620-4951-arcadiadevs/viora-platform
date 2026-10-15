package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * Value object representing the plain 6-digit password recovery code delivered to the user.
 *
 * <p>It only travels between the code generator, the email delivery and the reset request; the aggregate keeps
 * just its hash inside {@link PasswordResetToken}.</p>
 *
 * @param value the 6-digit numeric code
 */
public record PasswordResetCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("\\d{6}");

    /**
     * Compact constructor validating the code format.
     *
     * @throws IllegalArgumentException if the code is missing or is not exactly 6 digits
     */
    public PasswordResetCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("user.reset_token.null_or_empty");
        }
        value = value.trim();
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("user.reset_token.format");
        }
    }
}
