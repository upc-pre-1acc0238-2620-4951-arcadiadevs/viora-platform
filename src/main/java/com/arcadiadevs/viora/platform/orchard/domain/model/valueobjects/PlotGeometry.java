package com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects;

/**
 * Value Object encapsulating a cadastral georeferenced polygon and its surface area in hectares.
 * Invariant: GeoJSON must not be blank, must contain Polygon representation, and area must be strictly greater than 0.
 *
 * @param geoJson the GeoJSON string representation of the cadastral polygon in WGS84
 * @param areaHa  the computed surface area in hectares
 */
public record PlotGeometry(String geoJson, Double areaHa) {

    /**
     * Compact constructor validating polygon geometry and area invariants.
     *
     * @param geoJson the GeoJSON polygon string
     * @param areaHa  the computed surface area in hectares
     * @throws IllegalArgumentException if the GeoJSON is blank, invalid, or area is not strictly positive.
     */
    public PlotGeometry {
        if (geoJson == null || geoJson.isBlank()) {
            throw new IllegalArgumentException("plot.geometry.empty");
        }
        var trimmed = geoJson.trim();
        if (!trimmed.contains("Polygon") && !trimmed.contains("coordinates")) {
            throw new IllegalArgumentException("plot.geometry.invalid");
        }
        if (areaHa == null || areaHa <= 0.0) {
            throw new IllegalArgumentException("plot.area.positive");
        }
        geoJson = trimmed;
    }
}
