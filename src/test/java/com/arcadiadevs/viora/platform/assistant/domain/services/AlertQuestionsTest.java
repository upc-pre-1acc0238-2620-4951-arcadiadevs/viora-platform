package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AlertFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.UUID;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.THRESHOLDS;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.alert;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.forecast;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.frost;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.heat;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.hotThenMild;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.hydric;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AlertQuestions: the questions and answers of the alert screen")
class AlertQuestionsTest {

    private static final PlotId PLOT = new PlotId(UUID.randomUUID().toString());

    private final AnswerLocalizer localizer = new AnswerLocalizer(messages());

    @Test
    @DisplayName("ALERT is a plot screen whose questions use the alert. prefix")
    void screen() {
        assertThat(ScreenKey.fromValue("alert")).isEqualTo(ScreenKey.ALERT);
        assertThat(ScreenKey.ALERT.requiresPlot()).isTrue();
        assertThat(ScreenKey.ALERT.questionPrefix()).isEqualTo("alert.");
        assertThat(QuestionId.ALERT_WHEN_ENDS.screen()).isEqualTo(ScreenKey.ALERT);
    }

    @Test
    @DisplayName("catalog with a heat alert and a forecast: four questions in contract order, all enabled")
    void catalogWithHeat() {
        var questions = QuestionCatalog.questionsFor(alert(heat(), hotThenMild()));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "alert.what_to_do", "alert.fruitset_effect", "alert.when_ends", "alert.plots_at_risk");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("alert", "fruit", "calendar", "lote");
        assertThat(questions).allMatch(CatalogQuestion::enabled);
        assertThat(questions.get(0).label())
                .isEqualTo(CatalogText.of("assistant.question.alert.what_to_do.label.HEAT_WAVE"));
    }

    @Test
    @DisplayName("catalog without an open alert disables every question: \"Sin alertas activas en este lote\"")
    void catalogWithoutIncident() {
        var questions = QuestionCatalog.questionsFor(alert(null, hotThenMild()));

        assertThat(questions).noneMatch(CatalogQuestion::enabled);
        assertThat(questions).extracting(CatalogQuestion::disabledReason)
                .containsOnly(CatalogText.of("assistant.reason.alert_missing"));
        assertThat(questions.get(0).label()).isEqualTo(CatalogText.of("assistant.question.alert.what_to_do.label"));
        for (var question : questions) {
            assertThat(AnswerComposer.compose(question.questionId(), alert(null, hotThenMild()), PLOT)).isEmpty();
        }
    }

    @Test
    @DisplayName("catalog: alert.when_ends needs a forecast, and the forecast says nothing about soil moisture")
    void catalogWhenEnds() {
        assertThat(QuestionCatalog.questionsFor(alert(heat(), null)).get(2).disabledReason())
                .isEqualTo(CatalogText.of("assistant.reason.forecast_missing"));
        var hydric = QuestionCatalog.questionsFor(alert(hydric(), hotThenMild()));
        assertThat(hydric.get(2).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.forecast_soil"));
        assertThat(hydric).extracting(CatalogQuestion::enabled).containsExactly(true, true, false, true);
        assertThat(QuestionCatalog.questionsFor(alert(frost(), hotThenMild())).get(2).enabled()).isTrue();
    }

    @Test
    @DisplayName("what_to_do: the breach against the threshold and the mitigation steps done, in es and en")
    void whatToDo() {
        var es = answer(QuestionId.ALERT_WHAT_TO_DO, alert(heat(), null), "es");
        assertThat(es.text()).isEqualTo("Tu lote tiene una alerta de golpe de calor en nivel aviso: 34,2 °C frente "
                + "al umbral de 32 °C. Sigue los pasos de mitigación de la alerta; llevas 1 de 3 completados.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("34,2 °C", "máxima prevista", "umbral 32 °C", FactTone.PRIMARY),
                new AnswerFact("Aviso", "nivel de la alerta", "desde el 28 oct", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en la alerta del 28 de octubre");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.ALERT_WHAT_TO_DO, alert(heat(), null), "en");
        assertThat(en.text()).isEqualTo("Your plot has a heat wave alert at warning level: 34.2 °C against the "
                + "threshold of 32 °C. Follow the mitigation steps of the alert; you have completed 1 of 3.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("34.2 °C", "forecast maximum", "threshold 32 °C", FactTone.PRIMARY),
                new AnswerFact("Warning", "alert level", "since Oct 28", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label()).containsExactly("Based on the alert of October 28");
    }

    @Test
    @DisplayName("what_to_do without mitigation steps points to the technician, in es and en")
    void whatToDoWithoutSteps() {
        assertThat(answer(QuestionId.ALERT_WHAT_TO_DO, alert(hydric(), null), "es").text())
                .isEqualTo("Tu lote tiene una alerta de estrés hídrico en nivel aviso: 18,5 % frente al umbral de "
                        + "20 %. Vigila el lote estos días y revisa la alerta con tu técnico.");
        var en = answer(QuestionId.ALERT_WHAT_TO_DO, alert(hydric(), null), "en");
        assertThat(en.text()).isEqualTo("Your plot has a water stress alert at warning level: 18.5 % against the "
                + "threshold of 20 %. Keep an eye on the plot these days and review the alert with your technician.");
        assertThat(en.facts().get(0)).isEqualTo(
                new AnswerFact("18.5 %", "soil moisture", "threshold 20 %", FactTone.PRIMARY));
    }

    @Test
    @DisplayName("fruitset_effect: only the breach; Viora does not estimate the effect on fruit set")
    void fruitsetEffect() {
        var es = answer(QuestionId.ALERT_FRUITSET_EFFECT, alert(heat(), null), "es");
        assertThat(es.text()).isEqualTo("Tu alerta de golpe de calor marca 34,2 °C frente al umbral de 32 °C. Viora "
                + "aún no estima su efecto en el cuajado: evalúalo en el lote con tu técnico.");
        assertThat(es.facts()).extracting(AnswerFact::value).containsExactly("34,2 °C", "Aviso");

        assertThat(answer(QuestionId.ALERT_FRUITSET_EFFECT, alert(frost(), null), "en").text())
                .isEqualTo("Your frost alert reads -2.4 °C against the threshold of -2 °C. Viora does not estimate "
                        + "its effect on fruit set yet: assess it on the plot with your technician.");
    }

    @Test
    @DisplayName("when_ends: the heat lasts three days and ends on the fourth, from Telemetry's flags")
    void whenEndsHeat() {
        var es = answer(QuestionId.ALERT_WHEN_ENDS, alert(heat(), hotThenMild()), "es");
        assertThat(es.text()).isEqualTo("El pronóstico mantiene el riesgo de golpe de calor hasta el 1 de noviembre: "
                + "3 de 7 días cruzan el umbral de 32 °C. Desde el 2 de noviembre ningún día lo cruza.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("3", "días con riesgo", "umbral 32 °C", FactTone.PRIMARY),
                new AnswerFact("2 nov", "fin previsto del riesgo", "pronóstico de 7 días", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Pronóstico de Open-Meteo, actualizado el 30 de octubre");

        var en = answer(QuestionId.ALERT_WHEN_ENDS, alert(heat(), hotThenMild()), "en");
        assertThat(en.text()).isEqualTo("The forecast keeps the heat wave risk until November 1: 3 of 7 days cross "
                + "the threshold of 32 °C. From November 2 on, no day crosses it.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("3", "days at risk", "threshold 32 °C", FactTone.PRIMARY),
                new AnswerFact("Nov 2", "expected end of the risk", "7-day forecast", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("when_ends: risk on the last forecast day, or no risk at all, have their own texts")
    void whenEndsBeyondOrClear() {
        var beyond = answer(QuestionId.ALERT_WHEN_ENDS,
                alert(heat(), forecast(30.0, 31.0, 33.0, 30.0, 31.0, 33.5, 34.0)), "es");
        assertThat(beyond.text()).isEqualTo("El pronóstico mantiene el riesgo de golpe de calor hasta el 5 de "
                + "noviembre, el último día que cubre: 3 de 7 días cruzan el umbral de 32 °C. Aún no se ve el final; "
                + "vuelve a mirar el pronóstico en unos días.");
        assertThat(beyond.facts().get(1).value()).isEqualTo("Sin fecha");

        var clear = answer(QuestionId.ALERT_WHEN_ENDS,
                alert(heat(), forecast(30.0, 31.0, 29.0, 30.0, 31.0, 28.5, 27.0)), "en");
        assertThat(clear.text()).isEqualTo("No day of the next 7 crosses the threshold of 32 °C in the forecast. The "
                + "heat wave alert stays open until it is normalized; review its steps with your technician.");
        assertThat(clear.facts()).containsExactly(
                new AnswerFact("0", "days at risk", "threshold 32 °C", FactTone.PRIMARY),
                new AnswerFact("No risk", "expected end of the risk", "7-day forecast", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("when_ends of a frost uses the frost flags and threshold; a hydric alert or no forecast has no answer")
    void whenEndsFrostAndHydric() {
        var frost = answer(QuestionId.ALERT_WHEN_ENDS,
                alert(frost(), forecast(9.0, 8.5, 12.0, 15.0, 16.0, 18.0, 19.0)), "en");
        assertThat(frost.text()).isEqualTo("The forecast keeps the frost risk until October 31: 2 of 7 days cross the "
                + "threshold of 1.5 °C. From November 1 on, no day crosses it.");

        assertThat(AnswerComposer.compose(QuestionId.ALERT_WHEN_ENDS, alert(hydric(), hotThenMild()), PLOT)).isEmpty();
        assertThat(AnswerComposer.compose(QuestionId.ALERT_WHEN_ENDS, alert(heat(), null), PLOT)).isEmpty();
    }

    @Test
    @DisplayName("plots_at_risk: plots of the producer with the same alert, this one included, in es and en")
    void plotsAtRisk() {
        var many = answer(QuestionId.ALERT_PLOTS_AT_RISK, alert(heat(), null), "es");
        assertThat(many.text()).isEqualTo("2 de tus 5 lotes activos tienen una alerta de golpe de calor abierta, "
                + "contando este. Cada uno tiene los pasos de mitigación en su alerta.");
        assertThat(many.facts()).containsExactly(
                new AnswerFact("2", "lotes con esta alerta", "golpe de calor", FactTone.PRIMARY),
                new AnswerFact("5", "lotes activos", "solo tus lotes", FactTone.HIGHLIGHT));
        assertThat(many.sources()).extracting(source -> source.label())
                .containsExactly("Basado en las alertas abiertas de tus lotes");
        assertThat(answer(QuestionId.ALERT_PLOTS_AT_RISK, alert(heat(), null), "en").text())
                .isEqualTo("2 of your 5 active plots have an open heat wave alert, this one included. Each one has "
                        + "the mitigation steps in its alert.");

        assertThat(answer(QuestionId.ALERT_PLOTS_AT_RISK, new AlertFacts(THRESHOLDS, heat(), 1, 5, null), "es").text())
                .isEqualTo("Solo este lote tiene una alerta de golpe de calor abierta entre tus 5 lotes activos. Los "
                        + "demás no la muestran por ahora.");
        var single = answer(QuestionId.ALERT_PLOTS_AT_RISK, new AlertFacts(THRESHOLDS, heat(), 1, 1, null), "en");
        assertThat(single.text()).isEqualTo("This is your only active plot and it has an open heat wave alert. There "
                + "are no other plots to check for now.");
        assertThat(single.facts()).extracting(AnswerFact::label).containsExactly("plot with this alert", "active plot");
        assertThat(AnswerComposer.compose(QuestionId.ALERT_PLOTS_AT_RISK, new AlertFacts(THRESHOLDS, heat(), 0, 5,
                null), PLOT)).isEmpty();
    }

    @Test
    @DisplayName("the data version changes when the alert or the forecast changes, and not on another read")
    void dataVersion() {
        var version = AnswerComposer.dataVersionOf(ScreenKey.ALERT, alert(heat(), hotThenMild()));

        assertThat(AnswerComposer.dataVersionOf(ScreenKey.ALERT, alert(heat(), hotThenMild()))).isEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.ALERT, alert(frost(), hotThenMild()))).isNotEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.ALERT, alert(heat(), forecast(30.0, 31.0))))
                .isNotEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.ALERT, new AlertFacts(THRESHOLDS, heat(), 3, 5,
                hotThenMild()))).isNotEqualTo(version);
    }

    private AnswerLocalizer.Localized answer(QuestionId question, AlertFacts facts, String language) {
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
