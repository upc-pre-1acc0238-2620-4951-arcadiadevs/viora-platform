package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

/**
 * Value object capturing the specific telemetry metric and values that triggered the incident.
 *
 * @param metricName     the observed metric name (e.g. "SOIL_MOISTURE_30CM", "TEMPERATURE", "FORECAST_MIN_TEMP")
 * @param currentValue   the current recorded or forecasted reading value
 * @param thresholdValue the threshold boundary that was breached
 * @param unit           the measurement unit (e.g. "%", "°C")
 */
public record ThresholdBreachInfo(
        String metricName,
        Double currentValue,
        Double thresholdValue,
        String unit
) {

    /**
     * Compact constructor enforcing non-nullity and non-blank invariant checks.
     */
    public ThresholdBreachInfo {
        if (metricName == null || metricName.isBlank()) {
            throw new IllegalArgumentException("incident.breach_info.metric_name.null");
        }
        if (currentValue == null) {
            throw new IllegalArgumentException("incident.breach_info.current_value.null");
        }
        if (thresholdValue == null) {
            throw new IllegalArgumentException("incident.breach_info.threshold_value.null");
        }
        if (unit == null || unit.isBlank()) {
            throw new IllegalArgumentException("incident.breach_info.unit.null");
        }
    }
}
