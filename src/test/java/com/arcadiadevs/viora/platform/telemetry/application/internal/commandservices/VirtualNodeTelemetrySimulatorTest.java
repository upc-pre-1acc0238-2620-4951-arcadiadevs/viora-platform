package com.arcadiadevs.viora.platform.telemetry.application.internal.commandservices;

import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.meteo.HourlyWeatherProvider.HourlyWeatherObservation;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.IoTDevice;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.TelemetrySeries;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("VirtualNodeTelemetrySimulator Application Unit Tests")
class VirtualNodeTelemetrySimulatorTest {

    private static final double LATITUDE = -18.055;
    private static final double LONGITUDE = -70.245;

    @Mock
    private HourlyWeatherProvider hourlyWeatherProvider;

    @Mock
    private ExternalOrchardService externalOrchardService;

    @Mock
    private TelemetrySeriesRepository telemetrySeriesRepository;

    @Mock
    private IoTDeviceRepository ioTDeviceRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    private VirtualNodeTelemetrySimulator simulator;

    private final PlotId plotId = new PlotId();
    private final Instant now = Instant.parse("2026-10-07T15:20:00Z");

    @BeforeEach
    void setUp() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(externalOrchardService.findPlotCentroid(plotId)).thenReturn(Optional.of(new double[]{LATITUDE, LONGITUDE}));
        when(ioTDeviceRepository.findActiveByPlotId(plotId)).thenReturn(List.of());
        when(telemetrySeriesRepository.findByPlotId(plotId)).thenReturn(Optional.empty());
        when(telemetrySeriesRepository.findReadingsByPlotIdAndDateRange(eq(plotId), any(), any())).thenReturn(List.of());
        simulator = new VirtualNodeTelemetrySimulator(
                hourlyWeatherProvider,
                externalOrchardService,
                telemetrySeriesRepository,
                ioTDeviceRepository,
                transactionManager
        );
    }

    @Test
    @DisplayName("Should store one reading for every observed hour of the last week that has none")
    void shouldStoreTheMissingHours() {
        var currentHour = now.truncatedTo(ChronoUnit.HOURS);
        var observations = hoursBack(currentHour, 3);
        when(hourlyWeatherProvider.fetchPastHours(LATITUDE, LONGITUDE, 7)).thenReturn(observations);

        fill();

        var saved = savedSeries();
        assertThat(saved.readings()).hasSize(4);
        assertThat(saved.readings().get(0).temperature().celsius()).isEqualTo(20.0);
        assertThat(saved.readings().get(0).soilMoisture().percentage()).isEqualTo(20.6);
        assertThat(saved.readings().get(0).stemWaterPotential()).isNull();
    }

    @Test
    @DisplayName("Should keep the hours that already have a reading, such as the demo peaks")
    void shouldNotOverwriteCoveredHours() {
        var currentHour = now.truncatedTo(ChronoUnit.HOURS);
        var demoPeak = currentHour.minus(Duration.ofHours(2)).plus(Duration.ofMinutes(5));
        when(telemetrySeriesRepository.findReadingsByPlotIdAndDateRange(eq(plotId), any(), any()))
                .thenReturn(List.of(reading(demoPeak)));
        when(hourlyWeatherProvider.fetchPastHours(anyDouble(), anyDouble(), anyInt())).thenReturn(hoursBack(currentHour, 3));

        fill();

        var storedHours = savedSeries().readings().stream()
                .map(reading -> reading.timestamp().timestamp())
                .toList();
        assertThat(storedHours).hasSize(3).doesNotContain(currentHour.minus(Duration.ofHours(2)));
    }

    @Test
    @DisplayName("Should not call Open-Meteo while the last completed hour already has a reading")
    void shouldSkipWhenUpToDate() {
        var lastCompletedHour = now.truncatedTo(ChronoUnit.HOURS).minus(Duration.ofHours(1));
        when(telemetrySeriesRepository.findReadingsByPlotIdAndDateRange(eq(plotId), any(), any()))
                .thenReturn(List.of(reading(lastCompletedHour)));

        fill();

        verifyNoInteractions(hourlyWeatherProvider);
        verify(telemetrySeriesRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should call Open-Meteo at most once per plot every ten minutes, even after a failure")
    void shouldWaitBeforeRetrying() {
        when(hourlyWeatherProvider.fetchPastHours(anyDouble(), anyDouble(), anyInt())).thenReturn(List.of());

        fill();
        fill();
        simulatorAt(now.plus(Duration.ofMinutes(11)));

        verify(hourlyWeatherProvider, times(2)).fetchPastHours(anyDouble(), anyDouble(), anyInt());
        verify(telemetrySeriesRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should scale the soil moisture with the calibration of the active soil probe")
    void shouldApplyTheSoilProbeCalibration() {
        var probe = IoTDevice.register(
                plotId,
                new DeviceName("Sonda 30 cm"),
                DeviceType.SOIL_PROBE,
                new SensorDepth(30),
                SoilTextureType.SANDY_LOAM,
                new CalibrationMultiplier(1.5)
        );
        when(ioTDeviceRepository.findActiveByPlotId(plotId)).thenReturn(List.of(probe));
        when(hourlyWeatherProvider.fetchPastHours(anyDouble(), anyDouble(), anyInt()))
                .thenReturn(hoursBack(now.truncatedTo(ChronoUnit.HOURS), 0));

        fill();

        assertThat(savedSeries().readings().get(0).soilMoisture().percentage()).isEqualTo(30.9);
    }

    private void fill() {
        simulatorAt(now);
    }

    private void simulatorAt(Instant instant) {
        simulator.fillMissingHours(plotId, instant);
    }

    private TelemetrySeries savedSeries() {
        var captor = ArgumentCaptor.forClass(TelemetrySeries.class);
        verify(telemetrySeriesRepository).save(captor.capture());
        return captor.getValue();
    }

    /** The hours from {@code hours} hours before the current hour up to the current hour, oldest first. */
    private static List<HourlyWeatherObservation> hoursBack(Instant currentHour, int hours) {
        var observations = new ArrayList<HourlyWeatherObservation>();
        for (int i = hours; i >= 0; i--) {
            observations.add(new HourlyWeatherObservation(currentHour.minus(Duration.ofHours(i)), 20.0, 60.0, 20.6, 500.0));
        }
        return observations;
    }

    private static HourlyTelemetryReadingSnapshot reading(Instant timestamp) {
        return new HourlyTelemetryReadingSnapshot(
                new HourlyReadingId(),
                new ReadingTimestamp(timestamp),
                new AmbientTemperature(34.0),
                new RelativeHumidity(45.0),
                new SoilMoisture(22.0),
                new SolarRadiation(750.0),
                null
        );
    }
}
