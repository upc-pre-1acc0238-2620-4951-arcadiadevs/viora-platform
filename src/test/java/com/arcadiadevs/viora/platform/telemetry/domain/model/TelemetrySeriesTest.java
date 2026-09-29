package com.arcadiadevs.viora.platform.telemetry.domain.model;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReading;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.TelemetrySeries;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TelemetrySeries Aggregate Root and Value Objects Domain Unit Tests")
class TelemetrySeriesTest {

    private final PlotId plotId = new PlotId();
    private final DeviceId deviceId = new DeviceId();

    @Test
    @DisplayName("Should successfully create a new telemetry series with default NORMAL status")
    void shouldCreateTelemetrySeriesSuccessfully() {
        var series = TelemetrySeries.create(plotId, deviceId);

        assertThat(series).isNotNull();
        var snapshot = series.snapshot();
        assertThat(snapshot.id()).isNotNull();
        assertThat(snapshot.plotId().plotId()).isEqualTo(plotId.plotId());
        assertThat(snapshot.sensorNodeId().deviceId()).isEqualTo(deviceId.deviceId());
        assertThat(snapshot.currentStatus()).isEqualTo(TelemetrySeriesStatus.NORMAL);
        assertThat(snapshot.readings()).isEmpty();
        assertThat(snapshot.revision()).isEqualTo(0L);
    }

    @Test
    @DisplayName("Should ingest normal hourly reading and maintain NORMAL status")
    void shouldIngestNormalHourlyReading() {
        var series = TelemetrySeries.create(plotId, deviceId);

        var reading = HourlyTelemetryReading.create(
                new ReadingTimestamp(Instant.now()),
                new AmbientTemperature(22.5),
                new RelativeHumidity(55.0),
                new SoilMoisture(25.0),
                new SolarRadiation(600.0),
                new StemWaterPotential(-1.2)
        );

        series.ingestReading(reading);

        var snapshot = series.snapshot();
        assertThat(snapshot.readings()).hasSize(1);
        assertThat(snapshot.currentStatus()).isEqualTo(TelemetrySeriesStatus.NORMAL);
        assertThat(snapshot.readings().get(0).temperature().celsius()).isEqualTo(22.5);
        assertThat(snapshot.readings().get(0).relativeHumidity().percentage()).isEqualTo(55.0);
        assertThat(snapshot.readings().get(0).soilMoisture().percentage()).isEqualTo(25.0);
        assertThat(snapshot.readings().get(0).solarRadiation().wattsPerSquareMeter()).isEqualTo(600.0);
        assertThat(snapshot.readings().get(0).stemWaterPotential().stemWaterPotentialMpa()).isEqualTo(-1.2);
    }

    @Test
    @DisplayName("Should transition status to FROST_ALERT when temperature drops to 0°C or below")
    void shouldTransitionToFrostAlertWhenTemperatureFreezes() {
        var series = TelemetrySeries.create(plotId, deviceId);

        var frostReading = HourlyTelemetryReading.create(
                new ReadingTimestamp(Instant.now()),
                new AmbientTemperature(-1.5),
                new RelativeHumidity(80.0),
                new SoilMoisture(30.0),
                new SolarRadiation(0.0),
                null
        );

        series.ingestReading(frostReading);

        assertThat(series.currentStatus()).isEqualTo(TelemetrySeriesStatus.FROST_ALERT);
    }

    @Test
    @DisplayName("Should transition status to HYDRIC_STRESS_ACTIVE when soil moisture falls below 15%")
    void shouldTransitionToHydricStressWhenSoilMoistureDrops() {
        var series = TelemetrySeries.create(plotId, deviceId);

        var dryReading = HourlyTelemetryReading.create(
                new ReadingTimestamp(Instant.now()),
                new AmbientTemperature(28.0),
                new RelativeHumidity(35.0),
                new SoilMoisture(12.0),
                new SolarRadiation(850.0),
                new StemWaterPotential(-2.8)
        );

        series.ingestReading(dryReading);

        assertThat(series.currentStatus()).isEqualTo(TelemetrySeriesStatus.HYDRIC_STRESS_ACTIVE);
    }

    @Test
    @DisplayName("Should reconstitute aggregate from snapshot preserving all state")
    void shouldReconstituteAggregateFromSnapshot() {
        var series = TelemetrySeries.create(plotId, deviceId);
        var reading = HourlyTelemetryReading.create(
                new ReadingTimestamp(Instant.parse("2026-09-28T12:00:00Z")),
                new AmbientTemperature(20.0),
                new RelativeHumidity(50.0),
                new SoilMoisture(22.0),
                new SolarRadiation(500.0),
                null
        );
        series.ingestReading(reading);

        var snap = series.snapshot();
        var reconstituted = TelemetrySeries.reconstitute(snap);

        assertThat(reconstituted.id().seriesId()).isEqualTo(series.id().seriesId());
        assertThat(reconstituted.plotId().plotId()).isEqualTo(series.plotId().plotId());
        assertThat(reconstituted.readings()).hasSize(1);
        assertThat(reconstituted.readings().get(0).temperature().celsius()).isEqualTo(20.0);
    }

    @Test
    @DisplayName("AmbientTemperature should enforce limits between -30°C and 70°C")
    void shouldEnforceAmbientTemperatureInvariants() {
        assertThatThrownBy(() -> new AmbientTemperature(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.temperature.null");

        assertThatThrownBy(() -> new AmbientTemperature(-35.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.temperature.out_of_range");

        assertThatThrownBy(() -> new AmbientTemperature(75.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.temperature.out_of_range");

        var validTemp = new AmbientTemperature(25.4);
        assertThat(validTemp.celsius()).isEqualTo(25.4);
    }

    @Test
    @DisplayName("RelativeHumidity should enforce limits between 0.0% and 100.0%")
    void shouldEnforceRelativeHumidityInvariants() {
        assertThatThrownBy(() -> new RelativeHumidity(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.relative_humidity.null");

        assertThatThrownBy(() -> new RelativeHumidity(-5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.relative_humidity.invalid_percentage");

        assertThatThrownBy(() -> new RelativeHumidity(105.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.relative_humidity.invalid_percentage");

        var valid = new RelativeHumidity(72.5);
        assertThat(valid.percentage()).isEqualTo(72.5);
    }

    @Test
    @DisplayName("SoilMoisture should enforce limits between 0.0% and 100.0%")
    void shouldEnforceSoilMoistureInvariants() {
        assertThatThrownBy(() -> new SoilMoisture(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.soil_moisture.null");

        assertThatThrownBy(() -> new SoilMoisture(-1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.soil_moisture.invalid_percentage");

        assertThatThrownBy(() -> new SoilMoisture(101.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.soil_moisture.invalid_percentage");

        var valid = new SoilMoisture(33.0);
        assertThat(valid.percentage()).isEqualTo(33.0);
    }

    @Test
    @DisplayName("SolarRadiation should enforce non-negative values")
    void shouldEnforceSolarRadiationInvariants() {
        assertThatThrownBy(() -> new SolarRadiation(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.solar_radiation.null");

        assertThatThrownBy(() -> new SolarRadiation(-10.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.solar_radiation.negative");

        var valid = new SolarRadiation(0.0);
        assertThat(valid.wattsPerSquareMeter()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("StemWaterPotential should reject out-of-range values")
    void shouldEnforceStemWaterPotentialInvariants() {
        assertThatThrownBy(() -> new StemWaterPotential(-6.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.stem_water_potential.out_of_range");

        assertThatThrownBy(() -> new StemWaterPotential(1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.stem_water_potential.out_of_range");

        var valid = new StemWaterPotential(-1.5);
        assertThat(valid.stemWaterPotentialMpa()).isEqualTo(-1.5);
    }

    @Test
    @DisplayName("TelemetrySeriesId and HourlyReadingId should validate UUID strings")
    void shouldValidateIdentifiers() {
        assertThatThrownBy(() -> new TelemetrySeriesId(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.series.id.null_or_empty");

        assertThatThrownBy(() -> new TelemetrySeriesId("invalid-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.series.id.invalid_uuid");

        assertThatThrownBy(() -> new HourlyReadingId("not-a-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("telemetry.reading.id.invalid_uuid");

        var randomSeriesId = new TelemetrySeriesId();
        assertThat(randomSeriesId.seriesId()).isNotBlank();
    }
}
