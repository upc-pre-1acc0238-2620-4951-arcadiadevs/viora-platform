package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssemblerRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.issuedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.plan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.heat;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.hotThenMild;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

@DisplayName("home.daily_summary: the week plan with three cards (window days, week maximum, open alerts)")
class DailySummaryFactsAssemblerTest {

    private static final String SHARED = "assistant.template.";
    private final DailySummaryFactsAssembler summary = new DailySummaryFactsAssembler();
    private final HomeWeekPlanFactsAssembler weekPlan = new HomeWeekPlanFactsAssembler();

    @Test
    @DisplayName("Is registered for home.daily_summary")
    void registered() {
        assertThat(FactsAssemblerRegistry.standard().find(QuestionId.HOME_DAILY_SUMMARY))
                .map(FactsAssembler::questionId).contains(QuestionId.HOME_DAILY_SUMMARY);
    }

    @Test
    @DisplayName("Open window of a plot: days left, then the week maximum and the alerts of the week plan")
    void openWindowOfAPlot() {
        var data = onePlot(full(issuedPlan(), 0, List.of(heat()), null, hotThenMild()));
        var week = weekPlan.assemble(data).orElseThrow();

        var facts = summary.assemble(data).orElseThrow();

        assertThat(summary.questionId()).isEqualTo(QuestionId.HOME_DAILY_SUMMARY);
        assertThat(facts.facts()).containsAllEntriesOf(week.facts()).containsEntry("daysLeft", 12L)
                .doesNotContainKey("daysUntilOpen").hasSize(week.facts().size() + 1);
        assertThat(facts.text()).isEqualTo(week.text());
        assertThat(facts.sources()).isEqualTo(week.sources());
        assertThat(facts.actions()).isEmpty();
        assertThat(facts.cards()).containsExactly(
                new FactCardTemplate(CatalogText.of(SHARED + "value.integer", 12L),
                        CatalogText.of(SHARED + "card.days_left.label", 12L),
                        CatalogText.of(SHARED + "card.closes.pill", TextDate.shortOf(LocalDate.of(2026, 11, 10))),
                        FactTone.PRIMARY),
                week.cards().get(0), week.cards().get(1));
        assertThat(facts.cards().get(1).label().key()).isEqualTo(SHARED + "card.week_max.label");
        assertThat(facts.cards().get(2).label().key()).isEqualTo(SHARED + "card.open_alerts.label");
    }

    @Test
    @DisplayName("Field with a window opening this week: days until it opens")
    void windowOpeningInTheField() {
        var facts = summary.assemble(field(withPlan(plan(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20),
                null), 0), bare())).orElseThrow();

        assertThat(facts.facts()).contains(entry("scope", "FIELD"), entry("activePlots", 2),
                entry("plotsWindowOpening", 1), entry("windowOpensOn", "2026-11-02"), entry("daysUntilOpen", 3L));
        assertThat(facts.facts()).doesNotContainKey("daysLeft");
        assertThat(facts.cards().getFirst()).isEqualTo(new FactCardTemplate(
                CatalogText.of(SHARED + "value.integer", 3L), CatalogText.of(SHARED + "card.days_to_open.label", 3L),
                CatalogText.of(SHARED + "card.opens.pill", TextDate.shortOf(LocalDate.of(2026, 11, 2))),
                FactTone.PRIMARY));
        assertThat(facts.cards().get(2).pill()).isEqualTo(CatalogText.of(SHARED + "card.in_plots.pill", 0));
    }

    @Test
    @DisplayName("No window open or opening this week: the window card says so, without a number")
    void noWindow() {
        var facts = summary.assemble(field(bare(), withPlan(halfway(), 2))).orElseThrow();

        assertThat(facts.facts()).doesNotContainKeys("daysLeft", "daysUntilOpen");
        assertThat(facts.cards().getFirst()).isEqualTo(new FactCardTemplate(CatalogText.of(SHARED
                + "card.no_window.value"), CatalogText.of(SHARED + "card.window.label"),
                CatalogText.of(SHARED + "card.next_days.pill"), FactTone.PRIMARY));
        assertThat(facts.cards()).hasSize(3);
    }

    @Test
    @DisplayName("Nothing to summarize without active plots, nor from the data of another screen")
    void nothingToSummarize() {
        assertThat(summary.assemble(field())).isEmpty();
        assertThat(summary.assemble(AnswerDataSamples.plan(issuedPlan()))).isEmpty();
    }

    @Test
    @DisplayName("Facts carry two cards for a question or three for the daily summary, never another count")
    void cardsCount() {
        var card = new FactCardTemplate(CatalogText.of("v"), CatalogText.of("l"), CatalogText.of("p"),
                FactTone.PRIMARY);
        var facts = new java.util.TreeMap<String, Object>(Map.of("scope", "FIELD"));

        assertThat(new AssembledFacts(facts, CatalogText.of("t"), List.of(card, card), null, null).cards())
                .hasSize(AssembledFacts.CARDS);
        assertThat(new AssembledFacts(facts, CatalogText.of("t"), List.of(card, card, card), null, null).cards())
                .hasSize(AssembledFacts.SUMMARY_CARDS);
        for (var cards : List.of(List.of(card), List.of(card, card, card, card))) {
            assertThatThrownBy(() -> new AssembledFacts(facts, CatalogText.of("t"), cards, null, null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessage("assistant.facts.cards");
        }
    }
}
