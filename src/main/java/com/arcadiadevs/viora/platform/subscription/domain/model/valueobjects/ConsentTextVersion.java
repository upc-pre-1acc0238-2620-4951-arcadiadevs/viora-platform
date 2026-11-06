package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * Value object naming the version of the consent text a producer read and accepted, such as {@code 2026-11-v1}. The
 * text itself lives in the app; the version tells later which wording was accepted.
 *
 * @param value the version: letters, digits, dots and dashes, up to 40 characters
 */
public record ConsentTextVersion(String value) {

    private static final Pattern FORMAT = Pattern.compile("^[A-Za-z0-9.\\-]{1,40}$");

    /**
     * Validates the version.
     *
     * @throws IllegalArgumentException if it is null, blank or not a plain version name
     */
    public ConsentTextVersion {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("subscription.consent.text_version.blank");
        }
        value = value.trim();
        if (!FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("subscription.consent.text_version.invalid");
        }
    }
}
