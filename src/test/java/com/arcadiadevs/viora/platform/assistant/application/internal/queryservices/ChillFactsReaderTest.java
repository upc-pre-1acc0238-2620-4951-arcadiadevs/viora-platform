package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade.PlotChillSeason;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ChillFactsReader: the winter chill of a plot, through the Phenology facade")
class ChillFactsReaderTest {

    private static final String PLOT = "550e8400-e29b-41d4-a716-446655440001";

    private final PhenologyContextFacade phenology = mock(PhenologyContextFacade.class);
    private final ChillFactsReader reader = new ChillFactsReader(new ExternalPhenologyService(phenology));

    @Test
    @DisplayName("the season the chill metric describes and the one before it, field by field")
    void readsBothWinters() {
        when(phenology.currentChillSeasonYear()).thenReturn(2026);
        var daily = new TreeMap<LocalDate, Double>();
        daily.put(LocalDate.of(2026, 7, 19), 24.0);
        daily.put(LocalDate.of(2026, 7, 20), 24.43);
        when(phenology.findChillSeason(PLOT, 2026)).thenReturn(Optional.of(new PlotChillSeason(2026, 24.43, 30.0,
                "IN_PROGRESS", "PROJECTED", LocalDate.of(2026, 8, 4), null, LocalDate.of(2026, 8, 31),
                LocalDate.of(2026, 7, 20), 2, 24.0, daily)));
        when(phenology.findChillSeason(PLOT, 2025)).thenReturn(Optional.of(new PlotChillSeason(2025, 33.1, 30.0,
                "OFF_SEASON", "NOT_APPLICABLE", null, LocalDate.of(2025, 8, 10), LocalDate.of(2025, 8, 31),
                LocalDate.of(2025, 8, 31), 5, 24.0, null)));

        var facts = (ChillFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(reader.screen()).isEqualTo(ScreenKey.WINTER_CHILL);
        assertThat(facts.current()).isEqualTo(new ChillFacts.Season(2026, 24.43, 30.0, "IN_PROGRESS", "PROJECTED",
                LocalDate.of(2026, 8, 4), null, LocalDate.of(2026, 8, 31), LocalDate.of(2026, 7, 20), 2, 24.0, daily));
        assertThat(facts.lastWinter().seasonYear()).isEqualTo(2025);
        assertThat(facts.lastWinter().completionDate()).isEqualTo(LocalDate.of(2025, 8, 10));
        assertThat(facts.lastWinter().dailyPortions()).isEmpty();
        assertThat(facts.evaluated()).isTrue();
        assertThat(facts.lastWinterEvaluated()).isTrue();
    }

    @Test
    @DisplayName("without the current winter last winter is not read, and the screen has no chill figures")
    void withoutTheCurrentWinter() {
        when(phenology.currentChillSeasonYear()).thenReturn(2026);
        when(phenology.findChillSeason(PLOT, 2026)).thenReturn(Optional.empty());

        var facts = (ChillFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(facts.current()).isNull();
        assertThat(facts.lastWinter()).isNull();
        assertThat(facts.evaluated()).isFalse();
        verify(phenology, never()).findChillSeason(PLOT, 2025);
    }
}
