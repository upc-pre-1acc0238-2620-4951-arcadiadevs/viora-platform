package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ClimateThresholds;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ForecastDayOutlook;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ForecastOutlook;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.LatestReading;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.LinkedNode;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.PlotSensors;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ClimateFactsReader: the figures of the climate screen, through the Telemetry facade")
class ClimateFactsReaderTest {

    private static final Instant NOW = Instant.parse("2026-10-30T15:00:00Z");
    private static final String PLOT = "550e8400-e29b-41d4-a716-446655440001";

    private final TelemetryContextFacade telemetry = mock(TelemetryContextFacade.class);
    private ClimateFactsReader reader;

    @BeforeEach
    void setUp() {
        reader = new ClimateFactsReader(new ExternalTelemetryService(telemetry), Clock.fixed(NOW, ZoneOffset.UTC));
        when(telemetry.findClimateThresholds()).thenReturn(new ClimateThresholds(32.0, 36.0, 1.5));
        when(telemetry.findForecastOutlook(PLOT)).thenReturn(Optional.of(new ForecastOutlook(NOW,
                List.of(new ForecastDayOutlook(LocalDate.of(2026, 10, 30), 34.0, 14.0, true, false)))));
        when(telemetry.findPlotSensors(PLOT)).thenReturn(new PlotSensors(
                List.of(new LinkedNode("SOIL_PROBE", 30, true, 16.0, 20.0, "WARNING")),
                new LatestReading(NOW.minus(Duration.ofHours(2)), 27.4, 38.0, 18.0)));
    }

    @Test
    @DisplayName("the forecast, the nodes as Telemetry judged them and a reading of the last 24 hours")
    void readsForecastAndSensors() {
        var facts = (ClimateFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(reader.screen()).isEqualTo(ScreenKey.CLIMATE);
        assertThat(facts.nodes()).singleElement().satisfies(node -> {
            assertThat(node.simulated()).isTrue();
            assertThat(node.depthCm()).isEqualTo(30);
            assertThat(node.warningMoisture()).isEqualTo(20.0);
            assertThat(node.moistureRisk()).isEqualTo("WARNING");
        });
        assertThat(facts.latestReading().soilMoisture()).isEqualTo(18.0);
        assertThat(facts.forecast().days()).singleElement().satisfies(day -> assertThat(day.heatRisk()).isTrue());
        assertThat(facts.thresholds().heatWarning()).isEqualTo(32.0);
        verify(telemetry, never()).findOpenIncidents(any());
    }

    @Test
    @DisplayName("a reading older than 24 hours is not recent, and a missing forecast stays missing")
    void olderReadingIsNotRecent() {
        when(telemetry.findPlotSensors(PLOT)).thenReturn(new PlotSensors(
                List.of(new LinkedNode("MICROCLIMATE", null, true, null, null, null)),
                new LatestReading(NOW.minus(Duration.ofHours(25)), 27.4, 38.0, 18.0)));
        when(telemetry.findForecastOutlook(PLOT)).thenReturn(Optional.empty());

        var facts = (ClimateFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(facts.latestReading()).isNull();
        assertThat(facts.forecast()).isNull();
        assertThat(facts.nodes()).hasSize(1);

        when(telemetry.findPlotSensors(PLOT)).thenReturn(new PlotSensors(List.of(), null));
        assertThat(((ClimateFacts) reader.read(new PlotId(PLOT), 2026)).latestReading()).isNull();
    }
}
