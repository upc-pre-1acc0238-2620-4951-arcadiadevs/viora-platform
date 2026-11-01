package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Forecast;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.ForecastDay;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Incident;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotDetailFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.THRESHOLDS;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.TODAY;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.frost;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.heat;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.hydric;
import static com.arcadiadevs.viora.platform.assistant.domain.services.PlotDetailSamples.CAMPAIGN;
import static com.arcadiadevs.viora.platform.assistant.domain.services.PlotDetailSamples.hotToday;
import static com.arcadiadevs.viora.platform.assistant.domain.services.PlotDetailSamples.issuedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.PlotDetailSamples.openPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.PlotDetailSamples.plot;
import static com.arcadiadevs.viora.platform.assistant.domain.services.PlotDetailSamples.samplingPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.PlotDetailSamples.thinnedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.PlotDetailSamples.todayForecast;
import static com.arcadiadevs.viora.platform.assistant.domain.services.PlotDetailSamples.withoutIndex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlotDetailQuestions: the questions and answers of the plot detail screen")
class PlotDetailQuestionsTest {

    private static final PlotId PLOT = new PlotId(UUID.randomUUID().toString());
    private static final LocalDate OCT_29 = LocalDate.of(2026, 10, 29);

    private final AnswerLocalizer localizer = new AnswerLocalizer(messages());

    @Test
    @DisplayName("PLOT_DETAIL is a plot screen whose questions use the plot. prefix")
    void screen() {
        assertThat(ScreenKey.fromValue("PLOT_DETAIL")).isEqualTo(ScreenKey.PLOT_DETAIL);
        assertThat(ScreenKey.PLOT_DETAIL.requiresPlot()).isTrue();
        assertThat(ScreenKey.PLOT_DETAIL.questionPrefix()).isEqualTo("plot.");
        assertThat(QuestionId.PLOT_HOW_IS.screen()).isEqualTo(ScreenKey.PLOT_DETAIL);
        assertThat(plot(null, 0, List.of(), null).screen()).isEqualTo(ScreenKey.PLOT_DETAIL);
    }

    @Test
    @DisplayName("facts need today and keep only the heat, frost and hydric alerts")
    void facts() {
        assertThatThrownBy(() -> new PlotDetailFacts(null, CAMPAIGN, null, 0, null, null, 0, List.of(), THRESHOLDS,
                null)).isInstanceOf(IllegalArgumentException.class).hasMessage("assistant.plot_detail.invalid");

        var other = new Incident("SENSOR_OFFLINE", "CRITICAL", 0, 0, "", TODAY, 0, 0);
        assertThat(plot(null, 0, List.of(other, heat()), null).openAlerts()).containsExactly(heat());
        assertThat(plot(null, 0, null, null).openAlerts()).isEmpty();
        assertThat(plot(null, 0, List.of(), null).canonical())
                .isNotEqualTo(plot(openPlan(), 0, List.of(), null).canonical())
                .isNotEqualTo(plot(null, 0, List.of(heat()), null).canonical());
    }

    @Test
    @DisplayName("catalog with index, sampling and caliber: the four questions enabled, the harvest naming next year")
    void catalogComplete() {
        var questions = QuestionCatalog.questionsFor(plot(thinnedPlan(14250.0), 0, List.of(), hotToday()));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "plot.how_is", "plot.today", "plot.last_sampling", "plot.next_year_harvest");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("lote", "calendar", "fruit", "harvest");
        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(true, true, true, true);
        assertThat(questions).extracting(CatalogQuestion::disabledReason).containsOnlyNulls();
        assertThat(questions).extracting(CatalogQuestion::label).containsExactly(
                CatalogText.of("assistant.question.plot.how_is.label"),
                CatalogText.of("assistant.question.plot.today.label"),
                CatalogText.of("assistant.question.plot.last_sampling.label"),
                CatalogText.of("assistant.question.plot.next_year_harvest.label", "2027"));
    }

    @Test
    @DisplayName("catalog without index or sampling: only today enabled, each other question with its reason")
    void catalogWithoutData() {
        var questions = QuestionCatalog.questionsFor(withoutIndex(null, 0, List.of(), null));

        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(false, true, false, false);
        assertThat(questions).extracting(CatalogQuestion::disabledReason).containsExactly(
                CatalogText.of("assistant.reason.plot_data_missing"),
                null,
                CatalogText.of("assistant.reason.sampling_missing"),
                CatalogText.of("assistant.reason.caliber_missing"));
        assertThat(answerDraft(QuestionId.PLOT_HOW_IS, withoutIndex(null, 0, List.of(), null))).isFalse();
        assertThat(answerDraft(QuestionId.PLOT_LAST_SAMPLING, withoutIndex(null, 0, List.of(), null))).isFalse();
        assertThat(answerDraft(QuestionId.PLOT_NEXT_YEAR_HARVEST, withoutIndex(null, 0, List.of(), null))).isFalse();
    }

    @Test
    @DisplayName("how_is needs an index in a known band or a sampled tree; a plan without trees is not a sampling")
    void howIsRule() {
        var indexOnly = QuestionCatalog.questionsFor(plot(null, 0, List.of(), null));
        assertThat(indexOnly).extracting(CatalogQuestion::enabled).containsExactly(true, true, false, false);

        var samplingOnly = QuestionCatalog.questionsFor(withoutIndex(samplingPlan(3, OCT_29), 2, List.of(), null));
        assertThat(samplingOnly).extracting(CatalogQuestion::enabled).containsExactly(true, true, true, false);

        var unknownBand = new PlotDetailFacts(TODAY, CAMPAIGN, samplingPlan(0, null), 5, 0.5, "UNCLASSIFIED", 2,
                List.of(), THRESHOLDS, null);
        assertThat(QuestionCatalog.questionsFor(unknownBand)).extracting(CatalogQuestion::disabledReason)
                .containsExactly(CatalogText.of("assistant.reason.plot_data_missing"), null,
                        CatalogText.of("assistant.reason.sampling_missing"),
                        CatalogText.of("assistant.reason.caliber_missing"));
        assertThat(answerDraft(QuestionId.PLOT_HOW_IS, unknownBand)).isFalse();
    }

    @Test
    @DisplayName("next_year_harvest needs an estimated caliber with its figures")
    void harvestRule() {
        var notEstimated = new ThinningPlanData(true, 30.0, 0.57, 0.4, null, null, 5, OCT_29, OCT_29,
                "INSUFFICIENT_DATA", 180.4, "171/190", 14250.0);
        var withoutFigure = new ThinningPlanData(true, 30.0, 0.57, 0.4, null, null, 5, OCT_29, OCT_29,
                "ESTIMATED", null, "171/190", 14250.0);
        for (var plan : List.of(notEstimated, withoutFigure)) {
            var facts = plot(plan, 0, List.of(), null);
            assertThat(QuestionCatalog.questionsFor(facts).get(3).disabledReason())
                    .isEqualTo(CatalogText.of("assistant.reason.caliber_missing"));
            assertThat(answerDraft(QuestionId.PLOT_NEXT_YEAR_HARVEST, facts)).isFalse();
        }
    }

    @Test
    @DisplayName("how_is, contract example: index 0,51 with its band and 12 days left in the open window")
    void howIsOpen() {
        var facts = plot(openPlan(), 0, List.of(), hotToday());

        var es = answer(QuestionId.PLOT_HOW_IS, facts, "es");
        assertThat(es.text()).isEqualTo("Tu índice de vecería es 0,51, vecería moderada. La ventana de aclareo está "
                + "abierta: quedan 12 días para quitar el 30 % de los frutos, hasta el 10 de noviembre.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("0,51", "índice de vecería", "vecería moderada", FactTone.PRIMARY),
                new AnswerFact("12", "días de ventana", "ventana abierta", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 2 campañas registradas");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.PLOT_HOW_IS, facts, "en");
        assertThat(en.text()).isEqualTo("Your alternate bearing index is 0.51, moderate alternate bearing. The thinning "
                + "window is open: 12 days are left to remove 30 % of the fruits, until November 10.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("0.51", "alternate bearing index", "moderate alternate bearing", FactTone.PRIMARY),
                new AnswerFact("12", "days of window left", "window open", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label()).containsExactly("Based on 2 recorded campaigns");
    }

    @Test
    @DisplayName("how_is on the last day of the window counts today: 1 day left")
    void howIsLastDay() {
        var facts = plot(issuedPlan(LocalDate.of(2026, 10, 20), TODAY), 0, List.of(), null);

        var es = answer(QuestionId.PLOT_HOW_IS, facts, "es");
        assertThat(es.text()).endsWith("La ventana de aclareo está abierta: queda 1 día para quitar el 30 % de los "
                + "frutos, hasta el 30 de octubre.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("1", "día de ventana", "ventana abierta",
                FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.PLOT_HOW_IS, facts, "en").text()).endsWith("The thinning window is open: 1 day "
                + "is left to remove 30 % of the fruits, until October 30.");
    }

    @Test
    @DisplayName("how_is before the window: when it opens and the percentage")
    void howIsBefore() {
        var facts = plot(issuedPlan(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 20)), 0, List.of(), null);

        var es = answer(QuestionId.PLOT_HOW_IS, facts, "es");
        assertThat(es.text()).isEqualTo("Tu índice de vecería es 0,51, vecería moderada. La ventana de aclareo abre el "
                + "3 de noviembre y el plan indica quitar el 30 % de los frutos.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("4", "días para abrir", "abre el 3 nov",
                FactTone.HIGHLIGHT));
        var en = answer(QuestionId.PLOT_HOW_IS, facts, "en");
        assertThat(en.text()).isEqualTo("Your alternate bearing index is 0.51, moderate alternate bearing. The thinning "
                + "window opens on November 3 and the plan says to remove 30 % of the fruits.");
        assertThat(en.facts().get(1)).isEqualTo(new AnswerFact("4", "days until it opens", "opens Nov 3",
                FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("how_is after the window closed without a thinning")
    void howIsClosed() {
        var facts = plot(issuedPlan(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20)), 0, List.of(), null);

        var es = answer(QuestionId.PLOT_HOW_IS, facts, "es");
        assertThat(es.text()).isEqualTo("Tu índice de vecería es 0,51, vecería moderada. La ventana de aclareo cerró el "
                + "20 de octubre sin aclareo registrado.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("Cerrada", "ventana de aclareo", "cerró el 20 oct",
                FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.PLOT_HOW_IS, facts, "en").text()).isEqualTo("Your alternate bearing index is "
                + "0.51, moderate alternate bearing. The thinning window closed on October 20 with no thinning "
                + "recorded.");
    }

    @Test
    @DisplayName("how_is without index after the thinning: no index card, recorded thinning, sampling source")
    void howIsDoneWithoutIndex() {
        var facts = withoutIndex(thinnedPlan(14250.0), 0, List.of(), null);

        var es = answer(QuestionId.PLOT_HOW_IS, facts, "es");
        assertThat(es.text()).isEqualTo("Aún faltan cosechas registradas para calcular el índice de vecería del lote. "
                + "Registraste el aclareo de esta campaña el 28 de octubre.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("Sin índice", "índice de vecería", "faltan cosechas", FactTone.PRIMARY),
                new AnswerFact("Registrado", "aclareo de esta campaña", "el 28 oct", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 5 árboles, ronda del 27 de octubre");
        var en = answer(QuestionId.PLOT_HOW_IS, facts, "en");
        assertThat(en.text()).isEqualTo("More recorded harvests are needed to calculate the alternate bearing index of "
                + "the plot. You recorded the thinning of this campaign on October 28.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("No index", "alternate bearing index", "harvests missing", FactTone.PRIMARY),
                new AnswerFact("Recorded", "thinning this campaign", "on Oct 28", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label())
                .containsExactly("Based on 5 trees, round of October 27");
    }

    @Test
    @DisplayName("how_is with a sampling halfway: the trees still missing, singular and plural")
    void howIsSampling() {
        var facts = withoutIndex(samplingPlan(3, OCT_29), 2, List.of(), null);

        var es = answer(QuestionId.PLOT_HOW_IS, facts, "es");
        assertThat(es.text()).isEqualTo("Aún faltan cosechas registradas para calcular el índice de vecería del lote. "
                + "Al muestreo le faltan 2 árboles para calcular el plan de aclareo.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("Sin plan", "aclareo de esta campaña",
                "3 árboles muestreados", FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.PLOT_HOW_IS, facts, "en").text()).isEqualTo("More recorded harvests are needed to "
                + "calculate the alternate bearing index of the plot. The sampling still needs 2 trees to calculate "
                + "the thinning plan.");

        var oneMissing = withoutIndex(samplingPlan(4, OCT_29), 1, List.of(), null);
        assertThat(answer(QuestionId.PLOT_HOW_IS, oneMissing, "es").text())
                .endsWith("Al muestreo le falta 1 árbol para calcular el plan de aclareo.");
        assertThat(answer(QuestionId.PLOT_HOW_IS, oneMissing, "en").text())
                .endsWith("The sampling still needs 1 tree to calculate the thinning plan.");
    }

    @Test
    @DisplayName("how_is with a complete sampling and no prescription yet, undated: sampled trees as source")
    void howIsUnissued() {
        var facts = withoutIndex(samplingPlan(5, null), 0, List.of(), null);

        var es = answer(QuestionId.PLOT_HOW_IS, facts, "es");
        assertThat(es.text()).isEqualTo("Aún faltan cosechas registradas para calcular el índice de vecería del lote. "
                + "El muestreo de esta campaña está completo y el plan de aclareo aún no se calcula.");
        assertThat(es.sources()).extracting(source -> source.label()).containsExactly("Basado en 5 árboles muestreados");
        var en = answer(QuestionId.PLOT_HOW_IS, facts, "en");
        assertThat(en.text()).isEqualTo("More recorded harvests are needed to calculate the alternate bearing index of "
                + "the plot. The sampling of this campaign is complete and the thinning plan is not calculated yet.");
        assertThat(en.sources()).extracting(source -> source.label()).containsExactly("Based on 5 sampled trees");
    }

    @Test
    @DisplayName("how_is with an index and no sampling: no plan card; without harvests the plan is the source")
    void howIsNoSampling() {
        var facts = plot(null, 0, List.of(), null);

        var es = answer(QuestionId.PLOT_HOW_IS, facts, "es");
        assertThat(es.text()).isEqualTo("Tu índice de vecería es 0,51, vecería moderada. Aún no registras un muestreo "
                + "en esta campaña.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("Sin plan", "aclareo de esta campaña",
                "registra un muestreo", FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.PLOT_HOW_IS, facts, "en").text()).isEqualTo("Your alternate bearing index is "
                + "0.51, moderate alternate bearing. You have not recorded a sampling this campaign yet.");

        var noHarvests = new PlotDetailFacts(TODAY, CAMPAIGN, null, 0, 0.12, "REGULAR", 0, List.of(), THRESHOLDS,
                null);
        var regular = answer(QuestionId.PLOT_HOW_IS, noHarvests, "es");
        assertThat(regular.text()).startsWith("Tu índice de vecería es 0,12, producción regular.");
        assertThat(regular.sources()).extracting(source -> source.label())
                .containsExactly("Basado en el plan de aclareo de esta campaña");
    }

    @Test
    @DisplayName("today: open window, heat over the threshold and one heat alert")
    void todayOpenHeat() {
        var facts = plot(openPlan(), 0, List.of(heat()), hotToday());

        var es = answer(QuestionId.PLOT_TODAY, facts, "es");
        assertThat(es.text()).isEqualTo("Hoy puedes aclarear: la ventana cierra el 10 de noviembre y el plan indica "
                + "quitar el 30 % de los frutos. Hoy se esperan 34,2 °C de máxima, que alcanzan el umbral de calor de "
                + "32 °C. Tienes 1 alerta activa, la más grave de golpe de calor en nivel aviso.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("34,2 °C", "máxima de hoy", "mínima 14 °C", FactTone.PRIMARY),
                new AnswerFact("1", "alerta activa", "en este lote", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en el plan de aclareo de esta campaña");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.PLOT_TODAY, facts, "en");
        assertThat(en.text()).isEqualTo("You can thin today: the window closes on November 10 and the plan says to "
                + "remove 30 % of the fruits. Today a maximum of 34.2 °C is expected, reaching the heat threshold of "
                + "32 °C. You have 1 open alert, the most severe one of heat wave at warning level.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("34.2 °C", "today's maximum", "minimum 14 °C", FactTone.PRIMARY),
                new AnswerFact("1", "open alert", "on this plot", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("today: window still to open, frost at the threshold and no alerts")
    void todayBeforeFrost() {
        var facts = plot(issuedPlan(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 20)), 0, List.of(),
                todayForecast(9.0, 1.0, false, true));

        var es = answer(QuestionId.PLOT_TODAY, facts, "es");
        assertThat(es.text()).isEqualTo("Aún no toca aclarear: la ventana abre el 3 de noviembre. Hoy la mínima baja a "
                + "1 °C y alcanza el umbral de helada de 1,5 °C. No tienes alertas activas en el lote.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("9 °C", "máxima de hoy", "mínima 1 °C", FactTone.PRIMARY),
                new AnswerFact("0", "alertas activas", "en este lote", FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.PLOT_TODAY, facts, "en").text()).isEqualTo("It is not time to thin yet: the "
                + "window opens on November 3. Today the minimum drops to 1 °C and reaches the frost threshold of "
                + "1.5 °C. You have no open alerts in the plot.");
    }

    @Test
    @DisplayName("today: sampling halfway, a mild day and three alerts led by the critical frost")
    void todaySamplingMild() {
        var facts = withoutIndex(samplingPlan(3, OCT_29), 2, List.of(heat(), frost(), hydric()),
                todayForecast(24.0, 12.5, false, false));

        var es = answer(QuestionId.PLOT_TODAY, facts, "es");
        assertThat(es.text()).isEqualTo("Completa el muestreo: faltan 2 árboles para calcular el plan de aclareo. Hoy "
                + "se esperan entre 12,5 y 24 °C, sin riesgo de calor ni de helada. Tienes 3 alertas activas, la más "
                + "grave de helada en nivel crítico.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("24 °C", "máxima de hoy", "mínima 12,5 °C", FactTone.PRIMARY),
                new AnswerFact("3", "alertas activas", "en este lote", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en el plan de aclareo de esta campaña");
        assertThat(answer(QuestionId.PLOT_TODAY, facts, "en").text()).isEqualTo("Complete the sampling: 2 trees are "
                + "missing to calculate the thinning plan. Today between 12.5 and 24 °C are expected, with no heat or "
                + "frost risk. You have 3 open alerts, the most severe one of frost at critical level.");
    }

    @Test
    @DisplayName("today with nothing pending and no forecast still answers, based on the open alerts")
    void todayNothing() {
        var facts = withoutIndex(null, 0, List.of(), null);

        var es = answer(QuestionId.PLOT_TODAY, facts, "es");
        assertThat(es.text()).isEqualTo("Hoy no tienes tareas de aclareo pendientes en el lote. No hay pronóstico "
                + "disponible para hoy. No tienes alertas activas en el lote.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("Sin pronóstico", "máxima de hoy", "sin datos", FactTone.PRIMARY),
                new AnswerFact("0", "alertas activas", "en este lote", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en las alertas abiertas del lote");
        var en = answer(QuestionId.PLOT_TODAY, facts, "en");
        assertThat(en.text()).isEqualTo("You have no pending thinning tasks in the plot today. There is no forecast "
                + "available for today. You have no open alerts in the plot.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("No forecast", "today's maximum", "no data", FactTone.PRIMARY),
                new AnswerFact("0", "open alerts", "on this plot", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label())
                .containsExactly("Based on the open alerts of the plot");
    }

    @Test
    @DisplayName("today without a plan but with a forecast cites the forecast")
    void todayForecastSource() {
        var es = answer(QuestionId.PLOT_TODAY, withoutIndex(null, 0, List.of(), hotToday()), "es");
        assertThat(es.text()).startsWith("Hoy no tienes tareas de aclareo pendientes en el lote. Hoy se esperan "
                + "34,2 °C de máxima");
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Pronóstico de Open-Meteo, actualizado el 30 de octubre");
    }

    @Test
    @DisplayName("today: a closed or recorded window and a complete sampling leave no thinning task")
    void todayNoThinningTask() {
        var closed = plot(issuedPlan(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20)), 0, List.of(), null);
        var done = plot(thinnedPlan(14250.0), 0, List.of(), null);
        var complete = plot(samplingPlan(5, OCT_29), 0, List.of(), null);
        for (var facts : List.of(closed, done, complete)) {
            assertThat(answer(QuestionId.PLOT_TODAY, facts, "es").text())
                    .startsWith("Hoy no tienes tareas de aclareo pendientes en el lote.");
            assertThat(answer(QuestionId.PLOT_TODAY, facts, "es").sources()).extracting(source -> source.label())
                    .containsExactly("Basado en el plan de aclareo de esta campaña");
        }
    }

    @Test
    @DisplayName("today: without today in the forecast or without thresholds there is no forecast for today")
    void todayForecastMissing() {
        var stale = new Forecast(TODAY.minusDays(3), List.of(
                new ForecastDay(TODAY.minusDays(3), 34.0, 14.0, true, false),
                new ForecastDay(TODAY.plusDays(1), 34.0, 14.0, true, false)));
        var withoutToday = withoutIndex(null, 0, List.of(), stale);
        var withoutThresholds = new PlotDetailFacts(TODAY, CAMPAIGN, null, 0, null, null, 0, List.of(), null,
                hotToday());
        for (var facts : List.of(withoutToday, withoutThresholds)) {
            var es = answer(QuestionId.PLOT_TODAY, facts, "es");
            assertThat(es.text()).contains("No hay pronóstico disponible para hoy.");
            assertThat(es.facts().get(0).value()).isEqualTo("Sin pronóstico");
            assertThat(es.sources()).extracting(source -> source.label())
                    .containsExactly("Basado en las alertas abiertas del lote");
        }
    }

    @Test
    @DisplayName("last_sampling with the prescription issued: load against target and the percentage")
    void lastSamplingIssued() {
        var facts = plot(openPlan(), 0, List.of(), null);

        var es = answer(QuestionId.PLOT_LAST_SAMPLING, facts, "es");
        assertThat(es.text()).isEqualTo("Tu muestreo de esta campaña suma 5 árboles, el último el 29 de octubre: midió "
                + "0,57 frutos por brote frente al objetivo de 0,40, por eso el plan indica quitar el 30 % de los "
                + "frutos.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("5", "árboles muestreados", "última ronda 29 oct", FactTone.PRIMARY),
                new AnswerFact("0,57", "frutos por brote · estimado", "objetivo 0,40", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 5 árboles, ronda del 29 de octubre");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.PLOT_LAST_SAMPLING, facts, "en");
        assertThat(en.text()).isEqualTo("Your sampling this campaign has 5 trees, the latest on October 29: it measured "
                + "0.57 fruits per shoot against a target of 0.40, which is why the plan says to remove 30 % of the "
                + "fruits.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("5", "trees sampled", "latest round Oct 29", FactTone.PRIMARY),
                new AnswerFact("0.57", "fruits per shoot · estimated", "target 0.40", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label())
                .containsExactly("Based on 5 trees, round of October 29");
    }

    @Test
    @DisplayName("last_sampling halfway: the trees still missing for the plan")
    void lastSamplingHalfway() {
        var facts = withoutIndex(samplingPlan(1, OCT_29), 4, List.of(), null);

        var es = answer(QuestionId.PLOT_LAST_SAMPLING, facts, "es");
        assertThat(es.text()).isEqualTo("Tu muestreo de esta campaña lleva 1 árbol, el último el 29 de octubre: faltan "
                + "4 árboles para calcular el plan de aclareo.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("1", "árbol muestreado", "última ronda 29 oct", FactTone.PRIMARY),
                new AnswerFact("4", "árboles por muestrear", "para el plan", FactTone.HIGHLIGHT));
        var en = answer(QuestionId.PLOT_LAST_SAMPLING, facts, "en");
        assertThat(en.text()).isEqualTo("Your sampling this campaign has 1 tree, the latest on October 29: 4 trees are "
                + "missing to calculate the thinning plan.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("1", "tree sampled", "latest round Oct 29", FactTone.PRIMARY),
                new AnswerFact("4", "trees to sample", "for the plan", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("last_sampling complete without a plan and without a round date")
    void lastSamplingPendingUndated() {
        var facts = withoutIndex(samplingPlan(5, null), 0, List.of(), null);

        var es = answer(QuestionId.PLOT_LAST_SAMPLING, facts, "es");
        assertThat(es.text()).isEqualTo("Tu muestreo de esta campaña suma 5 árboles, sin fecha de ronda, y el plan de "
                + "aclareo aún no se calcula.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("5", "árboles muestreados", "sin fecha", FactTone.PRIMARY),
                new AnswerFact("Sin plan", "aclareo de esta campaña", "5 árboles muestreados", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label()).containsExactly("Basado en 5 árboles muestreados");
        var en = answer(QuestionId.PLOT_LAST_SAMPLING, facts, "en");
        assertThat(en.text()).isEqualTo("Your sampling this campaign has 5 trees, with no round date, and the thinning "
                + "plan is not calculated yet.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("5", "trees sampled", "no date", FactTone.PRIMARY),
                new AnswerFact("No plan", "thinning this campaign", "5 sampled trees", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("next_year_harvest with the yield: kilos and caliber of the confirmed thinning")
    void nextYearHarvestYield() {
        var facts = plot(thinnedPlan(14250.0), 0, List.of(), null);

        var es = answer(QuestionId.PLOT_NEXT_YEAR_HARVEST, facts, "es");
        assertThat(es.text()).isEqualTo("Con la carga que dejó tu aclareo, Viora estima 14.250 kg por hectárea para la "
                + "cosecha de esta campaña, con un calibre más probable de 180 frutos por kilo (171/190). Es una "
                + "proyección en calibración.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("14.250", "kg por hectárea estimados", "en calibración", FactTone.PRIMARY),
                new AnswerFact("180", "frutos por kilo · calibre más probable", "calibre 171/190",
                        FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en el aclareo registrado el 28 de octubre");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.PLOT_NEXT_YEAR_HARVEST, facts, "en");
        assertThat(en.text()).isEqualTo("With the load your thinning left, Viora estimates 14,250 kg per hectare for "
                + "the harvest of this campaign, with a most likely caliber of 180 fruits per kilo (171/190). It is a "
                + "projection still being calibrated.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("14,250", "estimated kg per hectare", "calibrating", FactTone.PRIMARY),
                new AnswerFact("180", "fruits per kilo · most likely caliber", "caliber 171/190",
                        FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label())
                .containsExactly("Based on the thinning recorded on October 28");
    }

    @Test
    @DisplayName("next_year_harvest without the yield: the caliber only")
    void nextYearHarvestNoYield() {
        var facts = plot(thinnedPlan(null), 0, List.of(), null);

        var es = answer(QuestionId.PLOT_NEXT_YEAR_HARVEST, facts, "es");
        assertThat(es.text()).isEqualTo("Tras tu aclareo, el calibre más probable es de 180 frutos por kilo (171/190). "
                + "Viora aún no estima los kilos de la cosecha: faltan cosechas registradas o la superficie del lote.");
        assertThat(es.facts().get(0)).isEqualTo(new AnswerFact("Sin estimar", "kilos de la cosecha", "faltan datos",
                FactTone.PRIMARY));
        var en = answer(QuestionId.PLOT_NEXT_YEAR_HARVEST, facts, "en");
        assertThat(en.text()).isEqualTo("After your thinning, the most likely caliber is 180 fruits per kilo (171/190). "
                + "Viora does not estimate the harvest kilos yet: recorded harvests or the plot area are missing.");
        assertThat(en.facts().get(0)).isEqualTo(new AnswerFact("Not estimated", "harvest kilos", "data missing",
                FactTone.PRIMARY));
    }

    @Test
    @DisplayName("next_year_harvest before the thinning is recorded cites the campaigns, or the plan without them")
    void nextYearHarvestSources() {
        var unconfirmed = new ThinningPlanData(true, 30.0, 0.5714, 0.4, null, null, 5, OCT_29, null, "ESTIMATED",
                180.4, "171/190", 14250.0);
        assertThat(answer(QuestionId.PLOT_NEXT_YEAR_HARVEST, plot(unconfirmed, 0, List.of(), null), "es").sources())
                .extracting(source -> source.label()).containsExactly("Basado en 2 campañas registradas");
        assertThat(answer(QuestionId.PLOT_NEXT_YEAR_HARVEST, withoutIndex(unconfirmed, 0, List.of(), null), "es")
                .sources()).extracting(source -> source.label())
                .containsExactly("Basado en el plan de aclareo de esta campaña");
    }

    @Test
    @DisplayName("a question of another screen is not answered by the plot detail")
    void otherScreenQuestion() {
        assertThat(PlotDetailQuestions.answer(QuestionId.CLIMATE_SENSORS_SAY, plot(openPlan(), 0, List.of(), null)))
                .isEmpty();
    }

    private boolean answerDraft(QuestionId question, PlotDetailFacts facts) {
        return AnswerComposer.compose(question, facts, PLOT).isPresent();
    }

    private AnswerLocalizer.Localized answer(QuestionId question, PlotDetailFacts facts, String language) {
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
