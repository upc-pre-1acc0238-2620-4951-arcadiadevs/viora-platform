package com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;

/**
 * Pure domain internal entity representing an hourly agroclimatic reading.
 * Subordinated exclusively to the {@link TelemetrySeries} aggregate root.
 */
public class HourlyTelemetryReading {

    private final HourlyReadingId id;
    private final ReadingTimestamp timestamp;
    private final AmbientTemperature temperature;
    private final RelativeHumidity relativeHumidity;
    private final SoilMoisture soilMoisture;
    private final SolarRadiation solarRadiation;
    private final StemWaterPotential stemWaterPotential;

    private HourlyTelemetryReading(
            HourlyReadingId id,
            ReadingTimestamp timestamp,
            AmbientTemperature temperature,
            RelativeHumidity relativeHumidity,
            SoilMoisture soilMoisture,
            SolarRadiation solarRadiation,
            StemWaterPotential stemWaterPotential
    ) {
        this.id = id;
        this.timestamp = timestamp;
        this.temperature = temperature;
        this.relativeHumidity = relativeHumidity;
        this.soilMoisture = soilMoisture;
        this.solarRadiation = solarRadiation;
        this.stemWaterPotential = stemWaterPotential;
    }

    /**
     * Domain factory method creating a new hourly telemetry reading.
     *
     * @param timestamp          the reading timestamp
     * @param temperature        ambient temperature in °C
     * @param relativeHumidity   relative humidity in %
     * @param soilMoisture       soil moisture in %
     * @param solarRadiation     solar radiation in W/m²
     * @param stemWaterPotential stem water potential in MPa (optional)
     * @return a new valid {@link HourlyTelemetryReading}
     */
    public static HourlyTelemetryReading create(
            ReadingTimestamp timestamp,
            AmbientTemperature temperature,
            RelativeHumidity relativeHumidity,
            SoilMoisture soilMoisture,
            SolarRadiation solarRadiation,
            StemWaterPotential stemWaterPotential
    ) {
        return new HourlyTelemetryReading(
                new HourlyReadingId(),
                timestamp,
                temperature,
                relativeHumidity,
                soilMoisture,
                solarRadiation,
                stemWaterPotential
        );
    }

    /**
     * Reconstitutes an entity from an immutable snapshot.
     *
     * @param snapshot the reading snapshot
     * @return the reconstituted {@link HourlyTelemetryReading}
     */
    public static HourlyTelemetryReading reconstitute(HourlyTelemetryReadingSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("telemetry.reading.snapshot.null");
        }
        return new HourlyTelemetryReading(
                snapshot.id(),
                snapshot.timestamp(),
                snapshot.temperature(),
                snapshot.relativeHumidity(),
                snapshot.soilMoisture(),
                snapshot.solarRadiation(),
                snapshot.stemWaterPotential()
        );
    }

    /**
     * Exports the immutable state of this internal entity.
     *
     * @return an {@link HourlyTelemetryReadingSnapshot} instance
     */
    public HourlyTelemetryReadingSnapshot snapshot() {
        return new HourlyTelemetryReadingSnapshot(
                id,
                timestamp,
                temperature,
                relativeHumidity,
                soilMoisture,
                solarRadiation,
                stemWaterPotential
        );
    }

    public HourlyReadingId id() {
        return id;
    }

    public ReadingTimestamp timestamp() {
        return timestamp;
    }

    public AmbientTemperature temperature() {
        return temperature;
    }

    public RelativeHumidity relativeHumidity() {
        return relativeHumidity;
    }

    public SoilMoisture soilMoisture() {
        return soilMoisture;
    }

    public SolarRadiation solarRadiation() {
        return solarRadiation;
    }

    public StemWaterPotential stemWaterPotential() {
        return stemWaterPotential;
    }
}
