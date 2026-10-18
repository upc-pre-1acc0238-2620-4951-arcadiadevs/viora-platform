package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * Value object representing a plain 6-digit one-time code delivered to the user by email.
 *
 * <p>Used to verify the email address at sign-up. It only travels between the code generator, the email delivery
 * and the request that submits it; the aggregate keeps just its hash, inside {@link EmailVerificationToken}.</p>
 *
 * @param value the 6-digit numeric code
 */
public record VerificationCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("\\d{6}");

    /**
     * Compact constructor validating the code format.
     *
     * @throws IllegalArgumentException if the code is missing or is not exactly 6 digits
     */
    public VerificationCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("user.verification_code.null_or_empty");
        }
        value = value.trim();
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("user.verification_code.format");
        }
    }
}
