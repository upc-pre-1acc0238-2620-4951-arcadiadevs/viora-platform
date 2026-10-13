package com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * Pure Domain Value Object representing an international telephone number conforming to ITU-T E.164.
 *
 * <p>Enforces the standard E.164 invariant (+ followed by 7 to 15 decimal digits).</p>
 *
 * @param value normalized E.164 phone string (e.g. {@code +51987654321})
 */
public record PhoneNumber(String value) {

    // ITU-T E.164 recommendation: leading '+' followed by 7 to 15 digits (country code + subscriber)
    private static final Pattern E164_PATTERN = Pattern.compile("^\\+[1-9]\\d{6,14}$");

    /**
     * Compact constructor validating that the phone conforms strictly to ITU-T E.164.
     *
     * @throws IllegalArgumentException if the value is null, blank, or does not match E.164 format
     */
    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("profile.phone.null_or_empty");
        }
        value = value.trim();
        if (!E164_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException("profile.phone.invalid_e164");
        }
    }
}
