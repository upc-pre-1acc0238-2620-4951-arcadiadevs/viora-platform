package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CampaignClosedFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningOutcomeData;
import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotBearingIndex;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.ThinningOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CampaignClosedFactsReader: the closed campaign, through the Phenology, Thinning and Orchard facades")
class CampaignClosedFactsReaderTest {

    private static final String PLOT = "550e8400-e29b-41d4-a716-446655440001";
    private static final LocalDate EXECUTED = LocalDate.of(2025, 11, 3);

    private final PhenologyContextFacade phenology = mock(PhenologyContextFacade.class);
    private final ThinningContextFacade thinning = mock(ThinningContextFacade.class);
    private final OrchardContextFacade orchard = mock(OrchardContextFacade.class);
    private final CampaignClosedFactsReader reader = new CampaignClosedFactsReader(
            new ExternalPhenologyService(phenology), new ExternalThinningService(thinning),
            new ExternalOrchardService(orchard));

    @Test
    @DisplayName("the harvests, the bearing index, the area and the thinning of the latest harvested campaign")
    void readsTheClosedCampaign() {
        when(phenology.findHistoricalYields(PLOT)).thenReturn(yields());
        when(phenology.findBiennialBearingIndex(PLOT)).thenReturn(Optional.of(
                new PlotBearingIndex(0.5123, "MODERATE_ALTERNATION")));
        // The closed campaign is 2025, the latest harvest, even though the calendar campaign is 2026
        when(thinning.findThinningOutcome(PLOT, 2025)).thenReturn(Optional.of(new ThinningOutcome(EXECUTED, 30.0,
                "OPTIMAL", 0.57, 0.4, 0.4, "BALANCED", 9100.0, 8200.0, "CALIBER_ADJUSTED", 3, "PROVISIONAL", 14,
                49)));
        when(orchard.findPlotAreaHectares(PLOT)).thenReturn(Optional.of(1.5));

        var facts = (CampaignClosedFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(reader.screen()).isEqualTo(ScreenKey.CAMPAIGN_CLOSED);
        assertThat(facts.screen()).isEqualTo(ScreenKey.CAMPAIGN_CLOSED);
        assertThat(facts.yieldsKg()).isEqualTo(yields());
        assertThat(facts.closedCampaignYear()).contains(2025);
        assertThat(facts.bearingIndex()).isEqualTo(0.5123);
        assertThat(facts.bearingCategory()).isEqualTo("MODERATE_ALTERNATION");
        assertThat(facts.outcome()).isEqualTo(new ThinningOutcomeData(EXECUTED, 30.0, "OPTIMAL", 0.57, 0.4, 0.4,
                "BALANCED", 9100.0, 8200.0, "CALIBER_ADJUSTED", 3, "PROVISIONAL", 14, 49));
        assertThat(facts.areaHectares()).isEqualTo(1.5);
        verify(thinning, never()).findThinningOutcome(PLOT, 2026);
    }

    @Test
    @DisplayName("harvests without index, thinning nor area keep those figures missing")
    void missingFigures() {
        when(phenology.findHistoricalYields(PLOT)).thenReturn(yields());
        when(phenology.findBiennialBearingIndex(PLOT)).thenReturn(Optional.empty());
        when(thinning.findThinningOutcome(PLOT, 2025)).thenReturn(Optional.empty());
        when(orchard.findPlotAreaHectares(PLOT)).thenReturn(Optional.empty());

        var facts = (CampaignClosedFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(facts.yieldsKg()).hasSize(4);
        assertThat(facts.bearingIndex()).isNull();
        assertThat(facts.bearingCategory()).isNull();
        assertThat(facts.outcome()).isNull();
        assertThat(facts.areaHectares()).isNull();
    }

    @Test
    @DisplayName("a thinning outcome without its execution day is not a confirmed thinning")
    void undatedOutcome() {
        when(phenology.findHistoricalYields(PLOT)).thenReturn(yields());
        when(phenology.findBiennialBearingIndex(PLOT)).thenReturn(Optional.empty());
        when(thinning.findThinningOutcome(PLOT, 2025)).thenReturn(Optional.of(new ThinningOutcome(null, null, null,
                null, null, null, null, null, null, null, 0, null, null, null)));
        when(orchard.findPlotAreaHectares(PLOT)).thenReturn(Optional.of(1.5));

        assertThat(((CampaignClosedFacts) reader.read(new PlotId(PLOT), 2026)).outcome()).isNull();
    }

    @Test
    @DisplayName("without a harvest nothing else is read and the screen has no closed campaign")
    void withoutHarvest() {
        when(phenology.findHistoricalYields(PLOT)).thenReturn(new TreeMap<>());

        var facts = (CampaignClosedFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(facts.yieldsKg()).isEmpty();
        assertThat(facts.closedCampaignYear()).isEmpty();
        assertThat(facts.bearingIndex()).isNull();
        assertThat(facts.outcome()).isNull();
        assertThat(facts.areaHectares()).isNull();
        verify(phenology, never()).findBiennialBearingIndex(anyString());
        verify(thinning, never()).findThinningOutcome(anyString(), anyInt());
        verify(orchard, never()).findPlotAreaHectares(anyString());
    }

    @Test
    @DisplayName("a facade failure is not swallowed: the reader does not invent figures")
    void facadeFailure() {
        when(phenology.findHistoricalYields(PLOT)).thenReturn(yields());
        when(phenology.findBiennialBearingIndex(PLOT)).thenReturn(Optional.empty());
        when(thinning.findThinningOutcome(PLOT, 2025)).thenThrow(new IllegalStateException("thinning down"));

        assertThatThrownBy(() -> reader.read(new PlotId(PLOT), 2026))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("thinning down");
        verify(orchard, never()).findPlotAreaHectares(anyString());
    }

    private static TreeMap<Integer, Double> yields() {
        var yields = new TreeMap<Integer, Double>();
        yields.put(2022, 8700.0);
        yields.put(2023, 9150.0);
        yields.put(2024, 9600.0);
        yields.put(2025, 12450.0);
        return yields;
    }
}
