package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AlertFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ClimateThresholds;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ForecastDayOutlook;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ForecastOutlook;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.OpenIncident;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("AlertFactsReader: the figures of the alert screen, through the Telemetry and Orchard facades")
class AlertFactsReaderTest {

    private static final Instant NOW = Instant.parse("2026-10-30T15:00:00Z");
    private static final String PLOT = "550e8400-e29b-41d4-a716-446655440001";
    private static final String PRODUCER = "6f1c2a10-0000-4000-8000-000000000001";

    private final TelemetryContextFacade telemetry = mock(TelemetryContextFacade.class);
    private final OrchardContextFacade orchard = mock(OrchardContextFacade.class);
    private AlertFactsReader reader;

    @BeforeEach
    void setUp() {
        reader = new AlertFactsReader(new ExternalTelemetryService(telemetry), new ExternalOrchardService(orchard));
        when(telemetry.findClimateThresholds()).thenReturn(new ClimateThresholds(32.0, 36.0, 1.5));
        when(telemetry.findForecastOutlook(PLOT)).thenReturn(Optional.of(new ForecastOutlook(NOW,
                List.of(new ForecastDayOutlook(LocalDate.of(2026, 10, 30), 34.0, 14.0, true, false)))));
    }

    @Test
    @DisplayName("the most severe open alert, the thresholds of Telemetry and the owner's plots only")
    void readsTheMostSevereIncident() {
        when(telemetry.findOpenIncidents(PLOT)).thenReturn(List.of(
                incident("HEAT_WAVE", "WARNING", "2026-10-29T20:00:00Z"),
                incident("FROST_WARNING", "CRITICAL", "2026-10-28T10:00:00Z"),
                incident("WARM_WINTER", "CRITICAL", "2026-10-30T10:00:00Z")));
        when(orchard.findPlotProducerId(PLOT)).thenReturn(Optional.of(PRODUCER));
        when(orchard.findActivePlotIdsByProducerId(PRODUCER)).thenReturn(List.of(PLOT, "other"));
        when(telemetry.countPlotsWithOpenIncident(List.of(PLOT, "other"), "FROST_WARNING")).thenReturn(2);

        var facts = (AlertFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(reader.screen()).isEqualTo(ScreenKey.ALERT);
        assertThat(facts.incident().type()).isEqualTo("FROST_WARNING");
        assertThat(facts.incident().threshold()).isEqualTo(32.0);
        assertThat(facts.incident().steps()).isEqualTo(3);
        assertThat(facts.incident().completedSteps()).isEqualTo(1);
        // Raised at 05:00 in Lima on the 28th
        assertThat(facts.incident().triggeredOn()).isEqualTo(LocalDate.of(2026, 10, 28));
        assertThat(facts.plotsAtRisk()).isEqualTo(2);
        assertThat(facts.activePlots()).isEqualTo(2);
        assertThat(facts.thresholds().heatWarning()).isEqualTo(32.0);
        assertThat(facts.thresholds().frostWarning()).isEqualTo(1.5);
        assertThat(facts.forecast().days()).hasSize(1);
        assertThat(facts.forecast().syncedOn()).isEqualTo(LocalDate.of(2026, 10, 30));
        verify(telemetry, never()).findPlotSensors(anyString());
    }

    @Test
    @DisplayName("a hydric alert reads no forecast, and without an alert no plot is counted")
    void readsOnlyWhatTheQuestionsUse() {
        when(telemetry.findOpenIncidents(PLOT)).thenReturn(List.of(incident("HYDRIC_STRESS", "WARNING",
                "2026-10-29T20:00:00Z")));
        when(orchard.findPlotProducerId(PLOT)).thenReturn(Optional.of(PRODUCER));
        when(orchard.findActivePlotIdsByProducerId(PRODUCER)).thenReturn(List.of(PLOT));
        when(telemetry.countPlotsWithOpenIncident(List.of(PLOT), "HYDRIC_STRESS")).thenReturn(1);

        assertThat(((AlertFacts) reader.read(new PlotId(PLOT), 2026)).forecast()).isNull();
        verify(telemetry, never()).findForecastOutlook(anyString());

        clearInvocations(telemetry);
        when(telemetry.findOpenIncidents(PLOT)).thenReturn(List.of());
        var none = (AlertFacts) reader.read(new PlotId(PLOT), 2026);
        assertThat(none.incident()).isNull();
        assertThat(none.plotsAtRisk()).isZero();
        assertThat(none.thresholds()).isNotNull();
        verify(telemetry, never()).countPlotsWithOpenIncident(any(), anyString());
    }

    private static OpenIncident incident(String type, String severity, String triggeredAt) {
        return new OpenIncident("id-" + type, type, severity, 34.0, 32.0, "°C", Instant.parse(triggeredAt), 3, 1);
    }
}
