package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.SamplingFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.ThinningPlanDetail;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("SamplingFactsReader: the sampling and thinning plan of a plot, through the Thinning facade")
class SamplingFactsReaderTest {

    private static final String PLOT = "550e8400-e29b-41d4-a716-446655440001";
    /** 03:00 UTC on November 1 is still October 31 for the producers (UTC-5). */
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T03:00:00Z"), ZoneOffset.UTC);

    private final ThinningContextFacade thinning = mock(ThinningContextFacade.class);
    private final SamplingFactsReader reader = new SamplingFactsReader(new ExternalThinningService(thinning), CLOCK);

    @Test
    @DisplayName("the plan field by field, the trees still missing and the day in the producers' time zone")
    void readsThePlan() {
        when(thinning.findThinningPlanDetail(PLOT, 2026)).thenReturn(Optional.of(new ThinningPlanDetail(true, 32.0,
                0.59, 0.4, LocalDate.of(2026, 10, 21), LocalDate.of(2026, 11, 11), 4, LocalDate.of(2026, 10, 30), null,
                null, null, null, null)));
        when(thinning.findPrescriptionAvailability(PLOT, 2026))
                .thenReturn(Optional.of(new PrescriptionAvailability(true, 32.0, 4, 1, null)));

        var facts = (SamplingFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(reader.screen()).isEqualTo(ScreenKey.SAMPLING_SUMMARY);
        assertThat(facts.screen()).isEqualTo(ScreenKey.SAMPLING_SUMMARY);
        assertThat(facts.today()).isEqualTo(LocalDate.of(2026, 10, 31));
        assertThat(facts.plan()).isEqualTo(new ThinningPlanData(true, 32.0, 0.59, 0.4, LocalDate.of(2026, 10, 21),
                LocalDate.of(2026, 11, 11), 4, LocalDate.of(2026, 10, 30), null, null, null, null, null));
        assertThat(facts.treesNeeded()).isEqualTo(1);
        assertThat(facts.sampled()).isTrue();
    }

    @Test
    @DisplayName("a plot never sampled this campaign has no plan, nothing missing to report and is not sampled")
    void withoutSampling() {
        when(thinning.findThinningPlanDetail(PLOT, 2026)).thenReturn(Optional.empty());
        when(thinning.findPrescriptionAvailability(PLOT, 2026)).thenReturn(Optional.empty());

        var facts = (SamplingFacts) reader.read(new PlotId(PLOT), 2026);

        assertThat(facts.plan()).isNull();
        assertThat(facts.treesNeeded()).isZero();
        assertThat(facts.sampled()).isFalse();
        assertThat(facts.questions()).noneMatch(question -> question.enabled());
    }

    @Test
    @DisplayName("a failure of the Thinning facade reaches the caller, no figures are made up")
    void facadeFailure() {
        when(thinning.findThinningPlanDetail(PLOT, 2026)).thenThrow(new IllegalStateException("thinning down"));

        assertThatThrownBy(() -> reader.read(new PlotId(PLOT), 2026))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("thinning down");
    }
}
