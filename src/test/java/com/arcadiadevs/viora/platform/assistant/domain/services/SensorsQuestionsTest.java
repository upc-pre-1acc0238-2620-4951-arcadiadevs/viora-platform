package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.Coverage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.SensorsFacts;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator.Verdict;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static com.arcadiadevs.viora.platform.assistant.domain.services.SensorsSamples.TODAY;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SensorsSamples.coverage;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SensorsSamples.reading;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SensorsSamples.realMicroclimate;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SensorsSamples.realProbe;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SensorsSamples.sensors;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SensorsSamples.virtualMicroclimate;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SensorsSamples.virtualProbe;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@DisplayName("SensorsQuestions: the questions and answers of the sensors screen")
class SensorsQuestionsTest {

    private static final PlotId PLOT = new PlotId(UUID.randomUUID().toString());
    private static final AnswerLanguage ES = AnswerLanguage.SPANISH;
    private static final AnswerLanguage EN = new AnswerLanguage("en");
    private static final String STEPS_ES = "Para vincular un nodo, regístralo en el lote con un nombre que no repita "
            + "otro y su tipo: microclima, para el aire, o sonda de suelo, para la humedad. A la sonda indícale a "
            + "cuántos centímetros está instalada, de 1 a 200.";
    private static final String STEPS_EN = "To link a node, register it on the plot with a name no other node uses "
            + "and its type: microclimate, for the air, or soil probe, for the moisture. For a probe, give the depth "
            + "it is installed at in centimeters, from 1 to 200.";

    private final AnswerLocalizer localizer = new AnswerLocalizer(messages());

    @Test
    @DisplayName("SENSORS is a plot screen whose questions use the sensors. prefix")
    void screen() {
        assertThat(ScreenKey.fromValue("SENSORS")).isEqualTo(ScreenKey.SENSORS);
        assertThat(ScreenKey.SENSORS.requiresPlot()).isTrue();
        assertThat(ScreenKey.SENSORS.questionPrefix()).isEqualTo("sensors.");
        assertThat(List.of(QuestionId.SENSORS_WHAT_SAY, QuestionId.SENSORS_WHY_PAUSED, QuestionId.SENSORS_RELIABILITY,
                QuestionId.SENSORS_HOW_LINK)).allMatch(id -> id.screen() == ScreenKey.SENSORS);
        assertThat(sensors(List.of(), null, null).screen()).isEqualTo(ScreenKey.SENSORS);
    }

    @Test
    @DisplayName("catalog with nodes reading recently: every question enabled but why_paused, always disabled")
    void catalogWithNodesReading() {
        var questions = QuestionCatalog.questionsFor(sensors(List.of(virtualMicroclimate(), virtualProbe()),
                reading(), coverage(5)));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "sensors.what_say", "sensors.why_paused", "sensors.reliability", "sensors.how_link");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("sensor", "alert", "history", "lote");
        assertThat(questions).extracting(CatalogQuestion::label).containsExactly(
                CatalogText.of("assistant.question.sensors.what_say.label"),
                CatalogText.of("assistant.question.sensors.why_paused.label"),
                CatalogText.of("assistant.question.sensors.reliability.label"),
                CatalogText.of("assistant.question.sensors.how_link.label"));
        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(true, false, true, true);
        assertThat(questions).extracting(CatalogQuestion::disabledReason).containsExactly(
                null, CatalogText.of("assistant.reason.no_paused_nodes"), null, null);
    }

    @Test
    @DisplayName("catalog without nodes: only how_link is offered; nodes without a recent reading disable what_say")
    void catalogWithoutData() {
        var none = QuestionCatalog.questionsFor(sensors(List.of(), null, coverage(0)));
        assertThat(none).extracting(CatalogQuestion::enabled).containsExactly(false, false, false, true);
        assertThat(none).extracting(CatalogQuestion::disabledReason).containsExactly(
                CatalogText.of("assistant.reason.nodes_missing"),
                CatalogText.of("assistant.reason.no_paused_nodes"),
                CatalogText.of("assistant.reason.nodes_missing"),
                null);

        var stale = QuestionCatalog.questionsFor(sensors(List.of(realProbe()), null, coverage(3)));
        assertThat(stale).extracting(CatalogQuestion::enabled).containsExactly(false, false, true, true);
        assertThat(stale.get(0).disabledReason()).isEqualTo(CatalogText.of("assistant.reason.readings_missing"));
    }

    @Test
    @DisplayName("what_say with a virtual node: the reading is \"dato simulado\", in es and en")
    void whatSaySimulated() {
        // One measured node does not make the reading measured while another one is virtual
        var facts = sensors(List.of(realMicroclimate(), virtualProbe()), reading(), coverage(5));

        var es = answer(QuestionId.SENSORS_WHAT_SAY, facts, ES);
        assertThat(es.text()).isEqualTo("La última lectura de tu lote marca 27,4 °C, 38 % de humedad del aire "
                + "y 21,5 % en el suelo: dato simulado. Sale del clima observado en tu zona, no de un sensor en el "
                + "lote.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("27,4 °C", "temperatura del aire", "dato simulado", FactTone.PRIMARY),
                new AnswerFact("21,5 %", "humedad del suelo", "aire 38 %", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label()).containsExactly("Nodo virtual · dato simulado");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.SENSORS_WHAT_SAY, facts, EN);
        assertThat(en.text()).isEqualTo("The latest reading of your plot shows 27.4 °C, 38 % air humidity and 21.5 % "
                + "in the soil: simulated data. It comes from the weather observed in your area, not from a sensor on "
                + "the plot.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("27.4 °C", "air temperature", "simulated data", FactTone.PRIMARY),
                new AnswerFact("21.5 %", "soil moisture", "air 38 %", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label()).containsExactly("Virtual node · simulated data");
    }

    @Test
    @DisplayName("what_say with measured nodes: one node and several nodes, in es and en")
    void whatSayMeasured() {
        var two = sensors(List.of(realMicroclimate(), realProbe()), reading(), coverage(5));
        var es = answer(QuestionId.SENSORS_WHAT_SAY, two, ES);
        assertThat(es.text()).isEqualTo("La última lectura de tus 2 nodos marca 27,4 °C, 38 % de humedad del aire y "
                + "21,5 % en el suelo. Es la lectura más reciente de tu lote.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("27,4 °C", "temperatura del aire", "lectura del lote", FactTone.PRIMARY),
                new AnswerFact("21,5 %", "humedad del suelo", "aire 38 %", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label()).containsExactly("Basado en 2 nodos del lote");
        var en = answer(QuestionId.SENSORS_WHAT_SAY, two, EN);
        assertThat(en.text()).isEqualTo("The latest reading of your 2 nodes shows 27.4 °C, 38 % air humidity and "
                + "21.5 % in the soil. It is the most recent reading of "
                + "your plot.");
        assertThat(en.sources()).extracting(source -> source.label()).containsExactly("Based on 2 nodes of the plot");

        var one = sensors(List.of(realProbe()), reading(), coverage(5));
        assertThat(answer(QuestionId.SENSORS_WHAT_SAY, one, ES).text()).isEqualTo("La última lectura de tu nodo marca "
                + "27,4 °C, 38 % de humedad del aire y 21,5 % en el suelo. Es la lectura más reciente de tu lote.");
        assertThat(answer(QuestionId.SENSORS_WHAT_SAY, one, ES).sources()).extracting(source -> source.label())
                .containsExactly("Basado en 1 nodo del lote");
        assertThat(answer(QuestionId.SENSORS_WHAT_SAY, one, EN).text()).isEqualTo("The latest reading of your node "
                + "shows 27.4 °C, 38 % air humidity and 21.5 % in the soil. It is the most recent reading of "
                + "your plot.");
    }

    @Test
    @DisplayName("what_say never answers without a recent reading or without nodes")
    void whatSayNeedsNodesAndAReading() {
        assertThat(AnswerComposer.compose(QuestionId.SENSORS_WHAT_SAY,
                sensors(List.of(realProbe()), null, coverage(5)), PLOT)).isEmpty();
        assertThat(AnswerComposer.compose(QuestionId.SENSORS_WHAT_SAY,
                sensors(List.of(), reading(), coverage(5)), PLOT)).isEmpty();
    }

    @Test
    @DisplayName("reliability with virtual nodes: simulated, readings on 5 of the last 7 days, in es and en")
    void reliabilitySimulated() {
        var facts = sensors(List.of(virtualMicroclimate(), virtualProbe()), reading(), coverage(5));

        var es = answer(QuestionId.SENSORS_RELIABILITY, facts, ES);
        assertThat(es.text()).isEqualTo("Hoy tus lecturas son simuladas: salen del clima observado en tu zona, no de "
                + "un sensor en el lote, así que úsalas como referencia. En los últimos 7 días hubo lecturas "
                + "en 5 días; la última es del 30 de octubre.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("5/7", "días con lecturas", "últimos 7 días", FactTone.PRIMARY),
                new AnswerFact("Simulado", "origen de las lecturas", "dato simulado", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label()).containsExactly("Nodo virtual · dato simulado");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.SENSORS_RELIABILITY, facts, EN);
        assertThat(en.text()).isEqualTo("For now your readings are simulated: they come from the weather observed in "
                + "your area, not from a sensor on the plot, so use them as a reference. In the last 7 days there were "
                + "readings on 5 days; the latest is from October 30.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("5/7", "days with readings", "last 7 days", FactTone.PRIMARY),
                new AnswerFact("Simulated", "origin of the readings", "simulated data", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("reliability with measured nodes: one node, one day, in es and en; it needs no recent reading")
    void reliabilityMeasured() {
        var facts = sensors(List.of(realProbe()), null, new Coverage(TODAY.minusDays(3), 1, 7));

        var es = answer(QuestionId.SENSORS_RELIABILITY, facts, ES);
        assertThat(es.text()).isEqualTo("Tus lecturas salen de tu nodo en el lote. En los últimos 7 días hubo lecturas "
                + "en 1 día; la última es del 27 de octubre.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("1/7", "días con lecturas", "últimos 7 días", FactTone.PRIMARY),
                new AnswerFact("Medido", "origen de las lecturas", "lectura del lote", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label()).containsExactly("Basado en 1 nodo del lote");
        assertThat(answer(QuestionId.SENSORS_RELIABILITY, facts, EN).text()).isEqualTo("Your readings come from your "
                + "node on the plot. In the last 7 days there were readings on 1 day; the latest is from October 27.");

        var two = sensors(List.of(realMicroclimate(), realProbe()), reading(), coverage(7));
        assertThat(answer(QuestionId.SENSORS_RELIABILITY, two, ES).text()).isEqualTo("Tus lecturas salen de tus 2 "
                + "nodos en el lote. En los últimos 7 días hubo lecturas en 7 días; la última es del 30 de octubre.");
        assertThat(answer(QuestionId.SENSORS_RELIABILITY, two, EN).text()).isEqualTo("Your readings come from your 2 "
                + "nodes on the plot. In the last 7 days there were readings on 7 days; the latest is from "
                + "October 30.");
    }

    @Test
    @DisplayName("reliability without readings in the week says so, in es and en")
    void reliabilityWithoutReadings() {
        var facts = sensors(List.of(realProbe()), null, coverage(0));

        var es = answer(QuestionId.SENSORS_RELIABILITY, facts, ES);
        assertThat(es.text()).isEqualTo("Tus lecturas salen de tu nodo en el lote. En los últimos 7 días no hubo "
                + "lecturas.");
        assertThat(es.facts().get(0)).isEqualTo(
                new AnswerFact("0/7", "días con lecturas", "últimos 7 días", FactTone.PRIMARY));
        assertThat(answer(QuestionId.SENSORS_RELIABILITY, facts, EN).text()).isEqualTo("Your readings come from your "
                + "node on the plot. There were no readings in the last 7 days.");

        // Days counted without the day of the latest reading cannot name it, so they read as none
        var undated = sensors(List.of(virtualProbe()), null, new Coverage(null, 2, 7));
        assertThat(answer(QuestionId.SENSORS_RELIABILITY, undated, ES).text()).endsWith(
                "En los últimos 7 días no hubo lecturas.");
    }

    @Test
    @DisplayName("reliability never answers without nodes or without the coverage of the week")
    void reliabilityNeedsNodesAndCoverage() {
        assertThat(AnswerComposer.compose(QuestionId.SENSORS_RELIABILITY,
                sensors(List.of(), reading(), coverage(5)), PLOT)).isEmpty();
        assertThat(AnswerComposer.compose(QuestionId.SENSORS_RELIABILITY,
                sensors(List.of(realProbe()), reading(), null), PLOT)).isEmpty();
    }

    @Test
    @DisplayName("how_link without nodes: none linked yet and what Telemetry asks, no button, in es and en")
    void howLinkWithoutNodes() {
        var facts = sensors(List.of(), null, null);

        var es = answer(QuestionId.SENSORS_HOW_LINK, facts, ES);
        assertThat(es.text()).isEqualTo("Tu lote aún no tiene nodos vinculados. " + STEPS_ES);
        assertThat(es.facts()).containsExactly(
                new AnswerFact("0", "nodos de microclima", "en este lote", FactTone.PRIMARY),
                new AnswerFact("0", "sondas de suelo", "en este lote", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en los nodos vinculados del lote");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.SENSORS_HOW_LINK, facts, EN);
        assertThat(en.text()).isEqualTo("Your plot has no linked nodes yet. " + STEPS_EN);
        assertThat(en.facts()).containsExactly(
                new AnswerFact("0", "microclimate nodes", "on this plot", FactTone.PRIMARY),
                new AnswerFact("0", "soil probes", "on this plot", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label())
                .containsExactly("Based on the nodes linked to the plot");
        assertThat(en.actions()).isEmpty();
    }

    @Test
    @DisplayName("how_link with nodes counts the microclimate nodes and the soil probes, in es and en")
    void howLinkWithNodes() {
        var two = sensors(List.of(virtualMicroclimate(), virtualProbe()), reading(), coverage(5));
        var es = answer(QuestionId.SENSORS_HOW_LINK, two, ES);
        assertThat(es.text()).isEqualTo("Tu lote tiene 2 nodos vinculados. " + STEPS_ES);
        assertThat(es.facts()).containsExactly(
                new AnswerFact("1", "nodo de microclima", "en este lote", FactTone.PRIMARY),
                new AnswerFact("1", "sonda de suelo", "en este lote", FactTone.HIGHLIGHT));
        var en = answer(QuestionId.SENSORS_HOW_LINK, two, EN);
        assertThat(en.text()).isEqualTo("Your plot has 2 linked nodes. " + STEPS_EN);
        assertThat(en.facts()).containsExactly(
                new AnswerFact("1", "microclimate node", "on this plot", FactTone.PRIMARY),
                new AnswerFact("1", "soil probe", "on this plot", FactTone.HIGHLIGHT));

        var three = sensors(List.of(realProbe(), virtualProbe(), realProbe()), null, null);
        var threeEs = answer(QuestionId.SENSORS_HOW_LINK, three, ES);
        assertThat(threeEs.text()).startsWith("Tu lote tiene 3 nodos vinculados.");
        assertThat(threeEs.facts()).extracting(AnswerFact::value, AnswerFact::label)
                .containsExactly(tuple("0", "nodos de microclima"), tuple("3", "sondas de suelo"));
        var one = sensors(List.of(realMicroclimate()), null, null);
        assertThat(answer(QuestionId.SENSORS_HOW_LINK, one, ES).text()).startsWith("Tu lote tiene 1 nodo vinculado.");
        assertThat(answer(QuestionId.SENSORS_HOW_LINK, one, EN).text()).startsWith("Your plot has 1 linked node.");
    }

    @Test
    @DisplayName("why_paused has no answer, and a question of another screen has none here")
    void whyPausedHasNoAnswer() {
        var facts = sensors(List.of(virtualProbe()), reading(), coverage(5));

        assertThat(AnswerComposer.compose(QuestionId.SENSORS_WHY_PAUSED, facts, PLOT)).isEmpty();
        assertThat(SensorsQuestions.answer(QuestionId.SENSORS_WHY_PAUSED, facts)).isEmpty();
        assertThat(SensorsQuestions.answer(QuestionId.CLIMATE_SENSORS_SAY, facts)).isEmpty();
    }

    @Test
    @DisplayName("the canonical form changes with the nodes, the reading and the coverage")
    void canonical() {
        var base = sensors(List.of(virtualProbe()), reading(), coverage(5));

        assertThat(base.canonical()).startsWith("SENSORS|")
                .isEqualTo(sensors(List.of(virtualProbe()), reading(), coverage(5)).canonical());
        assertThat(base.canonical()).isNotEqualTo(sensors(List.of(realProbe()), reading(), coverage(5)).canonical())
                .isNotEqualTo(sensors(List.of(virtualProbe()), null, coverage(5)).canonical())
                .isNotEqualTo(sensors(List.of(virtualProbe()), reading(), coverage(4)).canonical());
        assertThat(new SensorsFacts(null, null, null).nodes()).isEmpty();
    }

    @Test
    @DisplayName("every variant resolves in es and en, has two cards and passes the validator as its own rephrasing")
    void everyTemplatePassesAsItsOwnRephrasing() {
        var scenarios = List.of(
                sensors(List.of(virtualMicroclimate(), virtualProbe()), reading(), coverage(5)),
                sensors(List.of(realMicroclimate(), realProbe()), reading(), coverage(1)),
                sensors(List.of(realProbe()), null, coverage(0)),
                sensors(List.of(), null, null));
        var questions = List.of(QuestionId.SENSORS_WHAT_SAY, QuestionId.SENSORS_WHY_PAUSED,
                QuestionId.SENSORS_RELIABILITY, QuestionId.SENSORS_HOW_LINK);
        var variants = new HashSet<String>();
        for (var facts : scenarios) {
            for (var question : questions) {
                var draft = AnswerComposer.compose(question, facts, PLOT);
                if (draft.isEmpty()) {
                    continue;
                }
                variants.add(draft.get().text().key() + draft.get().text().args().toString());
                for (var language : List.of(ES, EN)) {
                    var answer = localizer.localize(draft.get(), language);
                    var where = question.value() + " [" + language.code() + "]: " + answer.text();
                    var texts = new ArrayList<>(List.of(answer.text()));
                    answer.facts().forEach(fact -> texts.addAll(List.of(fact.value(), fact.label(), fact.pill())));
                    answer.sources().forEach(source -> texts.add(source.label()));
                    assertThat(texts).as(where).allSatisfy(text -> assertThat(text).doesNotContain("assistant.")
                            .doesNotContain("{").doesNotContain("}").isNotBlank());
                    assertThat(answer.facts()).as(where).hasSize(2);
                    assertThat(AnswerTextValidator.validate(answer.text(), answer.text(), answer.facts(), language))
                            .as(where).isEqualTo(Verdict.ACCEPTED);
                }
            }
        }
        // what_say: simulated, measured; reliability: simulated + days, measured + days, measured + none;
        // how_link: 2 nodes (twice), 1 node, none
        assertThat(variants).hasSize(8);
    }

    private AnswerLocalizer.Localized answer(QuestionId question, SensorsFacts facts, AnswerLanguage language) {
        var draft = AnswerComposer.compose(question, facts, PLOT);
        assertThat(draft).as(question.value()).isPresent();
        assertThat(draft.get().facts()).hasSize(2);
        return localizer.localize(draft.get(), language);
    }

    private static ResourceBundleMessageSource messages() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
