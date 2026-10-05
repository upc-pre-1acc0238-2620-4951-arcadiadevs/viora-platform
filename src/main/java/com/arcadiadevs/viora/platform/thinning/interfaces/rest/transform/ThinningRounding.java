package com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform;

/** Rounds the figures the thinning API shows; the domain always works at full precision. */
public final class ThinningRounding {

    /** Unit of every crop load of the thinning API. */
    public static final String LOAD_UNIT = "FRUITS_PER_SHOOT";

    private ThinningRounding() {
    }

    /**
     * Rounds a crop load (fruits per shoot) to three decimals.
     *
     * @param value load, or null
     * @return the rounded load, or null
     */
    public static Double load(Double value) {
        return value == null ? null : Math.round(value * 1000.0) / 1000.0;
    }

    /**
     * Rounds a percentage to two decimals.
     *
     * @param value percentage, or null
     * @return the rounded percentage, or null
     */
    public static Double percentage(Double value) {
        return value == null ? null : Math.round(value * 100.0) / 100.0;
    }
}
