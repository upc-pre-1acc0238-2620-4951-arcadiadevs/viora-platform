package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.List;
import java.util.UUID;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.climate;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.forecast;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.hotThenMild;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.microclimate;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.reading;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.realProbe;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ClimateSamples.virtualProbe;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClimateQuestions: the questions and answers of the climate screen")
class ClimateQuestionsTest {

    private static final PlotId PLOT = new PlotId(UUID.randomUUID().toString());

    private final AnswerLocalizer localizer = new AnswerLocalizer(messages());

    @Test
    @DisplayName("CLIMATE is a plot screen whose questions use the climate. prefix")
    void screen() {
        assertThat(ScreenKey.fromValue("CLIMATE")).isEqualTo(ScreenKey.CLIMATE);
        assertThat(ScreenKey.CLIMATE.requiresPlot()).isTrue();
        assertThat(ScreenKey.CLIMATE.questionPrefix()).isEqualTo("climate.");
        assertThat(QuestionId.CLIMATE_SENSORS_SAY.screen()).isEqualTo(ScreenKey.CLIMATE);
    }

    @Test
    @DisplayName("catalog with virtual nodes only: irrigation disabled, \"Requiere una sonda de humedad real\"")
    void catalogWithVirtualNodes() {
        var questions = QuestionCatalog.questionsFor(climate(hotThenMild(), List.of(virtualProbe(), microclimate()),
                reading(18.0)));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "climate.irrigate", "climate.fruitset_effect", "climate.heat_ends", "climate.sensors_say");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("water", "fruit", "temp", "sensor");
        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(false, true, true, true);
        assertThat(questions.get(0).disabledReason())
                .isEqualTo(CatalogText.of("assistant.reason.real_probe_missing"));
        assertThat(questions.get(0).label()).isEqualTo(CatalogText.of("assistant.question.climate.irrigate.label"));
    }

    @Test
    @DisplayName("catalog with a real probe reading recently enables irrigation")
    void catalogWithRealProbe() {
        var questions = QuestionCatalog.questionsFor(climate(hotThenMild(), List.of(realProbe("WARNING")),
                reading(18.0)));

        assertThat(questions).allMatch(CatalogQuestion::enabled);
        assertThat(questions.get(0).disabledReason()).isNull();
    }

    @Test
    @DisplayName("catalog without forecast, nodes or recent readings disables the questions that need them")
    void catalogWithoutData() {
        var none = QuestionCatalog.questionsFor(climate(null, List.of(), null));
        assertThat(none).noneMatch(CatalogQuestion::enabled);
        assertThat(none).extracting(CatalogQuestion::disabledReason).containsExactly(
                CatalogText.of("assistant.reason.real_probe_missing"),
                CatalogText.of("assistant.reason.forecast_missing"),
                CatalogText.of("assistant.reason.forecast_missing"),
                CatalogText.of("assistant.reason.nodes_missing"));

        var stale = QuestionCatalog.questionsFor(climate(null, List.of(realProbe("WARNING")), null));
        assertThat(stale.get(0).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.readings_missing"));
        assertThat(stale.get(3).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.readings_missing"));
    }

    @Test
    @DisplayName("irrigate: a warning reading against the probe thresholds, the decision left to the producer")
    void irrigateWarning() {
        var facts = climate(null, List.of(realProbe("WARNING")), reading(18.5));

        var es = answer(QuestionId.CLIMATE_IRRIGATE, facts, "es");
        assertThat(es.text()).isEqualTo("Tu sonda a 30 cm marca 18,5 % de humedad, bajo el umbral de aviso de 20 % y "
                + "sin llegar al crítico de 16 %. Vigila la lectura y decide el agua con tu técnico.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("18,5 %", "humedad del suelo", "sonda 30 cm", FactTone.PRIMARY),
                new AnswerFact("20 %", "umbral de aviso", "crítico 16 %", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label()).containsExactly("Sonda 30 cm");
        assertThat(es.actions()).isEmpty();
        assertThat(answer(QuestionId.CLIMATE_IRRIGATE, facts, "en").text()).isEqualTo("Your probe at 30 cm reads "
                + "18.5 % moisture, under the warning threshold of 20 % and above the critical one of 16 %. Follow the "
                + "reading and decide on water with your technician.");
    }

    @Test
    @DisplayName("irrigate: critical and normal readings have their own texts, in es and en")
    void irrigateCriticalAndOk() {
        var critical = climate(null, List.of(realProbe("CRITICAL")), reading(14.0));
        assertThat(answer(QuestionId.CLIMATE_IRRIGATE, critical, "es").text()).isEqualTo("Tu sonda a 30 cm marca 14 % "
                + "de humedad, por debajo del umbral crítico de 16 %. La decisión del agua es tuya: compárala con tu "
                + "turno y con lo que ves en el lote.");
        assertThat(answer(QuestionId.CLIMATE_IRRIGATE, critical, "en").text()).isEqualTo("Your probe at 30 cm reads "
                + "14 % moisture, below the critical threshold of 16 %. The water decision is yours: weigh it against "
                + "your water turn and what you see on the plot.");

        var ok = climate(null, List.of(realProbe(null)), reading(24.0));
        assertThat(answer(QuestionId.CLIMATE_IRRIGATE, ok, "es").text()).isEqualTo("Tu sonda a 30 cm marca 24 % de "
                + "humedad, sin bajar del umbral de aviso de 20 %. Por ahora la lectura no muestra falta de agua en el "
                + "suelo.");
        assertThat(answer(QuestionId.CLIMATE_IRRIGATE, ok, "en").text()).isEqualTo("Your probe at 30 cm reads 24 % "
                + "moisture, not below the warning threshold of 20 %. For now the reading shows no lack of water in the "
                + "soil.");
    }

    @Test
    @DisplayName("irrigate never answers from a virtual probe or without a reading")
    void irrigateNeedsARealProbe() {
        assertThat(AnswerComposer.compose(QuestionId.CLIMATE_IRRIGATE,
                climate(null, List.of(virtualProbe()), reading(14.0)), PLOT)).isEmpty();
        assertThat(AnswerComposer.compose(QuestionId.CLIMATE_IRRIGATE,
                climate(null, List.of(realProbe("WARNING")), null), PLOT)).isEmpty();
    }

    @Test
    @DisplayName("fruitset_effect: the hottest forecast day and the days at the threshold, no effect estimated")
    void fruitsetEffect() {
        var es = answer(QuestionId.CLIMATE_FRUITSET_EFFECT, climate(hotThenMild(), List.of(), null), "es");
        assertThat(es.text()).isEqualTo("El pronóstico llega a 35,1 °C el 31 de octubre y 3 de 7 días alcanzan el "
                + "umbral de calor de 32 °C. Viora aún no estima el efecto en el cuajado: evalúalo con tu técnico.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("35,1 °C", "máxima del pronóstico", "el 31 oct", FactTone.PRIMARY),
                new AnswerFact("3", "días al umbral de calor", "umbral 32 °C", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Pronóstico de Open-Meteo, actualizado el 30 de octubre");
        assertThat(answer(QuestionId.CLIMATE_FRUITSET_EFFECT, climate(hotThenMild(), List.of(), null), "en").text())
                .isEqualTo("The forecast reaches 35.1 °C on October 31 and 3 of 7 days reach the heat threshold of "
                        + "32 °C. Viora does not estimate the effect on fruit set yet: assess it with your technician.");

        var mild = climate(forecast(30.0, 31.0, 29.0, 30.0, 31.0, 28.5, 27.0), List.of(), null);
        assertThat(answer(QuestionId.CLIMATE_FRUITSET_EFFECT, mild, "es").text()).isEqualTo("La máxima del pronóstico "
                + "es 31 °C el 31 de octubre, bajo el umbral de calor de 32 °C. Viora aún no estima el efecto del clima "
                + "en el cuajado: evalúalo con tu técnico.");
        assertThat(AnswerComposer.compose(QuestionId.CLIMATE_FRUITSET_EFFECT, climate(null, List.of(), null), PLOT))
                .isEmpty();
    }

    @Test
    @DisplayName("heat_ends: ends, lasts beyond the forecast, or no heat at all")
    void heatEnds() {
        var es = answer(QuestionId.CLIMATE_HEAT_ENDS, climate(hotThenMild(), List.of(), null), "es");
        assertThat(es.text()).isEqualTo("El calor del pronóstico sigue hasta el 1 de noviembre: 3 de 7 días alcanzan "
                + "el umbral de 32 °C. Desde el 2 de noviembre las máximas quedan bajo el umbral.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("3", "días al umbral de calor", "umbral 32 °C", FactTone.PRIMARY),
                new AnswerFact("2 nov", "fin previsto del riesgo", "pronóstico de 7 días", FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.CLIMATE_HEAT_ENDS, climate(hotThenMild(), List.of(), null), "en").text())
                .isEqualTo("The forecast heat lasts until November 1: 3 of 7 days reach the threshold of 32 °C. From "
                        + "November 2 on, the maxima stay below the threshold.");

        var beyond = climate(forecast(30.0, 31.0, 33.0, 30.0, 31.0, 33.5, 34.0), List.of(), null);
        assertThat(answer(QuestionId.CLIMATE_HEAT_ENDS, beyond, "es").text()).isEqualTo("El calor sigue hasta el 5 de "
                + "noviembre, el último día del pronóstico: 3 de 7 días alcanzan el umbral de 32 °C. Aún no se ve "
                + "cuándo baja; vuelve a mirar el pronóstico en unos días.");
        var clear = climate(forecast(30.0, 31.0, 29.0, 30.0, 31.0, 28.5, 27.0), List.of(), null);
        var clearEn = answer(QuestionId.CLIMATE_HEAT_ENDS, clear, "en");
        assertThat(clearEn.text()).isEqualTo("No day of the next 7 reaches the heat threshold of 32 °C. The forecast "
                + "maxima stay below it.");
        assertThat(clearEn.facts().get(1).value()).isEqualTo("No risk");
    }

    @Test
    @DisplayName("sensors_say: a virtual node reading is labeled simulated data, in es and en")
    void sensorsSaySimulated() {
        var facts = climate(null, List.of(virtualProbe(), microclimate()), reading(21.54));

        var es = answer(QuestionId.CLIMATE_SENSORS_SAY, facts, "es");
        assertThat(es.text()).isEqualTo("La última lectura de tu lote marca 27,4 °C, 38 % de humedad del aire y 21,5 % "
                + "en el suelo: dato simulado. Sale del clima observado en tu zona, no de un sensor en el lote.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("27,4 °C", "temperatura del aire", "dato simulado", FactTone.PRIMARY),
                new AnswerFact("21,5 %", "humedad del suelo", "aire 38 %", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label()).containsExactly("Nodo virtual · dato simulado");

        var en = answer(QuestionId.CLIMATE_SENSORS_SAY, facts, "en");
        assertThat(en.text()).isEqualTo("The latest reading of your plot shows 27.4 °C, 38 % air humidity and 21.5 % in "
                + "the soil: simulated data. It comes from the weather observed in your area, not from a sensor on the "
                + "plot.");
        assertThat(en.sources()).extracting(source -> source.label()).containsExactly("Virtual node · simulated data");
    }

    @Test
    @DisplayName("sensors_say: measured readings are not labeled simulated; no nodes or reading, no answer")
    void sensorsSayMeasured() {
        var en = answer(QuestionId.CLIMATE_SENSORS_SAY, climate(null, List.of(realProbe("WARNING")), reading(21.0)),
                "en");
        assertThat(en.text()).isEqualTo("The latest reading of your node shows 27.4 °C, 38 % air humidity and 21 % in "
                + "the soil. It is the most recent reading of your plot.");
        assertThat(en.facts().get(0).pill()).isEqualTo("plot reading");
        assertThat(en.sources()).extracting(source -> source.label()).containsExactly("Based on 1 node of the plot");

        assertThat(AnswerComposer.compose(QuestionId.CLIMATE_SENSORS_SAY, climate(null, List.of(), reading(21.0)),
                PLOT)).isEmpty();
        assertThat(AnswerComposer.compose(QuestionId.CLIMATE_SENSORS_SAY,
                climate(null, List.of(realProbe("WARNING")), null), PLOT)).isEmpty();
    }

    @Test
    @DisplayName("the data version changes when the forecast or the reading changes")
    void dataVersion() {
        var version = AnswerComposer.dataVersionOf(ScreenKey.CLIMATE,
                climate(hotThenMild(), List.of(realProbe("WARNING")), reading(18.0)));

        assertThat(AnswerComposer.dataVersionOf(ScreenKey.CLIMATE,
                climate(hotThenMild(), List.of(realProbe("WARNING")), reading(18.0)))).isEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.CLIMATE,
                climate(hotThenMild(), List.of(realProbe("WARNING")), reading(17.0)))).isNotEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.CLIMATE,
                climate(forecast(30.0), List.of(realProbe("WARNING")), reading(18.0)))).isNotEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.CLIMATE,
                climate(hotThenMild(), List.of(realProbe("CRITICAL")), reading(18.0)))).isNotEqualTo(version);
    }

    private AnswerLocalizer.Localized answer(QuestionId question, ClimateFacts facts, String language) {
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
