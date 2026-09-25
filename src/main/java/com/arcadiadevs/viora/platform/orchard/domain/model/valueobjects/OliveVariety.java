package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

/**
 * Botanical olive variety cultivated in the orchard plot.
 */
public enum OliveVariety {
    CRIOLLA,
    SEVILLANA,
    MANZANILLA,
    ARBEQUINA;

    /**
     * Parses and validates a botanical olive variety from its string representation.
     *
     * @param value the raw variety string
     * @return the corresponding {@link OliveVariety}
     * @throws IllegalArgumentException if the value is null, blank, or not a recognized variety
     */
    public static OliveVariety from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("plot.variety.blank");
        }
        try {
            return OliveVariety.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("plot.variety.unknown");
        }
    }
}
