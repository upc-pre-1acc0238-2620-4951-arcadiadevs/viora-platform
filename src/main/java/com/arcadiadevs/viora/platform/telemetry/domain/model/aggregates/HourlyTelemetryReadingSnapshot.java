package com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;

/**
 * Immutable snapshot representing an hourly telemetry reading state.
 *
 * @param id                  the unique reading identifier
 * @param timestamp           the reading timestamp
 * @param temperature         ambient air temperature in °C
 * @param relativeHumidity    relative air humidity in %
 * @param soilMoisture        volumetric soil moisture in %
 * @param solarRadiation      solar radiation in W/m²
 * @param stemWaterPotential  stem water potential in MPa (nullable)
 */
public record HourlyTelemetryReadingSnapshot(
        HourlyReadingId id,
        ReadingTimestamp timestamp,
        AmbientTemperature temperature,
        RelativeHumidity relativeHumidity,
        SoilMoisture soilMoisture,
        SolarRadiation solarRadiation,
        StemWaterPotential stemWaterPotential
) {
    /**
     * Compact constructor validating that mandatory components are not null.
     */
    public HourlyTelemetryReadingSnapshot {
        if (id == null) {
            throw new IllegalArgumentException("telemetry.reading.id.null_or_empty");
        }
        if (timestamp == null) {
            throw new IllegalArgumentException("telemetry.reading_timestamp.null");
        }
        if (temperature == null) {
            throw new IllegalArgumentException("telemetry.temperature.null");
        }
        if (relativeHumidity == null) {
            throw new IllegalArgumentException("telemetry.relative_humidity.null");
        }
        if (soilMoisture == null) {
            throw new IllegalArgumentException("telemetry.soil_moisture.null");
        }
        if (solarRadiation == null) {
            throw new IllegalArgumentException("telemetry.solar_radiation.null");
        }
    }
}
