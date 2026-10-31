package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DailySummaryDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.TODAY;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.forecast;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.heat;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.bare;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.field;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.full;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.onePlot;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.openPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.plan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.HomeSamples.withPlan;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Daily summary of the home screen: the week plan with three cards (window, week maximum, open alerts)")
class DailySummaryTextsTest {

    private static final AnswerLanguage ES = AnswerLanguage.SPANISH;
    private static final AnswerLanguage EN = new AnswerLanguage("en");

    private final AnswerLocalizer localizer = new AnswerLocalizer(messages());

    @Test
    @DisplayName("a summary always carries exactly three cards")
    void threeCards() {
        var text = CatalogText.of("a");
        var card = new FactDraft(text, text, text, FactTone.PRIMARY);
        assertThatThrownBy(() -> new DailySummaryDraft(text, List.of(card, card), text))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("assistant.daily_summary.facts.count");
        assertThatThrownBy(() -> new DailySummaryDraft(text, List.of(card, card, card, card), text))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("assistant.daily_summary.facts.count");
        assertThat(new DailySummaryDraft(text, List.of(card, card, card), text).facts()).hasSize(3);
    }

    @Test
    @DisplayName("open window of a plot (es): days left today included, then the week plan's cards, text and source")
    void openWindowOfAPlotEs() {
        var facts = onePlot(full(openPlan(), 0, List.of(heat()), null, forecast(34.2, 35.1, 33.0)));
        var summary = summary(facts, ES);

        assertThat(summary.facts()).hasSize(3);
        assertThat(summary.facts().get(0)).isEqualTo(
                new AnswerFact("12", "días de ventana", "cierra el 10 nov", FactTone.PRIMARY));
        var week = weekPlan(facts, ES);
        assertThat(summary.text()).isEqualTo(week.text());
        assertThat(summary.sources()).isEqualTo(week.sources());
        assertThat(summary.facts().subList(1, 3)).isEqualTo(week.facts());
        assertThat(summary.facts().get(1).value()).isEqualTo("35,1 °C");
        assertThat(summary.facts().get(2).pill()).isEqualTo("en este lote");
    }

    @Test
    @DisplayName("open window of a plot (en)")
    void openWindowOfAPlotEn() {
        var facts = onePlot(full(openPlan(), 0, List.of(heat()), null, forecast(34.2, 35.1, 33.0)));
        var summary = summary(facts, EN);

        assertThat(summary.facts().get(0)).isEqualTo(
                new AnswerFact("12", "days of window left", "closes Nov 10", FactTone.PRIMARY));
        assertThat(summary.text()).isEqualTo(weekPlan(facts, EN).text());
    }

    @Test
    @DisplayName("field with a window opening this week and none open: days until it opens (es and en)")
    void windowOpeningInTheField() {
        var facts = field(withPlan(plan(TODAY.plusDays(3), TODAY.plusDays(20)), 0), bare());

        assertThat(summary(facts, ES).facts().get(0)).isEqualTo(
                new AnswerFact("3", "días para abrir", "abre el 2 nov", FactTone.PRIMARY));
        assertThat(summary(facts, EN).facts().get(0)).isEqualTo(
                new AnswerFact("3", "days until it opens", "opens Nov 2", FactTone.PRIMARY));
        assertThat(summary(facts, ES).text()).isEqualTo(weekPlan(facts, ES).text());
    }

    @Test
    @DisplayName("the open window that closes first leads over one opening this week")
    void openWindowLeads() {
        var facts = field(withPlan(plan(TODAY.plusDays(2), TODAY.plusDays(30)), 0),
                withPlan(plan(TODAY.minusDays(5), LocalDate.of(2026, 11, 4)), 0),
                withPlan(openPlan(), 0));

        assertThat(summary(facts, ES).facts().get(0)).isEqualTo(
                new AnswerFact("6", "días de ventana", "cierra el 4 nov", FactTone.PRIMARY));
    }

    @Test
    @DisplayName("no window open or opening this week: the window card says so, without a number (es and en)")
    void noWindow() {
        var facts = onePlot(withPlan(plan(TODAY.plusDays(10), TODAY.plusDays(30)), 0));

        assertThat(summary(facts, ES).facts().get(0)).isEqualTo(
                new AnswerFact("Sin ventana", "ventana de aclareo", "próximos 7 días", FactTone.PRIMARY));
        assertThat(summary(facts, EN).facts().get(0)).isEqualTo(
                new AnswerFact("No window", "thinning window", "next 7 days", FactTone.PRIMARY));
    }

    @Test
    @DisplayName("nothing to summarize without active plots")
    void nothingToSummarize() {
        assertThat(DailySummaryTexts.summarize(new HomeFacts(TODAY, 2026, true, List.of(), null))).isEmpty();
    }

    @Test
    @DisplayName("the summary is not a question of the catalog")
    void notInTheCatalog() {
        assertThat(QuestionId.HOME_DAILY_SUMMARY.value()).isEqualTo("home.daily_summary");
        assertThat(QuestionCatalog.questionsFor(onePlot(bare())))
                .noneMatch(question -> question.questionId().equals(QuestionId.HOME_DAILY_SUMMARY));
        assertThat(AnswerComposer.compose(QuestionId.HOME_DAILY_SUMMARY, onePlot(bare()),
                new PlotId(UUID.randomUUID().toString()))).isEmpty();
    }

    private AnswerLocalizer.Localized summary(HomeFacts facts, AnswerLanguage language) {
        var draft = DailySummaryTexts.summarize(facts);
        assertThat(draft).isPresent();
        return localizer.localize(draft.get(), language);
    }

    private AnswerLocalizer.Localized weekPlan(HomeFacts facts, AnswerLanguage language) {
        return localizer.localize(HomeQuestions.answer(QuestionId.HOME_WEEK_PLAN, facts).orElseThrow(), language);
    }

    private static ResourceBundleMessageSource messages() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
