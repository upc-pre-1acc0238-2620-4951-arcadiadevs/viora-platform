package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AlternationFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlanFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnswerComposer: the deterministic answers of the assistant")
class AnswerComposerTest {

    private static final PlotId PLOT = new PlotId(UUID.randomUUID().toString());
    private static final List<QuestionId> ALL_QUESTIONS = List.of(
            QuestionId.ALTERNATION_WHY_INDEX, QuestionId.ALTERNATION_BREAK_CYCLE, QuestionId.ALTERNATION_VS_CAMPAIGNS,
            QuestionId.ALTERNATION_NEXT_YEAR, QuestionId.PLAN_WHY_PERCENT, QuestionId.PLAN_IF_LESS,
            QuestionId.PLAN_IF_NONE, QuestionId.PLAN_BEST_TIME);

    private final AnswerLocalizer localizer = new AnswerLocalizer(messages());

    /**
     * Three campaigns on 10 ha: 4.0, 10.0 and 5.0 t/ha, so the average is 6.3 and the last one (2025) is low.
     * The current campaign is 2026.
     */
    private static AlternationFacts alternationFacts() {
        var yields = new TreeMap<Integer, Double>();
        yields.put(2023, 40_000.0);
        yields.put(2024, 100_000.0);
        yields.put(2025, 50_000.0);
        return new AlternationFacts(2026, 10.0, yields, 0.50, "MODERATE_ALTERNATION", false);
    }

    /** A load of 0.60 fruits per shoot against a target of 0.40: 30 % to remove, between 3 and 14 November. */
    private static PlanFacts planFacts() {
        return new PlanFacts(2026, 0.60, 0.40, 30.0,
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 14), 5, false, null, 0);
    }

    private AnswerLocalizer.Localized answer(QuestionId question, ScreenFacts facts, String language) {
        var draft = AnswerComposer.compose(question, facts, PLOT);
        assertThat(draft).as(question.value()).isPresent();
        return localizer.localize(draft.get(), new AnswerLanguage(language));
    }

    @Test
    @DisplayName("why_index: the index, its band and the best and lowest campaign")
    void whyIndex() {
        var answer = answer(QuestionId.ALTERNATION_WHY_INDEX, alternationFacts(), "es");

        assertThat(answer.text()).isEqualTo("Tu índice de vecería es 0,50: vecería moderada. "
                + "Tu mejor campaña fue de 10,0 t/ha (2024) y la más baja de 4,0 t/ha (2023).");
        assertThat(answer.facts()).containsExactly(
                new AnswerFact("0,50", "índice de vecería", "vecería moderada", FactTone.PRIMARY),
                new AnswerFact("10,0 → 4,0", "t/ha · tu mejor y tu menor campaña", "2024 y 2023", FactTone.HIGHLIGHT));
        assertThat(answer.sources()).extracting(source -> source.label())
                .containsExactly("Basado en tus 3 campañas registradas");
        assertThat(answer.actions()).isEmpty();
    }

    @Test
    @DisplayName("break_cycle: after a low campaign the next one tends to carry more and the button opens the plan")
    void breakCycleAfterALowCampaign() {
        var answer = answer(QuestionId.ALTERNATION_BREAK_CYCLE, alternationFacts(), "es");

        assertThat(answer.text()).isEqualTo("Tu última campaña (2025) fue baja: 5,0 t/ha frente a un promedio de 6,3. "
                + "En 2026 el árbol tiende a cargar más fruta; aclarear cuando se abra la ventana evita que se "
                + "agote y repita el ciclo.");
        assertThat(answer.facts()).containsExactly(
                new AnswerFact("5,0", "t/ha · campaña 2025", "promedio 6,3 t/ha", FactTone.PRIMARY),
                new AnswerFact("-21 %", "frente a tu promedio", "año bajo", FactTone.HIGHLIGHT));
        assertThat(answer.actions()).singleElement().satisfies(action -> {
            assertThat(action.type()).isEqualTo("DEEPLINK");
            assertThat(action.label()).isEqualTo("Ver el plan de aclareo");
            assertThat(action.target()).isEqualTo("thinning/plan?plotId=" + PLOT.plotId());
        });
    }

    @Test
    @DisplayName("break_cycle: after a high campaign the button asks for a sampling instead")
    void breakCycleAfterAHighCampaign() {
        var yields = new TreeMap<Integer, Double>();
        yields.put(2024, 40_000.0);
        yields.put(2025, 120_000.0);
        var facts = new AlternationFacts(2026, 10.0, yields, 0.60, "SEVERE_ALTERNATION", false);

        var answer = answer(QuestionId.ALTERNATION_BREAK_CYCLE, facts, "es");

        assertThat(answer.text()).startsWith("Tu última campaña (2025) fue alta: 12,0 t/ha frente a un promedio de 8,0.");
        assertThat(answer.facts().get(1)).isEqualTo(
                new AnswerFact("+50 %", "frente a tu promedio", "año alto", FactTone.HIGHLIGHT));
        assertThat(answer.actions()).singleElement()
                .satisfies(action -> assertThat(action.target()).isEqualTo("thinning/sampling?plotId=" + PLOT.plotId()));
    }

    @Test
    @DisplayName("vs_campaigns: the last campaign against the average of the earlier ones")
    void versusCampaigns() {
        var answer = answer(QuestionId.ALTERNATION_VS_CAMPAIGNS, alternationFacts(), "es");

        assertThat(answer.text()).isEqualTo("Tu campaña 2025 fue de 5,0 t/ha, 29 % por debajo del promedio de tus "
                + "campañas anteriores (7,0 t/ha).");
        assertThat(answer.facts()).containsExactly(
                new AnswerFact("5,0", "t/ha · campaña 2025", "promedio previo 7,0 t/ha", FactTone.PRIMARY),
                new AnswerFact("-29 %", "frente a tus campañas anteriores", "2 previas", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("next_year: two years after a low campaign the pattern says low again, and it is a trend, not kilos")
    void nextYearFollowsTheParityOfTheYears() {
        var answer = answer(QuestionId.ALTERNATION_NEXT_YEAR, alternationFacts(), "es");

        assertThat(answer.text()).isEqualTo("Con tu historial, 2027 tiende a ser un año de carga baja: tus campañas "
                + "bajas rondaron 4,5 t/ha. Es una tendencia de tu lote, no una estimación de kilos.");
        assertThat(answer.facts()).containsExactly(
                new AnswerFact("4,5", "t/ha · tus campañas bajas", "tendencia 2027", FactTone.PRIMARY),
                new AnswerFact("5,0", "t/ha · tu última campaña", "2025", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("next_year: when the current campaign is already harvested, the next year is the opposite kind")
    void nextYearAfterTheCurrentCampaignIsHarvested() {
        var yields = new TreeMap<Integer, Double>();
        yields.put(2025, 100_000.0);
        yields.put(2026, 40_000.0);
        var facts = new AlternationFacts(2026, 10.0, yields, 0.60, "SEVERE_ALTERNATION", false);

        var answer = answer(QuestionId.ALTERNATION_NEXT_YEAR, facts, "es");

        assertThat(answer.text()).startsWith("Con tu historial, 2027 tiende a ser un año de carga alta: tus campañas "
                + "altas rondaron 10,0 t/ha.");
    }

    @Test
    @DisplayName("why_percent: the load, the target, the percentage and the end of the window")
    void whyPercent() {
        var answer = answer(QuestionId.PLAN_WHY_PERCENT, planFacts(), "es");

        assertThat(answer.text()).isEqualTo("Tu carga está en 0,60 frutos por brote y el árbol sostiene 0,40. "
                + "Quitar 30 % de los frutos la deja en el objetivo.");
        assertThat(answer.facts()).containsExactly(
                new AnswerFact("0,60", "frutos por brote · estimado", "objetivo 0,40", FactTone.PRIMARY),
                new AnswerFact("−30 %", "de los frutos", "ventana hasta el 14 nov", FactTone.HIGHLIGHT));
        assertThat(answer.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 5 árboles muestreados");
        assertThat(answer.actions()).singleElement().satisfies(action -> {
            assertThat(action.label()).isEqualTo("Registrar aclareo");
            assertThat(action.target()).isEqualTo("thinning/execute?plotId=" + PLOT.plotId());
        });
    }

    @Test
    @DisplayName("if_less: ten points less than prescribed leaves the load above the target")
    void ifLess() {
        var answer = answer(QuestionId.PLAN_IF_LESS, planFacts(), "es");

        assertThat(answer.text()).isEqualTo("Con 20 % quedarías en 0,48 frutos por brote, 20 % por encima de lo que "
                + "el árbol sostiene (0,40). Con esa carga la vecería puede alargarse.");
        assertThat(answer.facts()).containsExactly(
                new AnswerFact("0,48", "frutos por brote · con 20 %", "objetivo 0,40", FactTone.PRIMARY),
                new AnswerFact("+20 %", "sobre el objetivo", "prescrito 30 %", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("if_none: without thinning the load stays as sampled")
    void ifNone() {
        var answer = answer(QuestionId.PLAN_IF_NONE, planFacts(), "es");

        assertThat(answer.text()).isEqualTo("Sin aclarear te quedas en 0,60 frutos por brote, 50 % por encima de lo "
                + "que el árbol sostiene (0,40). Con esa carga se espera una cosecha más baja el año siguiente.");
        assertThat(answer.facts().get(1)).isEqualTo(
                new AnswerFact("+50 %", "sobre el objetivo", "sin aclareo", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("best_time: the window by its dates and length, never by days left")
    void bestTime() {
        var answer = answer(QuestionId.PLAN_BEST_TIME, planFacts(), "es");

        assertThat(answer.text()).isEqualTo("La ventana de aclareo va del 3 nov al 14 nov. Aclarear dentro de ese "
                + "rango, antes del endurecimiento del hueso, es lo recomendado.");
        assertThat(answer.facts()).containsExactly(
                new AnswerFact("3 nov – 14 nov", "ventana recomendada", "12 días", FactTone.PRIMARY),
                new AnswerFact("−30 %", "de los frutos a quitar", "objetivo 0,40", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("English uses the decimal point, the percent sign without a space and English month names")
    void english() {
        var whyPercent = answer(QuestionId.PLAN_WHY_PERCENT, planFacts(), "en");
        assertThat(whyPercent.text()).isEqualTo("Your load is 0.60 fruits per shoot and the tree sustains 0.40. "
                + "Removing 30% of the fruit brings it to the target.");
        assertThat(whyPercent.facts().get(1).pill()).isEqualTo("window until 14 Nov");

        var whyIndex = answer(QuestionId.ALTERNATION_WHY_INDEX, alternationFacts(), "en");
        assertThat(whyIndex.text()).startsWith("Your alternate bearing index is 0.50: moderate alternate bearing.");
    }

    @Test
    @DisplayName("every answer has exactly two cards and no message is left unresolved, in both languages")
    void everyAnswerIsComplete() {
        for (var language : List.of("es", "en")) {
            for (var question : ALL_QUESTIONS) {
                var facts = question.screen() == ScreenKey.ALTERNATION ? alternationFacts() : planFacts();
                var answer = answer(question, facts, language);

                var texts = new java.util.ArrayList<String>();
                texts.add(answer.text());
                answer.facts().forEach(fact -> texts.addAll(List.of(fact.value(), fact.label(), fact.pill())));
                answer.sources().forEach(source -> texts.add(source.label()));
                answer.actions().forEach(action -> texts.add(action.label()));

                assertThat(answer.facts()).as(question.value()).hasSize(2);
                assertThat(texts).as(question.value() + " " + language)
                        .noneMatch(text -> text.isBlank() || text.startsWith("assistant.") || text.contains("{"));
            }
        }
    }

    @Test
    @DisplayName("a question whose figures are missing has no answer, so the server never invents one")
    void withoutFiguresThereIsNoAnswer() {
        var emptyAlternation = new AlternationFacts(2026, 10.0, new TreeMap<>(), null, null, false);
        var emptyPlan = new PlanFacts(2026, null, null, null, null, null, 0, false, null, 0);

        for (var question : ALL_QUESTIONS) {
            ScreenFacts empty = question.screen() == ScreenKey.ALTERNATION ? emptyAlternation : emptyPlan;
            assertThat(AnswerComposer.compose(question, empty, PLOT)).as(question.value()).isEmpty();
        }
    }

    @Test
    @DisplayName("a prescription with nothing to remove, or a window that closes before it opens, has no answer")
    void degeneratePrescriptions() {
        var nothingToRemove = new PlanFacts(2026, 0.40, 0.40, 0.0,
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 14), 5, false, null, 0);
        assertThat(AnswerComposer.compose(QuestionId.PLAN_WHY_PERCENT, nothingToRemove, PLOT)).isEmpty();
        assertThat(AnswerComposer.compose(QuestionId.PLAN_IF_NONE, nothingToRemove, PLOT)).isEmpty();

        var backwards = new PlanFacts(2026, 0.60, 0.40, 30.0,
                LocalDate.of(2026, 11, 14), LocalDate.of(2026, 11, 3), 5, false, null, 0);
        assertThat(AnswerComposer.compose(QuestionId.PLAN_BEST_TIME, backwards, PLOT)).isEmpty();
    }

    @Test
    @DisplayName("without a closing date the window card says it follows the plan instead of failing")
    void windowWithoutClosingDate() {
        var facts = new PlanFacts(2026, 0.60, 0.40, 30.0, LocalDate.of(2026, 11, 3), null, 0, false, null, 0);

        var answer = answer(QuestionId.PLAN_WHY_PERCENT, facts, "es");

        assertThat(answer.facts().get(1).pill()).isEqualTo("según tu plan");
        assertThat(answer.sources()).extracting(source -> source.label()).containsExactly("Basado en tu plan de aclareo");
    }

    @Test
    @DisplayName("the data version changes exactly when a figure of the screen changes")
    void dataVersion() {
        var alternation = alternationFacts();
        var plan = planFacts();

        assertThat(AnswerComposer.dataVersionOf(ScreenKey.ALTERNATION, alternation))
                .isEqualTo(AnswerComposer.dataVersionOf(ScreenKey.ALTERNATION, alternationFacts()));
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.PLAN, plan))
                .isEqualTo(AnswerComposer.dataVersionOf(ScreenKey.PLAN, planFacts()));

        var moreYields = new TreeMap<>(alternation.yieldsKg());
        moreYields.put(2026, 30_000.0);
        var newHarvest = new AlternationFacts(2026, 10.0, moreYields, 0.50, "MODERATE_ALTERNATION", false);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.ALTERNATION, newHarvest))
                .isNotEqualTo(AnswerComposer.dataVersionOf(ScreenKey.ALTERNATION, alternation));

        var newPercentage = new PlanFacts(2026, 0.60, 0.40, 35.0,
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 14), 5, false, null, 0);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.PLAN, newPercentage))
                .isNotEqualTo(AnswerComposer.dataVersionOf(ScreenKey.PLAN, plan));

        // A harvest does not make the plan answers stale, nor a new prescription the alternation ones
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.PLAN, plan))
                .isEqualTo(AnswerComposer.dataVersionOf(ScreenKey.PLAN, plan));
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.ALTERNATION, alternation).value()).startsWith("sha256:");
    }

    private static ResourceBundleMessageSource messages() {
        var source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }
}
