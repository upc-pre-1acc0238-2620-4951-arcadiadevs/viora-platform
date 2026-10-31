package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotBearingIndex;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ClimateThresholds;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ForecastDayOutlook;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.ForecastOutlook;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade.OpenIncident;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.ThinningPlanDetail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("HomeFactsReader: the figures of the home screen, through the Orchard, Thinning, Phenology and Telemetry "
        + "facades")
class HomeFactsReaderTest {

    /** 03:00 UTC on 31 October is still 30 October in Lima. */
    private static final Instant NOW = Instant.parse("2026-10-31T03:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);
    private static final String PLOT = "550e8400-e29b-41d4-a716-446655440001";
    private static final String OTHER_PLOT = "550e8400-e29b-41d4-a716-446655440002";
    private static final String PRODUCER = "producer-1";

    private final OrchardContextFacade orchard = mock(OrchardContextFacade.class);
    private final ThinningContextFacade thinning = mock(ThinningContextFacade.class);
    private final PhenologyContextFacade phenology = mock(PhenologyContextFacade.class);
    private final TelemetryContextFacade telemetry = mock(TelemetryContextFacade.class);
    private HomeFactsReader reader;

    @BeforeEach
    void setUp() {
        reader = new HomeFactsReader(new ExternalOrchardService(orchard), new ExternalThinningService(thinning), new ExternalPhenologyService(phenology),
                new ExternalTelemetryService(telemetry), Clock.fixed(NOW, ZoneOffset.UTC));
        when(telemetry.findClimateThresholds()).thenReturn(new ClimateThresholds(32.0, 36.0, 1.5));
    }

    @Test
    @DisplayName("one plot: its plan, the trees missing, harvests, index with band, alerts and forecast")
    void readsEveryFigureOfAPlot() {
        stubPlot(PLOT);

        var facts = (HomeFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(reader.screen()).isEqualTo(ScreenKey.HOME);
        assertThat(facts.screen()).isEqualTo(ScreenKey.HOME);
        assertThat(facts.today()).isEqualTo(TODAY);
        assertThat(facts.campaignYear()).isEqualTo(2026);
        assertThat(facts.fieldScope()).isFalse();
        assertThat(facts.thresholds()).isEqualTo(new ClimateData.Thresholds(32.0, 36.0, 1.5));
        assertThat(facts.plots()).hasSize(1);
        var plot = facts.plots().getFirst();
        assertThat(plot.plan()).isEqualTo(new ThinningPlanData(true, 30.0, 0.5714, 0.4, LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 11, 10), 5, LocalDate.of(2026, 10, 29), null, null, null, null, null));
        assertThat(plot.treesNeeded()).isEqualTo(2);
        assertThat(plot.yields()).containsExactly(Map.entry(2024, 1000.0), Map.entry(2025, 3000.0));
        assertThat(plot.bearingIndex()).isEqualTo(0.5123);
        assertThat(plot.bearingCategory()).isEqualTo("MODERATE_ALTERNATION");
        assertThat(plot.openAlerts()).containsExactly(new ClimateData.Incident("HEAT_WAVE", "WARNING", 34.2, 32.0,
                "°C", LocalDate.of(2026, 10, 28), 3, 1));
        assertThat(plot.forecast().days()).containsExactly(new ClimateData.ForecastDay(TODAY, 34.2, 14.0, true,
                false));
    }

    @Test
    @DisplayName("a plot never sampled, without index, harvests, alerts or forecast reads as missing figures")
    void readsMissingFigures() {
        when(thinning.findThinningPlanDetail(PLOT, 2026)).thenReturn(Optional.empty());
        when(thinning.findPrescriptionAvailability(PLOT, 2026)).thenReturn(Optional.empty());
        when(phenology.findBiennialBearingIndex(PLOT)).thenReturn(Optional.empty());
        when(phenology.findHistoricalYields(PLOT)).thenReturn(new TreeMap<>());
        when(telemetry.findOpenIncidents(PLOT)).thenReturn(List.of());
        when(telemetry.findForecastOutlook(PLOT)).thenReturn(Optional.empty());

        var plot = ((HomeFacts) reader.read(new PlotId(PLOT), 2026)).plots().getFirst();

        assertThat(plot.plan()).isNull();
        assertThat(plot.treesNeeded()).isZero();
        assertThat(plot.yields()).isEmpty();
        assertThat(plot.bearingIndex()).isNull();
        assertThat(plot.bearingCategory()).isNull();
        assertThat(plot.openAlerts()).isEmpty();
        assertThat(plot.forecast()).isNull();
    }

    @Test
    @DisplayName("a plot named by the request is read alone, without listing the producer's plots")
    void readsThePlotAsked() {
        stubPlot(PLOT);

        var facts = (HomeFacts) reader.read(PRODUCER, new PlotId(PLOT), 2026);

        assertThat(facts.fieldScope()).isFalse();
        assertThat(facts.plots()).hasSize(1);
        verifyNoInteractions(orchard);
        assertThatThrownBy(() -> reader.read(null, 2026))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("assistant.plot.required");
    }

    @Test
    @DisplayName("without plot: every active plot of the producer asking, in the order Orchard lists them")
    void readsTheWholeField() {
        when(orchard.findActivePlotIdsByProducerId(PRODUCER)).thenReturn(List.of(PLOT, OTHER_PLOT));
        stubPlot(PLOT);
        when(thinning.findThinningPlanDetail(OTHER_PLOT, 2026)).thenReturn(Optional.empty());
        when(thinning.findPrescriptionAvailability(OTHER_PLOT, 2026)).thenReturn(Optional.empty());
        when(phenology.findBiennialBearingIndex(OTHER_PLOT)).thenReturn(Optional.empty());
        when(phenology.findHistoricalYields(OTHER_PLOT)).thenReturn(new TreeMap<>(Map.of(2025, 2000.0)));
        when(telemetry.findOpenIncidents(OTHER_PLOT)).thenReturn(List.of());
        when(telemetry.findForecastOutlook(OTHER_PLOT)).thenReturn(Optional.empty());

        var facts = (HomeFacts) reader.read(PRODUCER, null, 2026);

        assertThat(facts.fieldScope()).isTrue();
        assertThat(facts.today()).isEqualTo(TODAY);
        assertThat(facts.plots()).hasSize(2);
        assertThat(facts.plots().get(0).bearingIndex()).isEqualTo(0.5123);
        assertThat(facts.plots().get(1).plan()).isNull();
        assertThat(facts.plots().get(1).yields()).containsExactly(Map.entry(2025, 2000.0));
        assertThat(facts.activePlots()).isEqualTo(2);
        assertThat(facts.plotsWithAlerts()).isEqualTo(1);
    }

    @Test
    @DisplayName("without plot and without active plots: an empty field")
    void readsAnEmptyField() {
        when(orchard.findActivePlotIdsByProducerId(PRODUCER)).thenReturn(List.of());

        var facts = (HomeFacts) reader.read(PRODUCER, null, 2026);

        assertThat(facts.fieldScope()).isTrue();
        assertThat(facts.plots()).isEmpty();
        verifyNoInteractions(thinning, phenology);
    }

    @Test
    @DisplayName("a facade failure is not swallowed")
    void facadeFailure() {
        when(thinning.findThinningPlanDetail(PLOT, 2026)).thenReturn(Optional.empty());
        when(thinning.findPrescriptionAvailability(PLOT, 2026)).thenReturn(Optional.empty());
        when(phenology.findBiennialBearingIndex(PLOT)).thenReturn(Optional.empty());
        when(phenology.findHistoricalYields(PLOT)).thenReturn(new TreeMap<>());
        when(telemetry.findOpenIncidents(PLOT)).thenThrow(new IllegalStateException("telemetry down"));

        assertThatThrownBy(() -> reader.read(new PlotId(PLOT), 2026))
                .isInstanceOf(IllegalStateException.class).hasMessage("telemetry down");
    }

    private void stubPlot(String plotId) {
        when(thinning.findThinningPlanDetail(plotId, 2026)).thenReturn(Optional.of(new ThinningPlanDetail(true, 30.0,
                0.5714, 0.4, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 10), 5, LocalDate.of(2026, 10, 29),
                null, null, null, null, null)));
        when(thinning.findPrescriptionAvailability(plotId, 2026))
                .thenReturn(Optional.of(new PrescriptionAvailability(true, 30.0, 5, 2, null)));
        when(phenology.findBiennialBearingIndex(plotId))
                .thenReturn(Optional.of(new PlotBearingIndex(0.5123, "MODERATE_ALTERNATION")));
        when(phenology.findHistoricalYields(plotId)).thenReturn(new TreeMap<>(Map.of(2024, 1000.0, 2025, 3000.0)));
        when(telemetry.findOpenIncidents(plotId)).thenReturn(List.of(
                new OpenIncident("i-1", "HEAT_WAVE", "WARNING", 34.2, 32.0, "°C",
                        Instant.parse("2026-10-29T02:00:00Z"), 3, 1),
                new OpenIncident("i-2", "SENSOR_OFFLINE", "CRITICAL", 0, 0, "", NOW, 0, 0)));
        when(telemetry.findForecastOutlook(plotId)).thenReturn(Optional.of(new ForecastOutlook(NOW,
                List.of(new ForecastDayOutlook(TODAY, 34.2, 14.0, true, false)))));
    }
}
