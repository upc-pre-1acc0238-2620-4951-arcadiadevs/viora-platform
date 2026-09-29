package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Operational physiological and agroclimatic status of a plot telemetry series.
 */
public enum TelemetrySeriesStatus {
    /**
     * Normal operational condition, no active agroclimatic threshold breached.
     */
    NORMAL,

    /**
     * Active soil moisture deficit or high vapor pressure deficit triggering hydric stress alert.
     */
    HYDRIC_STRESS_ACTIVE,

    /**
     * Critical minimum temperature drop triggering frost alert.
     */
    FROST_ALERT
}
