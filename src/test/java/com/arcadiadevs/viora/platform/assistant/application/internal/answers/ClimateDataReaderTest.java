package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ClimateThresholds;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ForecastDayOutlook;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ForecastOutlook;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.LatestReading;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.LinkedNode;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.OpenIncident;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.PlotSensors;
import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Climate data reader: what the ALERT and CLIMATE screens read from Telemetry")
class ClimateDataReaderTest {

    private static final Instant NOW = Instant.parse("2026-10-30T15:00:00Z");
    private static final String PLOT = "550e8400-e29b-41d4-a716-446655440001";
    private static final String PRODUCER = "6f1c2a10-0000-4000-8000-000000000001";

    private final TelemetryContextFacade telemetry = mock(TelemetryContextFacade.class);
    private final OrchardContextFacade orchard = mock(OrchardContextFacade.class);
    private ClimateDataReader reader;

    @BeforeEach
    void setUp() {
        reader = new ClimateDataReader(new ExternalTelemetryService(telemetry), new ExternalOrchardService(orchard),
                Clock.fixed(NOW, ZoneOffset.UTC));
        when(telemetry.findClimateThresholds()).thenReturn(new ClimateThresholds(32.0, 36.0, 1.5));
        when(telemetry.findForecastOutlook(PLOT)).thenReturn(Optional.of(new ForecastOutlook(NOW,
                List.of(new ForecastDayOutlook(LocalDate.of(2026, 10, 30), 34.0, 14.0, true, false)))));
        when(telemetry.findPlotSensors(PLOT)).thenReturn(new PlotSensors(
                List.of(new LinkedNode("SOIL_PROBE", 30, true, 16.0, 20.0, "WARNING")),
                new LatestReading(NOW.minus(Duration.ofHours(2)), 27.4, 38.0, 18.0)));
    }

    @Test
    @DisplayName("ALERT: the most severe open alert, counted over the owner's plots only")
    void alertReadsTheMostSevereIncident() {
        when(telemetry.findOpenIncidents(PLOT)).thenReturn(List.of(
                incident("HEAT_WAVE", "WARNING", "2026-10-29T20:00:00Z"),
                incident("FROST_WARNING", "CRITICAL", "2026-10-28T10:00:00Z"),
                incident("WARM_WINTER", "CRITICAL", "2026-10-30T10:00:00Z")));
        when(orchard.findPlotProducerId(PLOT)).thenReturn(Optional.of(PRODUCER));
        when(orchard.findActivePlotIdsByProducerId(PRODUCER)).thenReturn(List.of(PLOT, "other"));
        when(telemetry.countPlotsWithOpenIncident(List.of(PLOT, "other"), "FROST_WARNING")).thenReturn(2);

        var climate = reader.read(ScreenKey.ALERT, new PlotId(PLOT));

        assertThat(climate.incident().type()).isEqualTo("FROST_WARNING");
        assertThat(climate.incident().triggeredOn()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(climate.plotsAtRisk()).isEqualTo(2);
        assertThat(climate.activePlots()).isEqualTo(2);
        assertThat(climate.thresholds().heatWarning()).isEqualTo(32.0);
        assertThat(climate.forecast().days()).hasSize(1);
        assertThat(climate.forecast().syncedOn()).isEqualTo(LocalDate.of(2026, 10, 30));
        verify(telemetry, never()).findPlotSensors(anyString());
    }

    @Test
    @DisplayName("ALERT: a hydric alert reads no forecast, and without an alert no plot is counted")
    void alertWithoutForecastNeed() {
        when(telemetry.findOpenIncidents(PLOT)).thenReturn(List.of(incident("HYDRIC_STRESS", "WARNING",
                "2026-10-29T20:00:00Z")));
        when(orchard.findPlotProducerId(PLOT)).thenReturn(Optional.of(PRODUCER));
        when(orchard.findActivePlotIdsByProducerId(PRODUCER)).thenReturn(List.of(PLOT));
        when(telemetry.countPlotsWithOpenIncident(List.of(PLOT), "HYDRIC_STRESS")).thenReturn(1);

        assertThat(reader.read(ScreenKey.ALERT, new PlotId(PLOT)).forecast()).isNull();
        verify(telemetry, never()).findForecastOutlook(anyString());

        when(telemetry.findOpenIncidents(PLOT)).thenReturn(List.of());
        var none = reader.read(ScreenKey.ALERT, new PlotId(PLOT));
        assertThat(none.incident()).isNull();
        assertThat(none.plotsAtRisk()).isZero();
    }

    @Test
    @DisplayName("CLIMATE: forecast, nodes and a reading of the last 24 hours; an older reading is not recent")
    void climateReadsSensors() {
        var climate = reader.read(ScreenKey.CLIMATE, new PlotId(PLOT));

        assertThat(climate.nodes()).hasSize(1);
        assertThat(climate.nodes().get(0).simulated()).isTrue();
        assertThat(climate.latestReading().soilMoisture()).isEqualTo(18.0);
        assertThat(climate.forecast()).isNotNull();
        verify(telemetry, never()).findOpenIncidents(any());

        when(telemetry.findPlotSensors(PLOT)).thenReturn(new PlotSensors(
                List.of(new LinkedNode("MICROCLIMATE", null, true, null, null, null)),
                new LatestReading(NOW.minus(Duration.ofHours(25)), 27.4, 38.0, 18.0)));
        assertThat(reader.read(ScreenKey.CLIMATE, new PlotId(PLOT)).latestReading()).isNull();
    }

    private static OpenIncident incident(String type, String severity, String triggeredAt) {
        return new OpenIncident("id-" + type, type, severity, 34.0, 32.0, "°C", Instant.parse(triggeredAt), 3, 1);
    }
}
