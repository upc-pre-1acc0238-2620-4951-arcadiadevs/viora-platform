package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * USDA soil textural classification used to parameterize volumetric water content calculations.
 */
public enum SoilTextureType {
    SANDY,
    LOAMY_SAND,
    SANDY_LOAM,
    LOAM,
    SILT_LOAM,
    SILT,
    SANDY_CLAY_LOAM,
    CLAY_LOAM,
    SILTY_CLAY_LOAM,
    SANDY_CLAY,
    SILTY_CLAY,
    CLAY,
    NOT_APPLICABLE;

    /**
     * Parses and validates a soil texture type from its string representation.
     *
     * @param value the raw soil texture type string
     * @return the corresponding {@link SoilTextureType}
     * @throws IllegalArgumentException if the value is null, blank, or not recognized
     */
    public static SoilTextureType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("device.soil_texture.blank");
        }
        try {
            return SoilTextureType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("device.soil_texture.unknown");
        }
    }
}
