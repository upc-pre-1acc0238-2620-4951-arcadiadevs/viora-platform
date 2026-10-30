package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.QuestionCatalog;
import com.arcadiadevs.viora.platform.assistant.domain.services.TextNumbers;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillIfIncompleteFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillMissingFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillReachFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillVsLastWinterFactsAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every WINTER_CHILL template and catalog text, rendered in Spanish and English: every number is a fact, the text
 * fits the answer and passes the validator the model text goes through.
 */
@DisplayName("WINTER_CHILL answer templates: every number is a fact, in es and en")
class ChillAnswerTemplatesTest {

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final Locale EN = Locale.forLanguageTag("en");
    private static final List<FactsAssembler> ASSEMBLERS = List.of(new ChillMissingFactsAssembler(),
            new ChillReachFactsAssembler(), new ChillVsLastWinterFactsAssembler(),
            new ChillIfIncompleteFactsAssembler());

    private final ResourceBundleMessageSource messageSource = messageSource();
    private final AnswerRenderer renderer = new AnswerRenderer(messageSource);

    @Test
    @DisplayName("Every variant: numbers backed by facts, at most 280 characters and 3 sentences, nothing unresolved")
    void everyNumberIsAFact() {
        var variants = new HashSet<String>();
        int rendered = 0;
        for (var data : scenarios()) {
            for (var assembler : ASSEMBLERS) {
                var facts = assembler.assemble(data);
                if (facts.isEmpty()) {
                    continue;
                }
                collectVariants(facts.get().text(), variants);
                for (var locale : List.of(ES, EN)) {
                    var answer = renderer.render(facts.get(), locale);
                    var where = facts.get().text().key() + " [" + locale + "]: " + answer.text();
                    assertThat(TextNumbers.unbackedNumbers(answer.text(), locale, facts.get().facts())).as(where)
                            .isEmpty();
                    var modelFacts = ModelFacts.of(new AnswerDraft(assembler.questionId(), locale,
                            facts.get().facts(), answer.text(), List.of()));
                    assertThat(AnswerTextValidator.check(answer.text(), locale, modelFacts)).as(where).isEmpty();
                    var texts = new ArrayList<String>(List.of(answer.text()));
                    answer.facts().forEach(fact -> texts.addAll(List.of(fact.value(), fact.label(), fact.pill())));
                    answer.sources().forEach(source -> texts.add(source.label()));
                    assertThat(texts).as(where).allSatisfy(text -> assertThat(text).doesNotContain("assistant.")
                            .doesNotContain("{").doesNotContain("}").isNotBlank());
                    assertThat(answer.facts()).hasSize(2);
                    rendered++;
                }
            }
        }
        // missing: 5 texts; reach: 4; vs last winter: 3 texts and 2 closings; if incomplete: 5
        assertThat(variants).hasSize(19);
        assertThat(rendered).isGreaterThan(40);
    }

    @Test
    @DisplayName("Every catalog label and reason resolves in es and en")
    void catalogTexts() {
        var datasets = List.of(chill(inProgress(), lastWinter()), chill(closedShort(), lastWinter()),
                chill(insufficient(), null), ChillData.NONE);
        for (var chill : datasets) {
            for (var question : QuestionCatalog.questionsFor(ScreenKey.WINTER_CHILL, PlotFacts.ofChill(2026, chill))) {
                for (var locale : List.of(ES, EN)) {
                    assertThat(resolve(question.label(), locale)).doesNotContain("{").isNotBlank();
                    if (question.disabledReason() != null) {
                        assertThat(resolve(question.disabledReason(), locale)).doesNotContain("{").isNotBlank();
                    }
                }
            }
        }
        var labels = QuestionCatalog.questionsFor(ScreenKey.WINTER_CHILL,
                PlotFacts.ofChill(2026, chill(inProgress(), lastWinter())));
        assertThat(labels).extracting(question -> resolve(question.label(), ES)).containsExactly(
                "¿Cuánto frío me falta?", "¿Cuándo llego a las 30 porciones?",
                "¿Cómo va frente al invierno pasado?", "¿Qué pasa si no llego a las 30 porciones?");
        assertThat(resolve(QuestionCatalog.questionsFor(ScreenKey.WINTER_CHILL,
                PlotFacts.ofChill(2026, chill(closedShort(), lastWinter()))).get(2).label(), EN))
                .isEqualTo("How did the 2026 winter compare with 2025?");
    }

    @Test
    @DisplayName("Reads as written: 24,4 of 30 and the estimated date against the budbreak")
    void readsAsWritten() {
        var missing = renderer.render(new ChillMissingFactsAssembler().assemble(data(inProgress(), lastWinter()))
                .orElseThrow(), ES);
        assertThat(missing.text()).isEqualTo("Tu lote lleva 24,4 de 30 porciones de frío este invierno: le faltan "
                + "5,6. Están contadas hasta el 20 de julio.");
        assertThat(missing.facts()).extracting(fact -> fact.value() + " | " + fact.label() + " | " + fact.pill())
                .containsExactly("24,4/30 | porciones de frío | hasta el 20 jul",
                        "5,6 | porciones por completar | requisito 30");
        assertThat(missing.sources()).extracting(source -> source.label()).containsExactly(
                "Modelo de frío de Erez con el clima horario de Open-Meteo, hasta el 20 de julio");

        var reach = renderer.render(new ChillReachFactsAssembler().assemble(data(inProgress(), lastWinter()))
                .orElseThrow(), ES);
        assertThat(reach.text()).isEqualTo("Al ritmo reciente, tu lote llegaría a las 30 porciones el 4 de agosto, "
                + "27 días antes de la brotación del 31 de agosto. Es una estimación: una racha de días cálidos la "
                + "puede retrasar.");
        assertThat(reach.facts()).extracting(fact -> fact.value() + " | " + fact.label() + " | " + fact.pill())
                .containsExactly("4 ago | fecha estimada | lleva 24,4", "31 ago | brotación | 27 días de margen");

        assertThat(renderer.render(new ChillVsLastWinterFactsAssembler().assemble(data(inProgress(),
                lastWinter())).orElseThrow(), EN).text()).isEqualTo("By July 20 your plot has 24.4 portions; last "
                + "winter, on the same date, it had 18. That winter reached the 30 portions on August 10.");
    }

    private static List<PlotAnswerData> scenarios() {
        var gap = season(2025, 33.1, "OFF_SEASON", "NOT_APPLICABLE", null, LocalDate.of(2025, 8, 10),
                LocalDate.of(2025, 7, 1), 5);
        return List.of(
                data(inProgress(), lastWinter()),
                data(inProgress(), lastWinterShort()),
                data(inProgress(), gap),
                data(halted(), lastWinterShort()),
                data(notReachable(), lastWinter()),
                data(insufficient(), lastWinter()),
                data(completedInSeason(), lastWinter()),
                data(closedShort(), lastWinter()),
                data(closedCompleted(), lastWinterShort()),
                data(notEvaluated(), lastWinter()));
    }

    /** The text key and every nested sentence key. */
    private static void collectVariants(CatalogText text, HashSet<String> variants) {
        variants.add(text.key());
        text.args().forEach(arg -> {
            if (arg instanceof CatalogText nested && nested.key().startsWith("assistant.template.chill.")) {
                collectVariants(nested, variants);
            }
        });
    }

    private String resolve(CatalogText text, Locale locale) {
        return messageSource.getMessage(text.key(), text.args().toArray(), locale);
    }

    private static ResourceBundleMessageSource messageSource() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
