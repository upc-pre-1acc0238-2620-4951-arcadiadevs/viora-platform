package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.SensorsFacts;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.LatestReading;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.LinkedNode;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.PlotSensors;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.TelemetryCoverage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SensorsFactsReader: the figures of the sensors screen, through the Telemetry facade")
class SensorsFactsReaderTest {

    /** 30 October 2026, 10:00 in Lima. */
    private static final Instant NOW = Instant.parse("2026-10-30T15:00:00Z");
    /** 24 October 2026, 00:00 in Lima: the first of the 7 days, today included. */
    private static final Instant WEEK_START = Instant.parse("2026-10-24T05:00:00Z");
    private static final String PLOT = "550e8400-e29b-41d4-a716-446655440001";

    private final TelemetryContextFacade telemetry = mock(TelemetryContextFacade.class);
    private SensorsFactsReader reader;

    @BeforeEach
    void setUp() {
        reader = new SensorsFactsReader(new ExternalTelemetryService(telemetry), Clock.fixed(NOW, ZoneOffset.UTC));
        when(telemetry.findTelemetryCoverage(PLOT, WEEK_START, NOW)).thenReturn(new TelemetryCoverage(2, 5));
    }

    @Test
    @DisplayName("the nodes, a reading of the last 24 hours and the days with readings of the week in Lima")
    void readsNodesReadingAndCoverage() {
        // 03:00 UTC on 30 October is still 29 October in Lima
        when(telemetry.findPlotSensors(PLOT)).thenReturn(new PlotSensors(List.of(
                new LinkedNode("MICROCLIMATE", null, true, null, null, null),
                new LinkedNode("SOIL_PROBE", 30, false, 16.0, 20.0, "WARNING")),
                new LatestReading(Instant.parse("2026-10-30T03:00:00Z"), 27.4, 38.0, 21.5)));

        var facts = (SensorsFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(reader.screen()).isEqualTo(ScreenKey.SENSORS);
        assertThat(facts.screen()).isEqualTo(ScreenKey.SENSORS);
        assertThat(facts.nodes()).containsExactly(
                new ClimateData.SensorNode("MICROCLIMATE", null, true, null, null, null),
                new ClimateData.SensorNode("SOIL_PROBE", 30, false, 16.0, 20.0, "WARNING"));
        assertThat(facts.simulated()).isTrue();
        assertThat(facts.latestReading()).isEqualTo(new ClimateData.Reading(27.4, 38.0, 21.5));
        assertThat(facts.coverage()).isEqualTo(new ClimateData.Coverage(LocalDate.of(2026, 10, 29), 5, 7));
        verify(telemetry).findTelemetryCoverage(PLOT, WEEK_START, NOW);
        verify(telemetry, never()).findForecastOutlook(anyString());
        verify(telemetry, never()).findOpenIncidents(any());
    }

    @Test
    @DisplayName("a reading older than 24 hours is not recent, but it still dates the latest reading of the week")
    void olderReadingIsNotRecent() {
        when(telemetry.findPlotSensors(PLOT)).thenReturn(new PlotSensors(
                List.of(new LinkedNode("SOIL_PROBE", 30, false, 16.0, 20.0, null)),
                new LatestReading(NOW.minus(Duration.ofHours(25)), 27.4, 38.0, 21.5)));

        var facts = (SensorsFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(facts.latestReading()).isNull();
        assertThat(facts.simulated()).isFalse();
        assertThat(facts.coverage()).isEqualTo(new ClimateData.Coverage(LocalDate.of(2026, 10, 29), 5, 7));
    }

    @Test
    @DisplayName("a reading exactly 24 hours old is still recent")
    void readingAtTheLimitIsRecent() {
        when(telemetry.findPlotSensors(PLOT)).thenReturn(new PlotSensors(
                List.of(new LinkedNode("SOIL_PROBE", 30, false, 16.0, 20.0, null)),
                new LatestReading(NOW.minus(Duration.ofHours(24)), 27.4, 38.0, 21.5)));

        assertThat(((SensorsFacts) reader.read(new PlotId(PLOT), 2026)).latestReading()).isNotNull();
    }

    @Test
    @DisplayName("without nodes or readings: no reading, no day of the latest one, and the days Telemetry counts")
    void withoutNodesOrReadings() {
        when(telemetry.findPlotSensors(PLOT)).thenReturn(new PlotSensors(List.of(), null));
        when(telemetry.findTelemetryCoverage(PLOT, WEEK_START, NOW)).thenReturn(new TelemetryCoverage(0, 0));

        var facts = (SensorsFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(facts.nodes()).isEmpty();
        assertThat(facts.simulated()).isFalse();
        assertThat(facts.latestReading()).isNull();
        assertThat(facts.coverage()).isEqualTo(new ClimateData.Coverage(null, 0, 7));
    }

    @Test
    @DisplayName("a failure of the Telemetry facade is not hidden as a plot without sensors")
    void facadeFailurePropagates() {
        when(telemetry.findPlotSensors(PLOT)).thenThrow(new IllegalStateException("telemetry down"));

        assertThatThrownBy(() -> reader.read(new PlotId(PLOT), 2026))
                .isInstanceOf(IllegalStateException.class).hasMessage("telemetry down");

        doReturn(new PlotSensors(List.of(), null)).when(telemetry).findPlotSensors(PLOT);
        when(telemetry.findTelemetryCoverage(PLOT, WEEK_START, NOW))
                .thenThrow(new IllegalStateException("coverage down"));
        assertThatThrownBy(() -> reader.read(new PlotId(PLOT), 2026))
                .isInstanceOf(IllegalStateException.class).hasMessage("coverage down");
    }
}
