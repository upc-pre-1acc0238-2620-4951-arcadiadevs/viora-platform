package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AlertFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import static com.arcadiadevs.viora.platform.assistant.domain.services.ChillSamples.notReachable;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.THRESHOLDS;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.alert;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.climate;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.forecast;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.frost;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.heat;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.hotThenMild;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.hydric;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.microclimate;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.reading;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.realProbe;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.virtualProbe;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every template of the ALERT, CLIMATE and WINTER_CHILL screens against the validator the model text goes through:
 * the template itself passes, so a faithful rephrasing can replace it, and a practice the template does not name
 * (irrigation above all, on a screen that asks about it) keeps the template.
 */
@DisplayName("ALERT, CLIMATE and WINTER_CHILL templates and the validator")
class AlertClimateChillTemplatesTest {

    private static final PlotId PLOT = new PlotId(UUID.randomUUID().toString());
    private static final AnswerLanguage ES = AnswerLanguage.SPANISH;
    private static final AnswerLanguage EN = new AnswerLanguage("en");
    private static final List<QuestionId> QUESTIONS = List.of(
            QuestionId.ALERT_WHAT_TO_DO, QuestionId.ALERT_FRUITSET_EFFECT, QuestionId.ALERT_WHEN_ENDS,
            QuestionId.ALERT_PLOTS_AT_RISK, QuestionId.CLIMATE_IRRIGATE, QuestionId.CLIMATE_FRUITSET_EFFECT,
            QuestionId.CLIMATE_HEAT_ENDS, QuestionId.CLIMATE_SENSORS_SAY, QuestionId.CHILL_MISSING,
            QuestionId.CHILL_REACH_30, QuestionId.CHILL_VS_LAST_WINTER, QuestionId.CHILL_IF_INCOMPLETE);

    private final AnswerLocalizer localizer = new AnswerLocalizer(messages());

    @Test
    @DisplayName("every variant resolves in es and en, has two cards and passes the validator as its own rephrasing")
    void everyTemplatePassesAsItsOwnRephrasing() {
        var variants = new HashSet<String>();
        int rendered = 0;
        for (var facts : scenarios()) {
            for (var question : QUESTIONS) {
                if (question.screen() != facts.screen()) {
                    continue;
                }
                var draft = AnswerComposer.compose(question, facts, PLOT);
                if (draft.isEmpty()) {
                    continue;
                }
                variants.add(draft.get().text().key());
                for (var language : List.of(ES, EN)) {
                    var answer = localizer.localize(draft.get(), language);
                    var where = draft.get().text().key() + " [" + language.code() + "]: " + answer.text();
                    var texts = new ArrayList<>(List.of(answer.text()));
                    answer.facts().forEach(fact -> texts.addAll(List.of(fact.value(), fact.label(), fact.pill())));
                    answer.sources().forEach(source -> texts.add(source.label()));
                    assertThat(texts).as(where).allSatisfy(text -> assertThat(text).doesNotContain("assistant.")
                            .doesNotContain("{").doesNotContain("}").isNotBlank());
                    assertThat(answer.facts()).as(where).hasSize(2);
                    assertThat(AnswerTextValidator.validate(answer.text(), answer.text(), answer.facts(), language))
                            .as(where).isEqualTo(Verdict.ACCEPTED);
                    rendered++;
                }
            }
        }
        // alert: 2 + 1 + 3 + 3; climate: 3 + 2 + 3 + 2; chill: 5 + 4 + 3 + 5
        assertThat(variants).hasSize(36);
        assertThat(rendered).isGreaterThan(90);
    }

    @Test
    @DisplayName("climate.irrigate: a rephrasing of the reading passes; adding irrigation keeps the template, es and en")
    void irrigationIsNeverAddedByTheModel() {
        var draft = AnswerComposer.compose(QuestionId.CLIMATE_IRRIGATE,
                climate(null, List.of(realProbe("CRITICAL")), reading(14.0)), PLOT).orElseThrow();
        var es = localizer.localize(draft, ES);
        var en = localizer.localize(draft, EN);

        assertThat(AnswerTextValidator.validate("Tu sonda a 30 cm marca 14 % de humedad, bajo el crítico de 16 %. La "
                + "decisión del agua es tuya según tu turno.", es.text(), es.facts(), ES)).isEqualTo(Verdict.ACCEPTED);
        assertThat(AnswerTextValidator.validate("Tu sonda a 30 cm marca 14 % de humedad, bajo el crítico de 16 %. "
                + "Riega hoy mismo.", es.text(), es.facts(), ES)).isEqualTo(Verdict.FORBIDDEN_TERM);
        assertThat(AnswerTextValidator.validate("Con 14 % de humedad a 30 cm debes regar esta semana.", es.text(),
                es.facts(), ES)).isEqualTo(Verdict.FORBIDDEN_TERM);
        assertThat(AnswerTextValidator.validate("Tu sonda marca 14 %: aumenta el riego.", es.text(), es.facts(), ES))
                .isEqualTo(Verdict.FORBIDDEN_TERM);

        assertThat(AnswerTextValidator.validate("Your probe at 30 cm reads 14 % moisture, below the critical 16 %. The "
                + "water decision is yours.", en.text(), en.facts(), EN)).isEqualTo(Verdict.ACCEPTED);
        assertThat(AnswerTextValidator.validate("Your probe at 30 cm reads 14 % moisture. You should irrigate today.",
                en.text(), en.facts(), EN)).isEqualTo(Verdict.FORBIDDEN_TERM);
        assertThat(AnswerTextValidator.validate("Your probe reads 14 %, so water the plot this week.", en.text(),
                en.facts(), EN)).isEqualTo(Verdict.FORBIDDEN_TERM);
    }

    @Test
    @DisplayName("the alert and chill templates keep their caution: no new practice, no lost hedge")
    void alertAndChillKeepTheirCaution() {
        var alert = localizer.localize(AnswerComposer.compose(QuestionId.ALERT_WHAT_TO_DO, alert(heat(), null), PLOT)
                .orElseThrow(), ES);
        assertThat(AnswerTextValidator.validate("Tu lote tiene un golpe de calor de 34,2 °C sobre 32 °C: aplica un "
                + "riego de auxilio.", alert.text(), alert.facts(), ES)).isEqualTo(Verdict.FORBIDDEN_TERM);

        var chill = localizer.localize(AnswerComposer.compose(QuestionId.CHILL_IF_INCOMPLETE,
                chill(inProgress(), null), PLOT).orElseThrow(), ES);
        assertThat(AnswerTextValidator.validate("Si tu lote no llega a las 30 porciones antes del 31 de agosto, la "
                + "brotación saldrá tardía y despareja.", chill.text(), chill.facts(), ES)).isEqualTo(Verdict.LOST_HEDGE);
        assertThat(AnswerTextValidator.validate("Si tu lote no llega a las 30 porciones antes del 31 de agosto, la "
                + "brotación puede salir tardía y despareja.", chill.text(), chill.facts(), ES))
                .isEqualTo(Verdict.ACCEPTED);
    }

    private static List<ScreenFacts> scenarios() {
        var mild = forecast(30.0, 31.0, 29.0, 30.0, 31.0, 28.5, 27.0);
        var beyond = forecast(30.0, 31.0, 33.0, 30.0, 31.0, 33.5, 34.0);
        return List.of(
                alert(heat(), hotThenMild()), alert(heat(), beyond), alert(heat(), mild),
                alert(frost(), forecast(9.0, 8.5, 12.0, 15.0, 16.0, 18.0, 19.0)), alert(hydric(), null),
                new AlertFacts(THRESHOLDS, heat(), 1, 5, null), new AlertFacts(THRESHOLDS, heat(), 1, 1, null),
                climate(hotThenMild(), List.of(realProbe("CRITICAL")), reading(14.0)),
                climate(beyond, List.of(realProbe("WARNING")), reading(18.5)),
                climate(mild, List.of(realProbe(null)), reading(24.0)),
                climate(hotThenMild(), List.of(virtualProbe(), microclimate()), reading(21.54)),
                chill(inProgress(), lastWinter()), chill(inProgress(), lastWinterShort()),
                chill(inProgress(), lastWinterWithGap()), chill(halted(), lastWinterShort()),
                chill(notReachable(), lastWinter()), chill(insufficient(), lastWinter()),
                chill(completedInSeason(), lastWinter()), chill(closedShort(), lastWinter()),
                chill(closedCompleted(), lastWinterShort()));
    }

    private static ResourceBundleMessageSource messages() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
