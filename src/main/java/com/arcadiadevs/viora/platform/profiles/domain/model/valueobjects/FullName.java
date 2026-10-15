package com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects;

/**
 * Value object representing a person's full name.
 *
 * @param value sanitized full name
 */
public record FullName(String value) {

    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 150;

    /**
     * Compact constructor validating name constraints.
     *
     * @throws IllegalArgumentException if value is null, blank, or out of length bounds
     */
    public FullName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("profile.full_name.null_or_empty");
        }
        value = value.trim();
        if (value.length() < MIN_LENGTH || value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("profile.full_name.length_invalid");
        }
    }
}
