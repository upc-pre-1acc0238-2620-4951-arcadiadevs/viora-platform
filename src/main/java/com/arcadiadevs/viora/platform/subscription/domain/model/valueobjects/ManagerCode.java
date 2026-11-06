package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value object holding a manager activation code as the technical manager types it, such as {@code GEST-7K2Q}.
 * <p>The code is normalized (trimmed and uppercased) and must be {@code GEST-} followed by four letters or digits.
 * Like a {@link CooperativeCode} it is never stored: only its {@link CodeFingerprint} is.</p>
 *
 * @param value the normalized code
 */
public record ManagerCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^GEST-[A-Z0-9]{4}$");

    /**
     * Normalizes and validates the code.
     *
     * @throws IllegalArgumentException if the code is null, blank, or does not follow the format
     */
    public ManagerCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("subscription.manager_code.blank");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("subscription.manager_code.invalid_format");
        }
    }

    /**
     * Computes the fingerprint used to find the code without storing it.
     *
     * @return the SHA-256 fingerprint of the normalized code
     */
    public CodeFingerprint fingerprint() {
        return CodeFingerprint.of(value);
    }
}
