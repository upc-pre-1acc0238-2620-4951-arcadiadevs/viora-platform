package com.arcadiadevs.viora.platform.telemetry.application.internal.commandservices;

import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider.HourlyWeatherObservation;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReading;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.TelemetrySeries;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Virtual node of a plot: fills the hours of the last week that have no reading with the weather
 * Open-Meteo observed at the plot centroid (US13, US14, US17).
 *
 * <p>No physical sensor sends readings yet, so without this the telemetry series only had the demo
 * readings and the climate sparklines and the day/night split stayed empty. It runs when the telemetry
 * is read and does nothing while the last completed hour already has a reading. The soil moisture is
 * scaled by the calibration multiplier of the plot's active soil probe, if there is one. Hours that
 * already have a reading are never overwritten.</p>
 */
@Service
public class VirtualNodeTelemetrySimulator {

    static final int PAST_DAYS = 7;
    static final Duration RETRY_COOLDOWN = Duration.ofMinutes(10);

    private final HourlyWeatherProvider hourlyWeatherProvider;
    private final ExternalOrchardService externalOrchardService;
    private final TelemetrySeriesRepository telemetrySeriesRepository;
    private final IoTDeviceRepository ioTDeviceRepository;
    private final TransactionTemplate transactionTemplate;
    private final Map<String, Instant> lastAttempts = new ConcurrentHashMap<>();

    public VirtualNodeTelemetrySimulator(
            HourlyWeatherProvider hourlyWeatherProvider,
            @Qualifier("telemetryExternalOrchardService") ExternalOrchardService externalOrchardService,
            TelemetrySeriesRepository telemetrySeriesRepository,
            IoTDeviceRepository ioTDeviceRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.hourlyWeatherProvider = hourlyWeatherProvider;
        this.externalOrchardService = externalOrchardService;
        this.telemetrySeriesRepository = telemetrySeriesRepository;
        this.ioTDeviceRepository = ioTDeviceRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /**
     * Stores a reading for every hour of the last week that has none, using the observed weather.
     *
     * @param plotId an active plot
     */
    public void fillMissingHours(PlotId plotId) {
        fillMissingHours(plotId, Instant.now());
    }

    void fillMissingHours(PlotId plotId, Instant now) {
        var currentHour = now.truncatedTo(ChronoUnit.HOURS);
        var lastCompletedHour = currentHour.minus(Duration.ofHours(1));
        var windowStart = currentHour.minus(Duration.ofDays(PAST_DAYS));

        var coveredHours = telemetrySeriesRepository.findReadingsByPlotIdAndDateRange(plotId, windowStart, now).stream()
                .map(reading -> reading.timestamp().timestamp().truncatedTo(ChronoUnit.HOURS))
                .collect(Collectors.toSet());
        if (coveredHours.contains(lastCompletedHour) || !claimAttempt(plotId, now)) {
            return;
        }

        var centroid = externalOrchardService.findPlotCentroid(plotId);
        if (centroid.isEmpty()) {
            return;
        }
        var missing = hourlyWeatherProvider.fetchPastHours(centroid.get()[0], centroid.get()[1], PAST_DAYS).stream()
                .filter(observation -> !observation.hour().isBefore(windowStart))
                .filter(observation -> !coveredHours.contains(observation.hour()))
                .toList();
        if (missing.isEmpty()) {
            return;
        }

        var soilMultiplier = soilProbeMultiplier(plotId);
        transactionTemplate.executeWithoutResult(status -> {
            var series = telemetrySeriesRepository.findByPlotId(plotId)
                    .orElseGet(() -> TelemetrySeries.create(plotId, null));
            missing.forEach(observation -> series.ingestReading(toReading(observation, soilMultiplier)));
            telemetrySeriesRepository.save(series);
        });
    }

    /** At most one call to Open-Meteo per plot every {@link #RETRY_COOLDOWN}, even if it failed. */
    private boolean claimAttempt(PlotId plotId, Instant now) {
        var claimed = new boolean[1];
        lastAttempts.compute(plotId.plotId(), (key, previous) -> {
            if (previous != null && previous.plus(RETRY_COOLDOWN).isAfter(now)) {
                return previous;
            }
            claimed[0] = true;
            return now;
        });
        return claimed[0];
    }

    private double soilProbeMultiplier(PlotId plotId) {
        return ioTDeviceRepository.findActiveByPlotId(plotId).stream()
                .map(device -> device.snapshot())
                .filter(device -> device.type() == DeviceType.SOIL_PROBE)
                .map(device -> device.calibrationMultiplier().value())
                .findFirst()
                .orElse(CalibrationMultiplier.DEFAULT_VALUE);
    }

    private static HourlyTelemetryReading toReading(HourlyWeatherObservation observation, double soilMultiplier) {
        return HourlyTelemetryReading.create(
                new ReadingTimestamp(observation.hour()),
                new AmbientTemperature(clamp(observation.temperatureCelsius(),
                        AmbientTemperature.MIN_TEMPERATURE_CELSIUS, AmbientTemperature.MAX_TEMPERATURE_CELSIUS)),
                new RelativeHumidity(clamp(observation.relativeHumidity(),
                        RelativeHumidity.MIN_HUMIDITY_PERCENT, RelativeHumidity.MAX_HUMIDITY_PERCENT)),
                new SoilMoisture(clamp(round(observation.soilMoisturePercent() * soilMultiplier),
                        SoilMoisture.MIN_SOIL_MOISTURE_PERCENT, SoilMoisture.MAX_SOIL_MOISTURE_PERCENT)),
                new SolarRadiation(Math.max(0.0, observation.solarRadiation())),
                null
        );
    }

    private static double round(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
