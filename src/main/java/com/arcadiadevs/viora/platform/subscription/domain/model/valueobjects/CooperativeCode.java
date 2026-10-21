package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Value object holding a cooperative invitation code as the producer types it, such as {@code COOP-7K2Q}.
 * <p>The code is normalized (trimmed and uppercased) and must be {@code COOP-} followed by four letters or digits.
 * It is never stored: only its {@link CodeFingerprint} is.</p>
 *
 * @param value the normalized code
 */
public record CooperativeCode(String value) {

    private static final Pattern FORMAT = Pattern.compile("^COOP-[A-Z0-9]{4}$");

    /**
     * Normalizes and validates the code.
     *
     * @throws IllegalArgumentException if the code is null, blank, or does not follow the format
     */
    public CooperativeCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("subscription.invitation_code.blank");
        }
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("subscription.invitation_code.invalid_format");
        }
    }

    /**
     * Computes the fingerprint used to find the code without storing it.
     *
     * @return the SHA-256 fingerprint of the normalized code
     */
    public CodeFingerprint fingerprint() {
        return CodeFingerprint.of(this);
    }
}
