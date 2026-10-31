package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotDetailFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
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
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PlotDetailFactsReader: the figures of the plot detail screen, through the Thinning, Phenology and "
        + "Telemetry facades")
class PlotDetailFactsReaderTest {

    /** 03:00 UTC on 31 October is still 30 October in Lima. */
    private static final Instant NOW = Instant.parse("2026-10-31T03:00:00Z");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 30);
    private static final String PLOT = "550e8400-e29b-41d4-a716-446655440001";

    private final ThinningContextFacade thinning = mock(ThinningContextFacade.class);
    private final PhenologyContextFacade phenology = mock(PhenologyContextFacade.class);
    private final TelemetryContextFacade telemetry = mock(TelemetryContextFacade.class);
    private PlotDetailFactsReader reader;

    @BeforeEach
    void setUp() {
        reader = new PlotDetailFactsReader(new ExternalThinningService(thinning), new ExternalPhenologyService(phenology),
                new ExternalTelemetryService(telemetry), Clock.fixed(NOW, ZoneOffset.UTC));
        when(telemetry.findClimateThresholds()).thenReturn(new ClimateThresholds(32.0, 36.0, 1.5));
    }

    @Test
    @DisplayName("the plan, the trees missing, the index with its band, the harvests, the alerts and the forecast")
    void readsEveryFigure() {
        when(thinning.findThinningPlanDetail(PLOT, 2026)).thenReturn(Optional.of(new ThinningPlanDetail(true, 30.0,
                0.5714, 0.4, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 10), 5, LocalDate.of(2026, 10, 29),
                null, null, null, null, null)));
        when(thinning.findPrescriptionAvailability(PLOT, 2026))
                .thenReturn(Optional.of(new PrescriptionAvailability(true, 30.0, 5, 0, null)));
        when(phenology.findBiennialBearingIndex(PLOT))
                .thenReturn(Optional.of(new PlotBearingIndex(0.5123, "MODERATE_ALTERNATION")));
        when(phenology.findHistoricalYields(PLOT)).thenReturn(new TreeMap<>(java.util.Map.of(2024, 1000.0,
                2025, 3000.0)));
        when(telemetry.findOpenIncidents(PLOT)).thenReturn(List.of(
                new OpenIncident("i-1", "HEAT_WAVE", "WARNING", 34.2, 32.0, "°C",
                        Instant.parse("2026-10-29T02:00:00Z"), 3, 1),
                new OpenIncident("i-2", "SENSOR_OFFLINE", "CRITICAL", 0, 0, "", NOW, 0, 0)));
        when(telemetry.findForecastOutlook(PLOT)).thenReturn(Optional.of(new ForecastOutlook(NOW,
                List.of(new ForecastDayOutlook(TODAY, 34.2, 14.0, true, false)))));

        var facts = (PlotDetailFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(reader.screen()).isEqualTo(ScreenKey.PLOT_DETAIL);
        assertThat(facts.screen()).isEqualTo(ScreenKey.PLOT_DETAIL);
        assertThat(facts.today()).isEqualTo(TODAY);
        assertThat(facts.campaignYear()).isEqualTo(2026);
        assertThat(facts.plan()).isEqualTo(new ThinningPlanData(true, 30.0, 0.5714, 0.4, LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 11, 10), 5, LocalDate.of(2026, 10, 29), null, null, null, null, null));
        assertThat(facts.sampled()).isTrue();
        assertThat(facts.treesNeeded()).isZero();
        assertThat(facts.bearingIndex()).isEqualTo(0.5123);
        assertThat(facts.bearingCategory()).isEqualTo("MODERATE_ALTERNATION");
        assertThat(facts.harvestCampaigns()).isEqualTo(2);
        assertThat(facts.openAlerts()).containsExactly(new ClimateData.Incident("HEAT_WAVE", "WARNING", 34.2, 32.0,
                "°C", LocalDate.of(2026, 10, 28), 3, 1));
        assertThat(facts.thresholds()).isEqualTo(new ClimateData.Thresholds(32.0, 36.0, 1.5));
        assertThat(facts.forecast().syncedOn()).isEqualTo(TODAY);
        assertThat(facts.forecast().days()).containsExactly(
                new ClimateData.ForecastDay(TODAY, 34.2, 14.0, true, false));
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

        var facts = (PlotDetailFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(facts.plan()).isNull();
        assertThat(facts.sampled()).isFalse();
        assertThat(facts.treesNeeded()).isZero();
        assertThat(facts.bearingIndex()).isNull();
        assertThat(facts.bearingCategory()).isNull();
        assertThat(facts.harvestCampaigns()).isZero();
        assertThat(facts.openAlerts()).isEmpty();
        assertThat(facts.forecast()).isNull();
        assertThat(facts.today()).isEqualTo(TODAY);
    }

    @Test
    @DisplayName("a sampling halfway carries the trees still missing from the prescription availability")
    void readsTreesNeeded() {
        when(thinning.findThinningPlanDetail(PLOT, 2025)).thenReturn(Optional.of(new ThinningPlanDetail(false, null,
                null, null, null, null, 2, null, null, null, null, null, null)));
        when(thinning.findPrescriptionAvailability(PLOT, 2025))
                .thenReturn(Optional.of(new PrescriptionAvailability(false, null, 2, 3, null)));
        when(phenology.findBiennialBearingIndex(PLOT)).thenReturn(Optional.empty());
        when(phenology.findHistoricalYields(PLOT)).thenReturn(new TreeMap<>());

        var facts = (PlotDetailFacts) reader.read(new PlotId(PLOT), 2025);

        assertThat(facts.campaignYear()).isEqualTo(2025);
        assertThat(facts.plan().sampledTrees()).isEqualTo(2);
        assertThat(facts.sampled()).isTrue();
        assertThat(facts.treesNeeded()).isEqualTo(3);
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
}
