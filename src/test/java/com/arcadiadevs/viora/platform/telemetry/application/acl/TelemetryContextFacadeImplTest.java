package com.arcadiadevs.viora.platform.telemetry.application.acl;

import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.WeatherForecastProvider;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncidentSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDeviceSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.WeatherForecastDay;
import com.arcadiadevs.viora.platform.telemetry.domain.model.entities.MitigationStepSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.services.AgroclimaticThresholdEvaluator;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ClimateThresholds;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.LinkedNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("TelemetryContextFacade: thresholds, open incidents, forecast and sensors for other contexts")
class TelemetryContextFacadeImplTest {

    private static final Instant NOW = Instant.parse("2026-10-30T15:00:00Z");
    private static final String PLOT = "550e8400-e29b-41d4-a716-446655440001";
    private static final String OTHER_PLOT = "550e8400-e29b-41d4-a716-446655440002";

    private final IoTDeviceRepository devices = mock(IoTDeviceRepository.class);
    private final TelemetrySeriesRepository series = mock(TelemetrySeriesRepository.class);
    private final AgroclimaticIncidentRepository incidents = mock(AgroclimaticIncidentRepository.class);
    private final WeatherForecastProvider forecastProvider = mock(WeatherForecastProvider.class);
    private final ExternalOrchardService orchard = mock(ExternalOrchardService.class);
    private TelemetryContextFacadeImpl facade;

    @BeforeEach
    void setUp() {
        facade = new TelemetryContextFacadeImpl(devices, series, incidents, forecastProvider, orchard,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    @DisplayName("Thresholds are the evaluator's own: 32 and 36 °C for heat, 1.5 °C for frost")
    void thresholdsComeFromTheEvaluator() {
        assertThat(facade.findClimateThresholds()).isEqualTo(new ClimateThresholds(
                AgroclimaticThresholdEvaluator.HEAT_WARNING_CELSIUS,
                AgroclimaticThresholdEvaluator.HEAT_CRITICAL_CELSIUS,
                AgroclimaticThresholdEvaluator.FROST_WARNING_CELSIUS));
        assertThat(facade.findClimateThresholds().heatWarningCelsius()).isEqualTo(32.0);
        assertThat(facade.findClimateThresholds().heatCriticalCelsius()).isEqualTo(36.0);
    }

    @Test
    @DisplayName("Open incidents are the active and snoozed ones, with their breach and steps")
    void listsOpenIncidents() {
        when(incidents.findByPlotId(new PlotId(PLOT))).thenReturn(List.of(
                incident(PLOT, IncidentType.HEAT_WAVE, IncidentSeverity.WARNING, IncidentStatus.ACTIVE, 34.2, 32.0),
                incident(PLOT, IncidentType.FROST_WARNING, IncidentSeverity.CRITICAL, IncidentStatus.SNOOZED, -2.5,
                        -2.0),
                incident(PLOT, IncidentType.HYDRIC_STRESS, IncidentSeverity.WARNING, IncidentStatus.NORMALIZED,
                        18.0, 20.0)));

        var open = facade.findOpenIncidents(PLOT);

        assertThat(open).extracting(incident -> incident.type()).containsExactly("HEAT_WAVE", "FROST_WARNING");
        var heat = open.get(0);
        assertThat(heat.severity()).isEqualTo("WARNING");
        assertThat(heat.currentValue()).isEqualTo(34.2);
        assertThat(heat.thresholdValue()).isEqualTo(32.0);
        assertThat(heat.unit()).isEqualTo("°C");
        assertThat(heat.steps()).isEqualTo(3);
        assertThat(heat.completedSteps()).isEqualTo(1);
        assertThat(facade.findOpenIncidents("not-a-uuid")).isEmpty();
    }

    @Test
    @DisplayName("Counts the given plots with an open incident of the type, never other plots")
    void countsPlotsWithOpenIncident() {
        var plots = List.of(new PlotId(PLOT), new PlotId(OTHER_PLOT));
        when(incidents.findAllByPlotIds(plots, null, null)).thenReturn(List.of(
                incident(PLOT, IncidentType.HEAT_WAVE, IncidentSeverity.WARNING, IncidentStatus.ACTIVE, 34.0, 32.0),
                incident(PLOT, IncidentType.HEAT_WAVE, IncidentSeverity.CRITICAL, IncidentStatus.ACTIVE, 37.0, 36.0),
                incident(OTHER_PLOT, IncidentType.HEAT_WAVE, IncidentSeverity.WARNING, IncidentStatus.NORMALIZED,
                        33.0, 32.0),
                incident(OTHER_PLOT, IncidentType.FROST_WARNING, IncidentSeverity.WARNING, IncidentStatus.ACTIVE,
                        1.0, 1.5)));

        assertThat(facade.countPlotsWithOpenIncident(List.of(PLOT, OTHER_PLOT), "HEAT_WAVE")).isEqualTo(1);
        assertThat(facade.countPlotsWithOpenIncident(List.of(PLOT, OTHER_PLOT), "FROST_WARNING")).isEqualTo(1);
        assertThat(facade.countPlotsWithOpenIncident(List.of(PLOT, OTHER_PLOT), "UNKNOWN")).isZero();
        assertThat(facade.countPlotsWithOpenIncident(List.of(), "HEAT_WAVE")).isZero();
    }

    @Test
    @DisplayName("The forecast flags each day with the evaluator, never raises incidents and is cached a while")
    void forecastOutlook() {
        when(orchard.findPlotCentroid(new PlotId(PLOT))).thenReturn(Optional.of(new double[]{-18.05, -70.25}));
        when(forecastProvider.fetchSevenDayForecast(anyDouble(), anyDouble())).thenReturn(List.of(
                day(LocalDate.of(2026, 10, 30), 34.0, 14.0),
                day(LocalDate.of(2026, 10, 31), 31.9, 1.5),
                day(LocalDate.of(2026, 11, 1), 36.4, 12.0)));

        var outlook = facade.findForecastOutlook(PLOT).orElseThrow();
        facade.findForecastOutlook(PLOT);

        assertThat(outlook.days()).extracting(day -> day.heatRisk()).containsExactly(true, false, true);
        assertThat(outlook.days()).extracting(day -> day.frostRisk()).containsExactly(false, true, false);
        assertThat(outlook.days().get(0).maxCelsius()).isEqualTo(34.0);
        assertThat(outlook.syncedAt()).isEqualTo(NOW);
        verify(forecastProvider, times(1)).fetchSevenDayForecast(-18.05, -70.25);
        verify(incidents, never()).save(any());
    }

    @Test
    @DisplayName("No location or no forecast gives no outlook, and an empty forecast is not cached")
    void forecastMissing() {
        when(orchard.findPlotCentroid(new PlotId(PLOT))).thenReturn(Optional.of(new double[]{-18.05, -70.25}));
        when(forecastProvider.fetchSevenDayForecast(anyDouble(), anyDouble())).thenReturn(List.of());

        assertThat(facade.findForecastOutlook(PLOT)).isEmpty();
        assertThat(facade.findForecastOutlook(PLOT)).isEmpty();
        verify(forecastProvider, times(2)).fetchSevenDayForecast(-18.05, -70.25);

        when(orchard.findPlotCentroid(new PlotId(OTHER_PLOT))).thenReturn(Optional.empty());
        assertThat(facade.findForecastOutlook(OTHER_PLOT)).isEmpty();
        assertThat(facade.findForecastOutlook("not-a-uuid")).isEmpty();
    }

    @Test
    @DisplayName("Sensors: every node is simulated, soil probes carry the evaluator thresholds of their texture")
    void plotSensors() {
        when(devices.findActiveByPlotId(new PlotId(PLOT))).thenReturn(List.of(
                device(DeviceType.SOIL_PROBE, 30, SoilTextureType.CLAY),
                device(DeviceType.MICROCLIMATE, null, SoilTextureType.NOT_APPLICABLE)));
        when(series.findReadingsByPlotIdAndDateRange(eq(new PlotId(PLOT)), any(), any())).thenReturn(List.of(
                reading(NOW.minus(Duration.ofHours(3)), 25.0, 40.0, 22.0),
                reading(NOW.minus(Duration.ofHours(1)), 27.4, 38.0, 21.5)));

        var sensors = facade.findPlotSensors(PLOT);

        assertThat(sensors.nodes()).containsExactly(
                new LinkedNode("SOIL_PROBE", 30, true, 20.0, 24.0, "WARNING"),
                new LinkedNode("MICROCLIMATE", null, true, null, null, null));
        assertThat(sensors.latestReading().readAt()).isEqualTo(NOW.minus(Duration.ofHours(1)));
        assertThat(sensors.latestReading().temperatureCelsius()).isEqualTo(27.4);
        assertThat(sensors.latestReading().relativeHumidity()).isEqualTo(38.0);
        assertThat(sensors.latestReading().soilMoisturePercent()).isEqualTo(21.5);
        verify(series).findReadingsByPlotIdAndDateRange(new PlotId(PLOT), NOW.minus(Duration.ofDays(7)), NOW);
    }

    @Test
    @DisplayName("Sensors of a plot without nodes or readings, or a malformed plot, are empty")
    void plotSensorsEmpty() {
        when(devices.findActiveByPlotId(new PlotId(PLOT))).thenReturn(List.of());
        when(series.findReadingsByPlotIdAndDateRange(eq(new PlotId(PLOT)), any(), any())).thenReturn(List.of());

        assertThat(facade.findPlotSensors(PLOT).nodes()).isEmpty();
        assertThat(facade.findPlotSensors(PLOT).latestReading()).isNull();
        assertThat(facade.findPlotSensors("not-a-uuid").nodes()).isEmpty();
    }

    private static AgroclimaticIncident incident(String plotId, IncidentType type, IncidentSeverity severity,
            IncidentStatus status, double value, double threshold) {
        var unit = type == IncidentType.HYDRIC_STRESS ? "%" : "°C";
        var steps = List.of(
                new MitigationStepSnapshot(new MitigationStepId(UUID.randomUUID().toString()), "step.one", true, NOW),
                new MitigationStepSnapshot(new MitigationStepId(UUID.randomUUID().toString()), "step.two", false,
                        null),
                new MitigationStepSnapshot(new MitigationStepId(UUID.randomUUID().toString()), "step.three", false,
                        null));
        return AgroclimaticIncident.reconstitute(new AgroclimaticIncidentSnapshot(
                new IncidentId(UUID.randomUUID().toString()), new PlotId(plotId), type, severity, status,
                new ThresholdBreachInfo("METRIC", value, threshold, unit), NOW.minus(Duration.ofHours(5)),
                status == IncidentStatus.NORMALIZED ? NOW : null, null, 0L, steps, 0L));
    }

    private static WeatherForecastDay day(LocalDate date, double max, double min) {
        return WeatherForecastDay.create(date, new AmbientTemperature(max), new AmbientTemperature(min),
                new Percentage(0.0), new WindSpeed(10.0), NOW);
    }

    private static IoTDevice device(DeviceType type, Integer depth, SoilTextureType texture) {
        return IoTDevice.reconstitute(new IoTDeviceSnapshot(new DeviceId(UUID.randomUUID().toString()),
                new PlotId(PLOT), new DeviceName("Nodo " + type), type, new SensorDepth(depth), texture,
                new CalibrationMultiplier(1.0), DeviceStatus.ACTIVE, NOW, 0L));
    }

    private static HourlyTelemetryReadingSnapshot reading(Instant at, double temperature, double humidity,
            double soil) {
        return new HourlyTelemetryReadingSnapshot(new HourlyReadingId(UUID.randomUUID().toString()),
                new ReadingTimestamp(at), new AmbientTemperature(temperature), new RelativeHumidity(humidity),
                new SoilMoisture(soil), new SolarRadiation(100.0), null);
    }
}
