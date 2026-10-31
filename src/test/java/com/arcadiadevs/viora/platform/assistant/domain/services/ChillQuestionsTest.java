package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.chill;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.closedCompleted;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.closedShort;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.completedInSeason;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.halted;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.inProgress;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.insufficient;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.lastWinter;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.lastWinterShort;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.lastWinterWithGap;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.notEvaluated;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.notReachable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChillQuestions: the questions and answers of the winter chill screen")
class ChillQuestionsTest {

    private static final PlotId PLOT = new PlotId(UUID.randomUUID().toString());
    private static final List<QuestionId> QUESTIONS = List.of(QuestionId.CHILL_MISSING, QuestionId.CHILL_REACH_30,
            QuestionId.CHILL_VS_LAST_WINTER, QuestionId.CHILL_IF_INCOMPLETE);

    private final AnswerLocalizer localizer = new AnswerLocalizer(messages());

    @Test
    @DisplayName("WINTER_CHILL is a plot screen whose questions use the contract prefix chill.")
    void screen() {
        assertThat(ScreenKey.fromValue("winter_chill")).isEqualTo(ScreenKey.WINTER_CHILL);
        assertThat(ScreenKey.WINTER_CHILL.requiresPlot()).isTrue();
        assertThat(ScreenKey.WINTER_CHILL.questionPrefix()).isEqualTo("chill.");
        assertThat(QuestionId.CHILL_REACH_30.value()).isEqualTo("chill.reach_30");
        assertThat(QuestionId.CHILL_REACH_30.screen()).isEqualTo(ScreenKey.WINTER_CHILL);
        assertThat(new QuestionId("chill.if_incomplete").screen()).isEqualTo(ScreenKey.WINTER_CHILL);
        assertThatThrownBy(() -> new QuestionId("winter_chill.missing").screen())
                .hasMessage("assistant.screen.invalid");
        // Digits only after the first word of the question
        assertThatThrownBy(() -> new QuestionId("chill.30_reach")).hasMessage("assistant.question.invalid");
    }

    @Test
    @DisplayName("catalog of a winter in progress: four questions in contract order, labels with the real threshold")
    void catalogInProgress() {
        var questions = QuestionCatalog.questionsFor(chill(inProgress(), lastWinter()));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "chill.missing", "chill.reach_30", "chill.vs_last_winter", "chill.if_incomplete");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("temp", "calendar", "history",
                "alert");
        assertThat(questions).allMatch(CatalogQuestion::enabled);
        assertThat(questions).extracting(CatalogQuestion::label).containsExactly(
                CatalogText.of("assistant.question.chill.missing.label"),
                CatalogText.of("assistant.question.chill.reach_30.label", 30.0),
                CatalogText.of("assistant.question.chill.vs_last_winter.label"),
                CatalogText.of("assistant.question.chill.if_incomplete.label", 30.0));
        assertThat(questions).extracting(q -> localizer.resolve(q.label(), Locale.forLanguageTag("es")))
                .containsExactly("¿Cuánto frío me falta?", "¿Cuándo llego a las 30 porciones?",
                        "¿Cómo va frente al invierno pasado?", "¿Qué pasa si no llego a las 30 porciones?");
    }

    @Test
    @DisplayName("catalog after August 31: the questions are about the finished winter and name its year")
    void catalogFinishedWinter() {
        var questions = QuestionCatalog.questionsFor(chill(closedShort(), lastWinter()));

        assertThat(questions).allMatch(CatalogQuestion::enabled);
        assertThat(questions).extracting(CatalogQuestion::label).containsExactly(
                CatalogText.of("assistant.question.chill.missing.label.closed", "2026"),
                CatalogText.of("assistant.question.chill.reach_30.label.closed", 30.0, "2026"),
                CatalogText.of("assistant.question.chill.vs_last_winter.label.closed", "2026", "2025"),
                CatalogText.of("assistant.question.chill.if_incomplete.label.closed", 30.0, "2026"));
        assertThat(localizer.resolve(questions.get(2).label(), Locale.ENGLISH))
                .isEqualTo("How did the 2026 winter compare with 2025?");
    }

    @Test
    @DisplayName("catalog without chill data disables every question: \"Sin datos de frío para este lote\"")
    void catalogWithoutData() {
        for (var facts : List.of(chill(null, null), chill(notEvaluated(), lastWinter()))) {
            var questions = QuestionCatalog.questionsFor(facts);
            assertThat(questions).noneMatch(CatalogQuestion::enabled);
            assertThat(questions).extracting(CatalogQuestion::disabledReason)
                    .containsOnly(CatalogText.of("assistant.reason.chill_missing"));
            assertThat(questions.get(1).label())
                    .isEqualTo(CatalogText.of("assistant.question.chill.reach_30.label_without_threshold"));
            for (var question : QUESTIONS) {
                assertThat(AnswerComposer.compose(question, facts, PLOT)).as(question.value()).isEmpty();
            }
        }
    }

    @Test
    @DisplayName("catalog: too few days to project disables only reach_30; without last winter only the comparison")
    void catalogPartialData() {
        var early = QuestionCatalog.questionsFor(chill(insufficient(), lastWinter()));
        assertThat(early).extracting(CatalogQuestion::enabled).containsExactly(true, false, true, true);
        assertThat(early.get(1).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.chill_pace_missing"));
        assertThat(AnswerComposer.compose(QuestionId.CHILL_REACH_30, chill(insufficient(), null), PLOT)).isEmpty();

        var noLastWinter = QuestionCatalog.questionsFor(chill(inProgress(), null));
        assertThat(noLastWinter).extracting(CatalogQuestion::enabled).containsExactly(true, true, false, true);
        assertThat(noLastWinter.get(2).disabledReason())
                .isEqualTo(CatalogText.of("assistant.reason.last_winter_missing"));
        assertThat(AnswerComposer.compose(QuestionId.CHILL_VS_LAST_WINTER, chill(inProgress(), null), PLOT)).isEmpty();

        // A completed winter needs no projection
        assertThat(QuestionCatalog.questionsFor(chill(completedInSeason(), null)).get(1).enabled()).isTrue();
    }

    @Test
    @DisplayName("missing: 24,4 of 30 and what is missing, so both figures add up to the requirement, in es and en")
    void missing() {
        var es = answer(QuestionId.CHILL_MISSING, chill(inProgress(), lastWinter()), "es");
        assertThat(es.text()).isEqualTo("Tu lote lleva 24,4 de 30 porciones de frío este invierno: le faltan 5,6. "
                + "Están contadas hasta el 20 de julio.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("24,4/30", "porciones de frío", "hasta el 20 jul", FactTone.PRIMARY),
                new AnswerFact("5,6", "porciones por completar", "requisito 30", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label()).containsExactly(
                "Modelo de frío de Erez con el clima horario de Open-Meteo, hasta el 20 de julio");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.CHILL_MISSING, chill(inProgress(), lastWinter()), "en");
        assertThat(en.text()).isEqualTo("Your plot has 24.4 of 30 chill portions this winter: 5.6 to go. They are "
                + "counted through July 20.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("24.4/30", "chill portions", "through Jul 20", FactTone.PRIMARY),
                new AnswerFact("5.6", "portions to go", "requirement 30", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label())
                .containsExactly("Erez chill model on Open-Meteo hourly weather, through July 20");
    }

    @Test
    @DisplayName("missing: a halted, completed or finished season says so")
    void missingOtherStates() {
        assertThat(answer(QuestionId.CHILL_MISSING, chill(halted(), null), "es").text()).isEqualTo("Tu lote lleva "
                + "24,4 de 30 porciones de frío: le faltan 5,6. Hasta el 20 de julio, una racha de días sobre 24 °C "
                + "frena la acumulación.");
        var completed = answer(QuestionId.CHILL_MISSING, chill(completedInSeason(), null), "en");
        assertThat(completed.text()).isEqualTo("Your plot met the 30 chill portions on August 4 and has 31.6 through "
                + "August 10. It is no longer short of chill this winter.");
        assertThat(completed.facts().get(1)).isEqualTo(
                new AnswerFact("Complete", "chill requirement", "on Aug 4", FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.CHILL_MISSING, chill(closedShort(), null), "es").text()).isEqualTo("En el "
                + "invierno 2026 tu lote acumuló 27,5 de 30 porciones de frío: le faltaron 2,5 al cerrar el 31 de "
                + "agosto.");
        assertThat(answer(QuestionId.CHILL_MISSING, chill(closedCompleted(), null), "es").text()).isEqualTo("En el "
                + "invierno 2026 tu lote acumuló 34,2 porciones de frío y llegó a las 30 el 4 de agosto. No le faltó "
                + "frío.");
    }

    @Test
    @DisplayName("reach_30: the estimated date against the budbreak of August 31, in es and en")
    void reachProjected() {
        var es = answer(QuestionId.CHILL_REACH_30, chill(inProgress(), lastWinter()), "es");
        assertThat(es.text()).isEqualTo("Al ritmo reciente, tu lote llegaría a las 30 porciones el 4 de agosto, 27 días "
                + "antes de la brotación del 31 de agosto. Es una estimación: una racha de días cálidos la puede "
                + "retrasar.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("4 ago", "fecha estimada", "lleva 24,4", FactTone.PRIMARY),
                new AnswerFact("31 ago", "brotación", "27 días de margen", FactTone.HIGHLIGHT));

        var en = answer(QuestionId.CHILL_REACH_30, chill(inProgress(), lastWinter()), "en");
        assertThat(en.text()).isEqualTo("At the recent pace your plot would reach 30 portions on August 4, 27 days "
                + "before the budbreak of August 31. It is an estimate: a run of warm days can delay it.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("Aug 4", "estimated date", "24.4 so far", FactTone.PRIMARY),
                new AnswerFact("Aug 31", "budbreak", "27 days of margin", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("reach_30: not reachable in season, reached, or a finished winter that fell short")
    void reachOtherStates() {
        var unreachable = answer(QuestionId.CHILL_REACH_30, chill(notReachable(), null), "es");
        assertThat(unreachable.text()).isEqualTo("Al ritmo reciente, tu lote no llegaría a las 30 porciones antes de la "
                + "brotación del 31 de agosto: lleva 12,1 y le faltan 17,9. Vuelve a mirarlo en unos días, el frío "
                + "puede repuntar.");
        assertThat(unreachable.facts()).containsExactly(
                new AnswerFact("Sin fecha", "fecha estimada", "lleva 12,1", FactTone.PRIMARY),
                new AnswerFact("31 ago", "brotación", "a 17,9 del requisito", FactTone.HIGHLIGHT));

        var reached = answer(QuestionId.CHILL_REACH_30, chill(completedInSeason(), null), "en");
        assertThat(reached.text()).isEqualTo("Your plot reached 30 chill portions on August 4, 27 days before the "
                + "budbreak of August 31.");
        assertThat(reached.facts().get(0)).isEqualTo(
                new AnswerFact("Aug 4", "requirement met", "requirement 30", FactTone.PRIMARY));
        assertThat(answer(QuestionId.CHILL_REACH_30, chill(closedCompleted(), null), "en").text())
                .startsWith("Your plot reached 30 chill portions on August 4");
        assertThat(answer(QuestionId.CHILL_REACH_30, chill(closedShort(), null), "es").text())
                .isEqualTo("El invierno 2026 cerró el 31 de agosto con 27,5 porciones: tu lote no llegó a las 30.");
    }

    @Test
    @DisplayName("vs_last_winter: the same date of last winter while in season, and how that winter ended")
    void versusLastWinterInSeason() {
        var es = answer(QuestionId.CHILL_VS_LAST_WINTER, chill(inProgress(), lastWinter()), "es");
        assertThat(es.text()).isEqualTo("Al 20 de julio tu lote lleva 24,4 porciones; el invierno pasado, a la misma "
                + "fecha, llevaba 18. Aquel invierno llegó a las 30 porciones el 10 de agosto.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("24,4", "porciones este invierno", "hasta el 20 jul", FactTone.PRIMARY),
                new AnswerFact("18", "invierno pasado", "a la misma fecha", FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.CHILL_VS_LAST_WINTER, chill(inProgress(), lastWinter()), "en").text())
                .isEqualTo("By July 20 your plot has 24.4 portions; last winter, on the same date, it had 18. That "
                        + "winter reached the 30 portions on August 10.");

        assertThat(answer(QuestionId.CHILL_VS_LAST_WINTER, chill(inProgress(), lastWinterShort()), "es").text())
                .endsWith("Aquel invierno cerró con 26,8 porciones, sin llegar a las 30.");

        // Last winter without the same day: only its whole season
        var gap = answer(QuestionId.CHILL_VS_LAST_WINTER, chill(inProgress(), lastWinterWithGap()), "es");
        assertThat(gap.text()).isEqualTo("Tu lote lleva 24,4 porciones al 20 de julio. Aquel invierno llegó a las 30 "
                + "porciones el 10 de agosto.");
        assertThat(gap.facts().get(1)).isEqualTo(
                new AnswerFact("33,1", "invierno pasado", "temporada completa", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("vs_last_winter: once finished, both whole seasons and their warm days, in es and en")
    void versusLastWinterClosed() {
        var es = answer(QuestionId.CHILL_VS_LAST_WINTER, chill(closedShort(), lastWinter()), "es");
        assertThat(es.text()).isEqualTo("El invierno 2026 cerró con 27,5 porciones y el de 2025 con 33,1. En 2026 hubo "
                + "11 días sobre 24 °C, frente a 5 en 2025.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("27,5", "porciones en 2026", "11 días sobre 24 °C", FactTone.PRIMARY),
                new AnswerFact("33,1", "porciones en 2025", "5 días sobre 24 °C", FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.CHILL_VS_LAST_WINTER, chill(closedShort(), lastWinter()), "en").text())
                .isEqualTo("The 2026 winter closed with 27.5 portions and the 2025 one with 33.1. In 2026 there were "
                        + "11 days above 24 °C, against 5 in 2025.");
    }

    @Test
    @DisplayName("if_incomplete: what a short winter means, worded conservatively, or that the requirement was met")
    void ifIncomplete() {
        var es = answer(QuestionId.CHILL_IF_INCOMPLETE, chill(inProgress(), null), "es");
        assertThat(es.text()).isEqualTo("Si tu lote no completa las 30 porciones antes de la brotación del 31 de agosto "
                + "(lleva 24,4), la brotación y la floración pueden salir más tardías y desparejas. Revisa con tu "
                + "técnico cómo manejarlo.");
        assertThat(es.facts()).extracting(AnswerFact::value).containsExactly("24,4/30", "5,6");
        assertThat(answer(QuestionId.CHILL_IF_INCOMPLETE, chill(inProgress(), null), "en").text()).isEqualTo("If your "
                + "plot does not reach 30 portions before the budbreak of August 31 (it has 24.4), budbreak and bloom "
                + "may come later and uneven. Check with your technician how to handle it.");

        assertThat(answer(QuestionId.CHILL_IF_INCOMPLETE, chill(notReachable(), null), "es").text()).isEqualTo("Al "
                + "ritmo reciente, tu lote no llegaría a las 30 porciones antes del 31 de agosto: lleva 12,1. Con frío "
                + "insuficiente, la brotación y la floración pueden salir más tardías y desparejas; revisa con tu "
                + "técnico cómo manejarlo.");
        assertThat(answer(QuestionId.CHILL_IF_INCOMPLETE, chill(insufficient(), null), "es").text())
                .startsWith("Si tu lote no completa las 30 porciones").contains("(lleva 3,2)");
        assertThat(answer(QuestionId.CHILL_IF_INCOMPLETE, chill(completedInSeason(), null), "en").text())
                .isEqualTo("Your plot already met the 30 chill portions on August 4, so it is not short of chill this "
                        + "winter. If budbreak still comes uneven, review it with your technician.");
        assertThat(answer(QuestionId.CHILL_IF_INCOMPLETE, chill(closedShort(), null), "es").text()).isEqualTo("Tu "
                + "lote cerró el invierno 2026 con 27,5 de 30 porciones: le faltaron 2,5. Con frío insuficiente, la "
                + "brotación y la floración pueden salir más tardías y desparejas; revísalo con tu técnico.");
        assertThat(answer(QuestionId.CHILL_IF_INCOMPLETE, chill(closedCompleted(), null), "en").text())
                .isEqualTo("In the 2026 winter your plot met the 30 chill portions on August 4: it was not short of "
                        + "chill. If budbreak comes uneven, review it with your technician.");
    }

    @Test
    @DisplayName("the data version changes with a new evaluated day or with last winter, never with the daily curve alone")
    void dataVersion() {
        var version = AnswerComposer.dataVersionOf(ScreenKey.WINTER_CHILL, chill(inProgress(), lastWinter()));

        assertThat(AnswerComposer.dataVersionOf(ScreenKey.WINTER_CHILL, chill(inProgress(), lastWinter())))
                .isEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.WINTER_CHILL, chill(halted(), lastWinter())))
                .isNotEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.WINTER_CHILL, chill(inProgress(), lastWinterShort())))
                .isNotEqualTo(version);
        var otherCurve = new ChillFacts.Season(2026, 24.43, 30.0, "IN_PROGRESS", "PROJECTED",
                inProgress().projectedCompletionDate(), null, inProgress().seasonEnd(), inProgress().evaluatedThrough(),
                2, 24.0, null);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.WINTER_CHILL, chill(otherCurve, lastWinter())))
                .isEqualTo(version);
    }

    private AnswerLocalizer.Localized answer(QuestionId question, ChillFacts facts, String language) {
        var draft = AnswerComposer.compose(question, facts, PLOT);
        assertThat(draft).as(question.value()).isPresent();
        assertThat(draft.get().facts()).hasSize(2);
        return localizer.localize(draft.get(), new AnswerLanguage(language));
    }

    private static ResourceBundleMessageSource messages() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
