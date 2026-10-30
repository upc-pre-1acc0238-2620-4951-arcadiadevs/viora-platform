package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssemblerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.PLOT;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.TODAY;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@DisplayName("WINTER_CHILL facts assemblers")
class ChillFactsAssemblersTest {

    private static final String PREFIX = "assistant.template.chill.";

    @Test
    @DisplayName("The standard registry answers the four chill questions")
    void registered() {
        var registry = FactsAssemblerRegistry.standard();
        assertThat(List.of(QuestionId.CHILL_MISSING, QuestionId.CHILL_REACH_30, QuestionId.CHILL_VS_LAST_WINTER,
                QuestionId.CHILL_IF_INCOMPLETE)).allSatisfy(id -> assertThat(registry.find(id))
                .hasValueSatisfying(assembler -> assertThat(assembler.questionId()).isEqualTo(id)));
    }

    @Test
    @DisplayName("chill.missing: portions so far against the requirement and what is missing (24/30, faltan 6)")
    void missing() {
        var pending = new ChillMissingFactsAssembler().assemble(data(inProgress(), lastWinter())).orElseThrow();
        assertThat(pending.facts()).containsExactly(entry("accumulatedPortions", 24.4),
                entry("evaluatedThrough", "2026-07-20"), entry("missingPortions", 5.6),
                entry("seasonState", "IN_PROGRESS"), entry("seasonYear", 2026), entry("thresholdPortions", 30.0));
        assertThat(pending.text()).isEqualTo(CatalogText.of(PREFIX + "missing.text.pending", 24.4, 30.0, 5.6,
                TextDate.of(LocalDate.of(2026, 7, 20))));
        assertTwoCards(pending);
        assertThat(pending.cards().get(0).value()).isEqualTo(CatalogText.of("assistant.template.value.portions",
                24.4, 30.0));
        assertThat(pending.sources()).containsExactly(CatalogText.of("assistant.template.source.chill",
                TextDate.of(LocalDate.of(2026, 7, 20))));
        assertThat(pending.actions()).isEmpty();

        var halted = new ChillMissingFactsAssembler().assemble(data(halted(), null)).orElseThrow();
        assertThat(halted.facts()).containsEntry("warmDayCelsius", 24.0).containsEntry("seasonState", "HALTED");
        assertThat(halted.text().key()).isEqualTo(PREFIX + "missing.text.halted");

        assertThat(textKey(new ChillMissingFactsAssembler(), completedInSeason()))
                .isEqualTo(PREFIX + "missing.text.completed");
        var closed = new ChillMissingFactsAssembler().assemble(data(closedShort(), null)).orElseThrow();
        assertThat(closed.text().key()).isEqualTo(PREFIX + "missing.text.closed.short");
        // Shown figures add up to the requirement: 27.5 + 2.5 = 30
        assertThat(closed.facts()).containsEntry("accumulatedPortions", 27.5).containsEntry("missingPortions", 2.5);
        var closedCompleted = new ChillMissingFactsAssembler().assemble(data(closedCompleted(), null)).orElseThrow();
        assertThat(closedCompleted.text().key()).isEqualTo(PREFIX + "missing.text.closed.completed");
        assertThat(closedCompleted.facts()).containsEntry("missingPortions", 0.0)
                .containsEntry("completionDate", "2026-08-04");
    }

    @Test
    @DisplayName("chill.reach_30: estimated date against the budbreak of August 31 (fecha estimada 4 ago vs 31 ago)")
    void reach() {
        var projected = new ChillReachFactsAssembler().assemble(data(inProgress(), lastWinter())).orElseThrow();
        assertThat(projected.facts()).containsExactly(entry("accumulatedPortions", 24.4),
                entry("budbreakOn", "2026-08-31"), entry("evaluatedThrough", "2026-07-20"), entry("marginDays", 27L),
                entry("projectedCompletionDate", "2026-08-04"), entry("projectionStatus", "PROJECTED"),
                entry("seasonYear", 2026), entry("thresholdPortions", 30.0));
        assertThat(projected.text().key()).isEqualTo(PREFIX + "reach_30.text.projected");
        assertTwoCards(projected);
        assertThat(projected.cards()).extracting(FactCardTemplate::value).containsExactly(
                CatalogText.of("assistant.template.card.date.value", TextDate.shortOf(LocalDate.of(2026, 8, 4))),
                CatalogText.of("assistant.template.card.date.value", TextDate.shortOf(LocalDate.of(2026, 8, 31))));

        var unreachable = new ChillReachFactsAssembler().assemble(data(notReachable(), null)).orElseThrow();
        assertThat(unreachable.text().key()).isEqualTo(PREFIX + "reach_30.text.not_reachable");
        assertThat(unreachable.facts()).containsEntry("missingPortions", 17.9).doesNotContainKey("marginDays");

        var reached = new ChillReachFactsAssembler().assemble(data(completedInSeason(), null)).orElseThrow();
        assertThat(reached.text().key()).isEqualTo(PREFIX + "reach_30.text.reached");
        assertThat(reached.facts()).containsEntry("completionDate", "2026-08-04").containsEntry("marginDays", 27L);
        assertThat(textKey(new ChillReachFactsAssembler(), closedCompleted()))
                .isEqualTo(PREFIX + "reach_30.text.reached");
        assertThat(textKey(new ChillReachFactsAssembler(), closedShort()))
                .isEqualTo(PREFIX + "reach_30.text.closed.short");

        // Fewer than 14 days evaluated: no date to give
        assertThat(new ChillReachFactsAssembler().assemble(data(insufficient(), null))).isEmpty();
    }

    @Test
    @DisplayName("chill.vs_last_winter: same date of last winter while in season, whole seasons once finished")
    void vsLastWinter() {
        var inSeason = new ChillVsLastWinterFactsAssembler().assemble(data(inProgress(), lastWinter()))
                .orElseThrow();
        assertThat(inSeason.facts()).containsExactly(entry("accumulatedPortions", 24.4),
                entry("evaluatedThrough", "2026-07-20"), entry("lastWinterCompletionDate", "2025-08-10"),
                entry("lastWinterPortions", 33.1), entry("lastWinterSameDatePortions", 18.0),
                entry("lastWinterYear", 2025), entry("seasonYear", 2026), entry("thresholdPortions", 30.0));
        assertThat(inSeason.text()).isEqualTo(CatalogText.of(PREFIX + "vs_last_winter.text.in_season",
                TextDate.of(LocalDate.of(2026, 7, 20)), 24.4, 18.0, CatalogText.of(PREFIX
                        + "vs_last_winter.closing.completed", 30.0, TextDate.of(LocalDate.of(2025, 8, 10)))));
        assertTwoCards(inSeason);

        var shortLast = new ChillVsLastWinterFactsAssembler().assemble(data(inProgress(), lastWinterShort()))
                .orElseThrow();
        assertThat(((CatalogText) shortLast.text().args().get(3)).key())
                .isEqualTo(PREFIX + "vs_last_winter.closing.short");
        assertThat(shortLast.facts()).doesNotContainKey("lastWinterCompletionDate");

        // Last winter without the same day: only its whole season
        var gap = season(2025, 33.1, "OFF_SEASON", "NOT_APPLICABLE", null, LocalDate.of(2025, 8, 10),
                LocalDate.of(2025, 7, 1), 5);
        var total = new ChillVsLastWinterFactsAssembler().assemble(data(inProgress(), gap)).orElseThrow();
        assertThat(total.text().key()).isEqualTo(PREFIX + "vs_last_winter.text.in_season.total");
        assertThat(total.facts()).doesNotContainKey("lastWinterSameDatePortions");

        var closed = new ChillVsLastWinterFactsAssembler().assemble(data(closedShort(), lastWinter())).orElseThrow();
        assertThat(closed.text().key()).isEqualTo(PREFIX + "vs_last_winter.text.closed");
        assertThat(closed.facts()).containsEntry("daysAbove24Celsius", 11).containsEntry("lastWinterDaysAbove24Celsius",
                5).containsEntry("warmDayCelsius", 24.0).containsEntry("lastWinterPortions", 33.1);

        assertThat(new ChillVsLastWinterFactsAssembler().assemble(data(inProgress(), null))).isEmpty();
    }

    @Test
    @DisplayName("chill.if_incomplete: what a short winter means, or that the requirement was already met")
    void ifIncomplete() {
        var pending = new ChillIfIncompleteFactsAssembler().assemble(data(inProgress(), null)).orElseThrow();
        assertThat(pending.facts()).containsExactly(entry("accumulatedPortions", 24.4),
                entry("budbreakOn", "2026-08-31"), entry("evaluatedThrough", "2026-07-20"),
                entry("missingPortions", 5.6), entry("projectionStatus", "PROJECTED"),
                entry("seasonState", "IN_PROGRESS"), entry("seasonYear", 2026), entry("thresholdPortions", 30.0));
        assertThat(pending.text().key()).isEqualTo(PREFIX + "if_incomplete.text.pending");
        assertTwoCards(pending);

        assertThat(textKey(new ChillIfIncompleteFactsAssembler(), notReachable()))
                .isEqualTo(PREFIX + "if_incomplete.text.at_risk");
        assertThat(textKey(new ChillIfIncompleteFactsAssembler(), insufficient()))
                .isEqualTo(PREFIX + "if_incomplete.text.pending");
        assertThat(textKey(new ChillIfIncompleteFactsAssembler(), completedInSeason()))
                .isEqualTo(PREFIX + "if_incomplete.text.completed");
        assertThat(textKey(new ChillIfIncompleteFactsAssembler(), closedShort()))
                .isEqualTo(PREFIX + "if_incomplete.text.closed.short");
        assertThat(textKey(new ChillIfIncompleteFactsAssembler(), closedCompleted()))
                .isEqualTo(PREFIX + "if_incomplete.text.closed.completed");
    }

    @Test
    @DisplayName("No chill question answers without an evaluated day, or from another screen's data")
    void nothingWithoutData() {
        var other = new PlotAnswerData(PLOT, TODAY, 2026, null, null, null, null);
        for (FactsAssembler assembler : List.of(new ChillMissingFactsAssembler(), new ChillReachFactsAssembler(),
                new ChillVsLastWinterFactsAssembler(), new ChillIfIncompleteFactsAssembler())) {
            assertThat(assembler.assemble(data(notEvaluated(), lastWinter()))).isEmpty();
            assertThat(assembler.assemble(PlotAnswerData.ofChill(PLOT, TODAY, 2026, ChillData.NONE))).isEmpty();
            assertThat(assembler.assemble(other)).isEmpty();
        }
    }

    private static String textKey(FactsAssembler assembler, ChillData.Season current) {
        return assembler.assemble(data(current, lastWinter())).orElseThrow().text().key();
    }

    private static void assertTwoCards(AssembledFacts facts) {
        assertThat(facts.cards()).extracting(FactCardTemplate::tone)
                .containsExactly(FactTone.PRIMARY, FactTone.HIGHLIGHT);
    }
}
