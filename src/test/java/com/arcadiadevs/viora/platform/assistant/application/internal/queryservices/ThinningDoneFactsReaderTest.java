package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningDoneFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningOutcomeData;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.ThinningOutcome;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("ThinningDoneFactsReader: the outcome of the confirmed thinning, through the Thinning facade")
class ThinningDoneFactsReaderTest {

    private static final String PLOT = "550e8400-e29b-41d4-a716-446655440001";

    private final ThinningContextFacade thinning = mock(ThinningContextFacade.class);
    private final ThinningDoneFactsReader reader = new ThinningDoneFactsReader(new ExternalThinningService(thinning));

    @Test
    @DisplayName("the outcome of the campaign, field by field")
    void readsTheOutcome() {
        when(thinning.findThinningOutcome(PLOT, 2026)).thenReturn(Optional.of(new ThinningOutcome(
                LocalDate.of(2026, 10, 28), 30.0, "OPTIMAL", 0.5714, 0.4, 0.4, "BALANCED", 11700.0, 8200.0,
                "FRUIT_COUNT_ONLY", 3, "SYNTHETIC_DEMO", 14, 49)));

        var facts = (ThinningDoneFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(reader.screen()).isEqualTo(ScreenKey.THINNING_DONE);
        assertThat(facts.screen()).isEqualTo(ScreenKey.THINNING_DONE);
        assertThat(facts.campaignYear()).isEqualTo(2026);
        assertThat(facts.outcome()).isEqualTo(new ThinningOutcomeData(LocalDate.of(2026, 10, 28), 30.0, "OPTIMAL",
                0.5714, 0.4, 0.4, "BALANCED", 11700.0, 8200.0, "FRUIT_COUNT_ONLY", 3, "SYNTHETIC_DEMO", 14, 49));
    }

    @Test
    @DisplayName("the figures Thinning could not compute stay null; only the day is required")
    void readsAPartialOutcome() {
        when(thinning.findThinningOutcome(PLOT, 2026)).thenReturn(Optional.of(new ThinningOutcome(
                LocalDate.of(2026, 10, 28), null, null, null, null, null, null, null, null, null, 0, null, null,
                null)));

        var facts = (ThinningDoneFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(facts.outcome()).isEqualTo(new ThinningOutcomeData(LocalDate.of(2026, 10, 28), null, null, null,
                null, null, null, null, null, null, 0, null, null, null));
    }

    @Test
    @DisplayName("before the thinning is confirmed, or without its day, the screen has no outcome")
    void withoutTheOutcome() {
        when(thinning.findThinningOutcome(PLOT, 2026)).thenReturn(Optional.empty());

        var facts = (ThinningDoneFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(facts.campaignYear()).isEqualTo(2026);
        assertThat(facts.outcome()).isNull();

        when(thinning.findThinningOutcome(PLOT, 2025)).thenReturn(Optional.of(new ThinningOutcome(null, 30.0,
                "OPTIMAL", 0.5714, 0.4, 0.4, "BALANCED", 11700.0, 8200.0, "FRUIT_COUNT_ONLY", 3, "SYNTHETIC_DEMO", 14,
                49)));

        assertThat(((ThinningDoneFacts) reader.read(new PlotId(PLOT), 2025)).outcome()).isNull();
    }

    @Test
    @DisplayName("a failure of the Thinning facade is not hidden as a missing thinning")
    void facadeFailure() {
        when(thinning.findThinningOutcome(PLOT, 2026)).thenThrow(new IllegalStateException("thinning unavailable"));

        assertThatThrownBy(() -> reader.read(new PlotId(PLOT), 2026))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("thinning unavailable");
    }
}
