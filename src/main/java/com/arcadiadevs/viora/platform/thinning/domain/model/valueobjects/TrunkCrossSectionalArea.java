package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Value Object representing trunk diameter and trunk cross-sectional area (TCSA).
 *
 * @param trunkDiameterMm the trunk diameter in millimeters measured at 30 cm from ground (must be > 0)
 */
public record TrunkCrossSectionalArea(Double trunkDiameterMm) {

    /**
     * Compact constructor validating positive diameter.
     */
    public TrunkCrossSectionalArea {
        if (trunkDiameterMm == null || trunkDiameterMm <= 0.0) {
            throw new IllegalArgumentException("thinning.trunk_diameter.positive");
        }
    }

    /**
     * Calculates the Trunk Cross-Sectional Area (TCSA) in square centimeters ($\text{cm}^2$).
     *
     * <p>$$\text{TCSA} = \pi \times \left(\frac{d_{\text{cm}}}{2}\right)^2$$</p>
     *
     * @return TCSA in $\text{cm}^2$
     */
    public double toSquareCentimeters() {
        double radiusCm = (trunkDiameterMm / 10.0) / 2.0;
        return Math.PI * radiusCm * radiusCm;
    }
}
