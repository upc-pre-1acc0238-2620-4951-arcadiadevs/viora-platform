package com.arcadiadevs.viora.platform.assistant.application.internal.answers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AssembledFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotAnswerData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.TextNumbers;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.BestTimeFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.BreakCycleFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.IfLessFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.IfNoneFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.NextYearFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.VsCampaignsFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.WhyIndexFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.WhyPercentFactsAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.regex.Pattern;

import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every template of every question, rendered in Spanish and English over plot data reaching each variant. The
 * numbers a text shows must all be facts: the same rule the numeric validator will apply to the model.
 */
@DisplayName("Answer templates: every number is a fact, in es and en")
class AnswerTemplatesTest {

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final Locale EN = Locale.forLanguageTag("en");
    private static final List<FactsAssembler> ASSEMBLERS = List.of(new WhyIndexFactsAssembler(),
            new BreakCycleFactsAssembler(), new VsCampaignsFactsAssembler(), new NextYearFactsAssembler(),
            new WhyPercentFactsAssembler(), new IfLessFactsAssembler(), new IfNoneFactsAssembler(),
            new BestTimeFactsAssembler());

    private final AnswerRenderer renderer = new AnswerRenderer(messageSource());

    @Test
    @DisplayName("Every rendered number comes from the facts, texts are short and nothing is left unresolved")
    void everyNumberIsAFact() {
        var variants = new HashSet<String>();
        int rendered = 0;
        for (var data : scenarios()) {
            for (var assembler : ASSEMBLERS) {
                var facts = assembler.assemble(data);
                if (facts.isEmpty()) {
                    continue;
                }
                variants.add(facts.get().text().key());
                for (var locale : List.of(ES, EN)) {
                    assertRendered(facts.get(), locale);
                    rendered++;
                }
            }
        }
        // Every text variant of the eight questions was reached
        assertThat(variants).hasSize(16);
        assertThat(rendered).isGreaterThan(60);
    }

    @Test
    @DisplayName("Spanish uses decimal comma and tú; English decimal point")
    void readsAsTheMockup() {
        var facts = new WhyPercentFactsAssembler().assemble(plan(issuedPlan())).orElseThrow();

        var es = renderer.render(facts, ES);
        var en = renderer.render(facts, EN);

        assertThat(es.text()).isEqualTo("Tu carga está en 0,57 frutos por brote y el plan apunta a 0,40, la que el "
                + "árbol puede sostener. Por eso indica quitar el 30 % de los frutos.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("0,57", "frutos por brote · estimado", "objetivo 0,40",
                        com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone.PRIMARY),
                new AnswerFact("−30 %", "de los frutos · quedan 12 días", "ventana abierta",
                        com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 5 árboles, ronda del 29 de octubre");
        assertThat(es.actions()).extracting(action -> action.label()).containsExactly("Registrar aclareo");
        assertThat(en.text()).isEqualTo("Your load is 0.57 fruits per shoot and the plan aims at 0.40, what the tree "
                + "can sustain. That is why it calls for removing 30% of the fruits.");
        assertThat(en.sources()).extracting(source -> source.label())
                .containsExactly("Based on 5 trees, round of October 29");
    }

    private void assertRendered(AssembledFacts facts, Locale locale) {
        var answer = renderer.render(facts, locale);
        var where = facts.text().key() + " [" + locale + "]: " + answer.text();
        assertThat(TextNumbers.unbackedNumbers(answer.text(), locale, facts.facts())).as(where).isEmpty();
        assertThat(answer.text().length()).as(where).isLessThanOrEqualTo(280);
        long sentences = Pattern.compile("[.;](\\s|$)").matcher(answer.text()).results().count();
        assertThat(sentences).as(where).isBetween(2L, 3L);
        var texts = new ArrayList<String>(List.of(answer.text()));
        answer.facts().forEach(fact -> texts.addAll(List.of(fact.value(), fact.label(), fact.pill())));
        answer.sources().forEach(source -> texts.add(source.label()));
        answer.actions().forEach(action -> texts.add(action.label()));
        assertThat(texts).as(where).allSatisfy(text -> {
            assertThat(text).doesNotContain("assistant.").doesNotContain("{").doesNotContain("}").isNotBlank();
        });
        assertThat(answer.facts()).hasSize(2);
    }

    private static List<PlotAnswerData> scenarios() {
        var equal = new TreeMap<Integer, Double>();
        equal.put(2024, 5000.0);
        equal.put(2025, 5000.0);
        var below = new TreeMap<Integer, Double>();
        below.put(2024, 9600.0);
        below.put(2025, 3100.0);
        var light = new ThinningPlanData(true, 8.0, 0.43, 0.4, LocalDate.of(2026, 10, 20),
                LocalDate.of(2026, 11, 10), 1, null, null, null, null, null, null);
        return List.of(
                alternation(issuedPlan()),
                alternation((ThinningPlanData) null),
                alternation(thinnedPlan(8213.6)),
                alternation(thinnedPlan(null)),
                alternation(equal),
                alternation(below),
                plan(issuedPlan()),
                plan(plan(LocalDate.of(2026, 10, 20), TODAY, null)),
                plan(plan(LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 20), null)),
                plan(plan(LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 20), null)),
                plan(plan(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 20), null)),
                plan(plan(LocalDate.of(2026, 10, 20), LocalDate.of(2026, 11, 10), LocalDate.of(2026, 10, 28))),
                plan(light));
    }

    private static ResourceBundleMessageSource messageSource() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
