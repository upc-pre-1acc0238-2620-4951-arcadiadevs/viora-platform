package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

/**
 * Value Object representing the agronomic descriptive name of a plot.
 * Invariant: Must not be blank and length must be between 3 and 100 characters.
 *
 * @param value the string representation of the plot name
 */
public record PlotName(String value) {

    /**
     * Compact constructor enforcing name length and non-blank invariants.
     *
     * @param value the plot name string
     */
    public PlotName {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("plot.name.blank");
        }
        var trimmed = value.trim();
        if (trimmed.length() < 3 || trimmed.length() > 100) {
            throw new IllegalArgumentException("plot.name.invalid_length");
        }
        value = trimmed;
    }
}
