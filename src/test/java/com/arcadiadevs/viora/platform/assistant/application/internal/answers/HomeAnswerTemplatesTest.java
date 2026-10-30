package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.TextNumbers;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeAlertsAffectingFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeCampaignVsPreviousFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeHarvestEstimateFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeWeekPlanFactsAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.issuedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.plan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.forecast;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.frost;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.heat;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.hotThenMild;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateDataSamples.hydric;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every HOME template, for one plot and for the field, rendered in Spanish and English: every number is a fact,
 * the text fits the answer and passes the validator the model text goes through.
 */
@DisplayName("HOME answer templates: every number is a fact, in es and en")
class HomeAnswerTemplatesTest {

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final Locale EN = Locale.forLanguageTag("en");
    private static final List<FactsAssembler> ASSEMBLERS = List.of(new HomeWeekPlanFactsAssembler(),
            new HomeCampaignVsPreviousFactsAssembler(), new HomeAlertsAffectingFactsAssembler(),
            new HomeHarvestEstimateFactsAssembler());

    private final AnswerRenderer renderer = new AnswerRenderer(messageSource());

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
        // Week plan: 1 text and 15 sentences; alerts: 4; campaign: 6 texts and 3 closings; harvest: 5
        assertThat(variants).hasSize(34);
        assertThat(rendered).isGreaterThan(60);
    }

    @Test
    @DisplayName("Reads as written for a plot and for the field")
    void readsAsWritten() {
        var plot = renderer.render(new HomeWeekPlanFactsAssembler().assemble(onePlot(full(issuedPlan(), 0,
                List.of(heat()), null, hotThenMild()))).orElseThrow(), ES);
        assertThat(plot.text()).isEqualTo("Tu ventana de aclareo está abierta hasta el 10 de noviembre y el plan "
                + "indica quitar el 30 % de los frutos. La máxima de la semana, 35,1 °C el 31 de octubre, alcanza el "
                + "umbral de calor de 32 °C. Tienes 1 alerta activa, la más grave de golpe de calor en nivel aviso.");
        assertThat(plot.facts()).extracting(fact -> fact.value() + " | " + fact.label() + " | " + fact.pill())
                .containsExactly("35,1 °C | máxima de la semana | el 31 oct", "1 | alerta activa | en este lote");

        var field = renderer.render(new HomeHarvestEstimateFactsAssembler().assemble(field(
                withPlan(estimated(8213.6), 0), withPlan(estimated(9100.2), 0), bare())).orElseThrow(), ES);
        assertThat(field.text()).isEqualTo("En 2 de tus 3 lotes, Viora estima entre 8.214 y 9.100 kg por hectárea "
                + "tras el aclareo. A 1 lote aún le falta la estimación.");
        assertThat(field.facts()).extracting(fact -> fact.value() + " | " + fact.label() + " | " + fact.pill())
                .containsExactly("8.214–9.100 | kg por hectárea estimados | en calibración",
                        "2 | lotes con estimación | de 3 lotes activos");
        assertThat(renderer.render(new HomeAlertsAffectingFactsAssembler().assemble(field(withAlerts(heat()),
                withAlerts(frost()), bare())).orElseThrow(), EN).text()).isEqualTo("You have 2 open alerts on 2 of "
                + "your 3 plots and the most severe one is of frost at critical level: -2.4 °C against the threshold "
                + "of -2 °C. Open it to follow its mitigation steps.");
    }

    private static List<PlotAnswerData> scenarios() {
        var opening = plan(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20), null);
        var closesFirst = plan(LocalDate.of(2026, 10, 25), LocalDate.of(2026, 11, 5), null);
        var mild = forecast(30.0, 31.0, 29.0, 30.0, 31.0, 28.5, 27.0);
        return List.of(
                onePlot(full(issuedPlan(), 0, List.of(heat()), harvests(2023, 9600, 3100, 8300), hotThenMild())),
                onePlot(full(opening, 0, List.of(heat(), hydric()), harvests(2024, 5000, 4000), mild)),
                onePlot(full(halfway(), 1, List.of(), harvests(2024, 5000, 5000), null)),
                onePlot(full(halfway(), 4, List.of(), null, null)),
                onePlot(withYields(harvests(2024, 4000, 5000), null, null)),
                onePlot(withPlan(estimated(8213.6), 0)),
                onePlot(withPlan(estimated(null), 0)),
                field(withPlan(issuedPlan(), 0), withPlan(closesFirst, 0), withPlan(halfway(), 2),
                        full(null, 0, List.of(heat(), frost()), harvests(2024, 1000, 3000), hotThenMild()),
                        withAlerts(hydric())),
                field(withPlan(issuedPlan(), 0), bare()),
                field(withPlan(opening, 0), withPlan(opening, 0), withForecast(mild)),
                field(withPlan(opening, 0), bare()),
                field(withPlan(halfway(), 2), withPlan(halfway(), 1), withAlerts(heat())),
                field(withPlan(halfway(), 2), bare()),
                field(bare(), bare(), withAlerts(heat())),
                field(withYields(harvests(2024, 1000, 3000), null, null), withYields(harvests(2025, 2000), null, null),
                        withYields(harvests(2023, 4000, 4000, 4000), null, null)),
                field(withYields(harvests(2024, 5000, 4000), null, null), withYields(harvests(2024, 1000, 500), null,
                        null)),
                field(withYields(harvests(2024, 3000, 3000), null, null), withYields(harvests(2024, 2000, 2000),
                        null, null)),
                field(withPlan(estimated(8213.6), 0), withPlan(estimated(9100.2), 0), bare()),
                field(withPlan(estimated(8000.0), 0), withPlan(estimated(8000.4), 0)),
                field(withPlan(estimated(8000.0), 0), withPlan(estimated(null), 0), bare()),
                field(withPlan(estimated(null), 0), bare()),
                field(withPlan(estimated(null), 0), withPlan(estimated(null), 0), bare()),
                field());
    }

    /** The text key and every nested sentence key. */
    private static void collectVariants(CatalogText text, HashSet<String> variants) {
        variants.add(text.key());
        text.args().forEach(arg -> {
            if (arg instanceof CatalogText nested && nested.key().startsWith("assistant.template.home.")) {
                collectVariants(nested, variants);
            }
        });
    }

    private static ResourceBundleMessageSource messageSource() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
