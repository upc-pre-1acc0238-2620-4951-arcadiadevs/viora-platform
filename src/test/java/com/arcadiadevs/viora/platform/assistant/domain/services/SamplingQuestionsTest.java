package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.SamplingFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator.Verdict;
import com.arcadiadevs.viora.platform.thinning.domain.services.SamplingCoverageEvaluator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static com.arcadiadevs.viora.platform.assistant.domain.services.SamplingSamples.TODAY;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SamplingSamples.beforeWindow;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SamplingSamples.closedWindow;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SamplingSamples.done;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SamplingSamples.halfway;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SamplingSamples.issued;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SamplingSamples.representative;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SamplingSamples.sampling;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SamplingSamples.unissued;
import static com.arcadiadevs.viora.platform.assistant.domain.services.SamplingSamples.within;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SamplingQuestions: the questions and answers of the sampling summary screen")
class SamplingQuestionsTest {

    private static final PlotId PLOT = new PlotId(UUID.randomUUID().toString());
    private static final AnswerLanguage ES = AnswerLanguage.SPANISH;
    private static final AnswerLanguage EN = new AnswerLanguage("en");
    private static final List<QuestionId> QUESTIONS = List.of(QuestionId.SAMPLING_ENOUGH_TREES,
            QuestionId.SAMPLING_MEANING, QuestionId.SAMPLING_NEXT, QuestionId.SAMPLING_CORRECT_TREE);
    private static final CatalogText CORRECTION_UNAVAILABLE =
            CatalogText.of("assistant.reason.tree_correction_unavailable");
    private static final CatalogText SAMPLING_MISSING = CatalogText.of("assistant.reason.sampling_missing");

    private final AnswerLocalizer localizer = new AnswerLocalizer(messages());

    @Test
    @DisplayName("SAMPLING_SUMMARY is a plot screen whose questions use the sampling. prefix")
    void screen() {
        assertThat(ScreenKey.fromValue("SAMPLING_SUMMARY")).isEqualTo(ScreenKey.SAMPLING_SUMMARY);
        assertThat(ScreenKey.SAMPLING_SUMMARY.requiresPlot()).isTrue();
        assertThat(ScreenKey.SAMPLING_SUMMARY.questionPrefix()).isEqualTo("sampling.");
        assertThat(QUESTIONS).allSatisfy(id -> assertThat(id.screen()).isEqualTo(ScreenKey.SAMPLING_SUMMARY));
        assertThat(sampling(representative(), 0).screen()).isEqualTo(ScreenKey.SAMPLING_SUMMARY);
    }

    @Test
    @DisplayName("the minimum shown is the one Thinning uses for a representative sampling")
    void minimumMatchesThinning() {
        assertThat(SamplingQuestions.MIN_REPRESENTATIVE_TREES)
                .isEqualTo(SamplingCoverageEvaluator.MIN_REPRESENTATIVE_TREES);
    }

    @Test
    @DisplayName("the figures need the day the window is judged against")
    void requiresToday() {
        assertThatThrownBy(() -> new SamplingFacts(null, representative(), 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("assistant.sampling_summary.invalid");
    }

    @Test
    @DisplayName("catalog with the issued prescription: three questions enabled, correct_tree always disabled")
    void catalogWithPrescription() {
        var questions = QuestionCatalog.questionsFor(sampling(representative(), 0));

        assertThat(questions).extracting(q -> q.questionId().value()).containsExactly(
                "sampling.enough_trees", "sampling.meaning", "sampling.next", "sampling.correct_tree");
        assertThat(questions).extracting(CatalogQuestion::icon).containsExactly("lote", "fruit", "calendar",
                "pruning");
        assertThat(questions).extracting(CatalogQuestion::label).containsExactly(
                CatalogText.of("assistant.question.sampling.enough_trees.label"),
                CatalogText.of("assistant.question.sampling.meaning.label"),
                CatalogText.of("assistant.question.sampling.next.label"),
                CatalogText.of("assistant.question.sampling.correct_tree.label"));
        assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(true, true, true, false);
        assertThat(questions).extracting(CatalogQuestion::disabledReason)
                .containsExactly(null, null, null, CORRECTION_UNAVAILABLE);
    }

    @Test
    @DisplayName("catalog with a sampling but no prescription: meaning disabled, \"Sin plan de aclareo activo\"")
    void catalogWithoutPrescription() {
        for (var facts : List.of(sampling(halfway(3), 2), sampling(unissued(), 0))) {
            var questions = QuestionCatalog.questionsFor(facts);

            assertThat(questions).extracting(CatalogQuestion::enabled).containsExactly(true, false, true, false);
            assertThat(questions).extracting(CatalogQuestion::disabledReason).containsExactly(null,
                    CatalogText.of("assistant.reason.plan_missing"), null, CORRECTION_UNAVAILABLE);
        }
    }

    @Test
    @DisplayName("catalog without a tree sampled this campaign: every question disabled, nothing answered")
    void catalogWithoutSampling() {
        var noTree = new ThinningPlanData(false, null, null, null, null, null, 0, null, null, null, null, null, null);
        for (var facts : List.of(sampling(null, 5), sampling(noTree, 5))) {
            var questions = QuestionCatalog.questionsFor(facts);

            assertThat(facts.sampled()).isFalse();
            assertThat(questions).noneMatch(CatalogQuestion::enabled);
            assertThat(questions).extracting(CatalogQuestion::disabledReason).containsExactly(
                    SAMPLING_MISSING, SAMPLING_MISSING, SAMPLING_MISSING, CORRECTION_UNAVAILABLE);
            assertThat(QUESTIONS).allSatisfy(id -> assertThat(AnswerComposer.compose(id, facts, PLOT)).isEmpty());
        }
    }

    @Test
    @DisplayName("correct_tree has no answer, whatever the sampling")
    void correctTreeNeverAnswers() {
        for (var facts : List.of(sampling(representative(), 0), sampling(halfway(3), 2), sampling(done(), 0))) {
            assertThat(AnswerComposer.compose(QuestionId.SAMPLING_CORRECT_TREE, facts, PLOT)).isEmpty();
        }
    }

    @Test
    @DisplayName("enough_trees, contract example: 5/5 and 0,59 fruits per shoot against 0,40, in es and en")
    void enoughTrees() {
        var facts = sampling(representative(), 0);

        var es = answer(QuestionId.SAMPLING_ENOUGH_TREES, facts, ES);
        assertThat(es.text()).isEqualTo("Sí: muestreaste 5 árboles y el mínimo es 5. La muestra da 0,59 frutos por "
                + "brote frente al objetivo de 0,40.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("5/5", "árboles muestreados", "muestra representativa", FactTone.PRIMARY),
                new AnswerFact("0,59", "frutos por brote · estimado", "objetivo 0,40", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 5 árboles, ronda del 30 de octubre");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.SAMPLING_ENOUGH_TREES, facts, EN);
        assertThat(en.text()).isEqualTo("Yes: you sampled 5 trees and the minimum is 5. The sample gives 0.59 fruits "
                + "per shoot against a target of 0.40.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("5/5", "trees sampled", "representative sample", FactTone.PRIMARY),
                new AnswerFact("0.59", "fruits per shoot · estimated", "target 0.40", FactTone.HIGHLIGHT));
        assertThat(en.sources()).extracting(source -> source.label())
                .containsExactly("Based on 5 trees, round of October 30");
    }

    @Test
    @DisplayName("enough_trees halfway: the trees missing for the plan, in es and en")
    void enoughTreesHalfway() {
        var facts = sampling(halfway(3), 2);

        var es = answer(QuestionId.SAMPLING_ENOUGH_TREES, facts, ES);
        assertThat(es.text()).isEqualTo("Aún no: muestreaste 3 árboles y el mínimo para una muestra representativa "
                + "es 5. Faltan 2 árboles para calcular el plan de aclareo.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("3/5", "árboles muestreados", "faltan 2", FactTone.PRIMARY),
                new AnswerFact("Sin plan", "aclareo de esta campaña", "3 árboles muestreados", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 3 árboles, ronda del 31 de octubre");

        var en = answer(QuestionId.SAMPLING_ENOUGH_TREES, facts, EN);
        assertThat(en.text()).isEqualTo("Not yet: you sampled 3 trees and the minimum for a representative sample is "
                + "5. 2 more trees are needed to calculate the thinning plan.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("3/5", "trees sampled", "2 missing", FactTone.PRIMARY),
                new AnswerFact("No plan", "thinning this campaign", "3 sampled trees", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("enough_trees halfway in the singular: one tree sampled, one tree missing")
    void enoughTreesSingular() {
        var one = answer(QuestionId.SAMPLING_ENOUGH_TREES, sampling(halfway(1), 4), ES);
        assertThat(one.text()).isEqualTo("Aún no: muestreaste 1 árbol y el mínimo para una muestra representativa es "
                + "5. Faltan 4 árboles para calcular el plan de aclareo.");
        assertThat(one.facts().get(0)).isEqualTo(new AnswerFact("1/5", "árbol muestreado", "faltan 4",
                FactTone.PRIMARY));
        assertThat(one.facts().get(1).pill()).isEqualTo("1 árbol muestreado");
        assertThat(answer(QuestionId.SAMPLING_ENOUGH_TREES, sampling(halfway(1), 4), EN).text()).isEqualTo("Not yet: "
                + "you sampled 1 tree and the minimum for a representative sample is 5. 4 more trees are needed to "
                + "calculate the thinning plan.");

        var lastOne = answer(QuestionId.SAMPLING_ENOUGH_TREES, sampling(halfway(4), 1), ES);
        assertThat(lastOne.text()).isEqualTo("Aún no: muestreaste 4 árboles y el mínimo para una muestra "
                + "representativa es 5. Falta 1 árbol para calcular el plan de aclareo.");
        assertThat(lastOne.facts().get(0).pill()).isEqualTo("falta 1");
        assertThat(answer(QuestionId.SAMPLING_ENOUGH_TREES, sampling(halfway(4), 1), EN).text()).isEqualTo("Not yet: "
                + "you sampled 4 trees and the minimum for a representative sample is 5. 1 more tree is needed to "
                + "calculate the thinning plan.");
    }

    @Test
    @DisplayName("enough_trees with a representative sampling and no prescription yet, in es and en")
    void enoughTreesUnissued() {
        var facts = sampling(unissued(), 0);

        var es = answer(QuestionId.SAMPLING_ENOUGH_TREES, facts, ES);
        assertThat(es.text()).isEqualTo("Sí: muestreaste 5 árboles y el mínimo es 5. El plan de aclareo aún no se "
                + "calcula con esta muestra.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("5/5", "árboles muestreados", "muestra representativa", FactTone.PRIMARY),
                new AnswerFact("Sin plan", "aclareo de esta campaña", "5 árboles muestreados", FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.SAMPLING_ENOUGH_TREES, facts, EN).text()).isEqualTo("Yes: you sampled 5 trees "
                + "and the minimum is 5. The thinning plan is not calculated with this sample yet.");
    }

    @Test
    @DisplayName("meaning over the target: 1,48 times the load the tree can sustain, remove 32 %, in es and en")
    void meaningOver() {
        var facts = sampling(representative(), 0);

        var es = answer(QuestionId.SAMPLING_MEANING, facts, ES);
        assertThat(es.text()).isEqualTo("Tus árboles cargan 0,59 frutos por brote y el plan apunta a 0,40, la carga "
                + "que el árbol puede sostener: hoy llevan 1,48 veces esa carga. Por eso el plan indica quitar el 32 % "
                + "de los frutos.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("0,59", "frutos por brote · estimado", "objetivo 0,40", FactTone.PRIMARY),
                new AnswerFact("32 %", "de los frutos a quitar", "según el plan", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 5 árboles, ronda del 30 de octubre");
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.SAMPLING_MEANING, facts, EN);
        assertThat(en.text()).isEqualTo("Your trees carry 0.59 fruits per shoot and the plan aims at 0.40, the load "
                + "the tree can sustain: today they carry 1.48 times that load. That is why the plan says to remove "
                + "32 % of the fruits.");
        assertThat(en.facts()).containsExactly(
                new AnswerFact("0.59", "fruits per shoot · estimated", "target 0.40", FactTone.PRIMARY),
                new AnswerFact("32%", "of the fruits to remove", "per the plan", FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("meaning within the target: the load the tree can sustain, in es and en")
    void meaningWithin() {
        var facts = sampling(within(), 0);

        var es = answer(QuestionId.SAMPLING_MEANING, facts, ES);
        assertThat(es.text()).isEqualTo("Tus árboles cargan 0,38 frutos por brote y el plan apunta a 0,40: la carga ya "
                + "está en lo que el árbol puede sostener, y el plan indica quitar el 0 % de los frutos.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("0 %", "de los frutos a quitar", "según el plan",
                FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.SAMPLING_MEANING, facts, EN).text()).isEqualTo("Your trees carry 0.38 fruits per "
                + "shoot and the plan aims at 0.40: the load is already what the tree can sustain, and the plan says "
                + "to remove 0 % of the fruits.");
    }

    @Test
    @DisplayName("meaning is never answered without the issued prescription")
    void meaningNeedsThePrescription() {
        assertThat(AnswerComposer.compose(QuestionId.SAMPLING_MEANING, sampling(halfway(3), 2), PLOT)).isEmpty();
        assertThat(AnswerComposer.compose(QuestionId.SAMPLING_MEANING, sampling(unissued(), 0), PLOT)).isEmpty();
    }

    @Test
    @DisplayName("next halfway: sample the trees still missing, no button, in es and en")
    void nextSample() {
        var facts = sampling(halfway(3), 2);

        var es = answer(QuestionId.SAMPLING_NEXT, facts, ES);
        assertThat(es.text()).isEqualTo("Muestrea 2 árboles más para llegar al mínimo de 5 y calcular el plan de "
                + "aclareo.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("3/5", "árboles muestreados", "faltan 2", FactTone.PRIMARY),
                new AnswerFact("Sin plan", "aclareo de esta campaña", "3 árboles muestreados", FactTone.HIGHLIGHT));
        assertThat(es.actions()).isEmpty();
        assertThat(answer(QuestionId.SAMPLING_NEXT, facts, EN).text()).isEqualTo("Sample 2 more trees to reach the "
                + "minimum of 5 and calculate the thinning plan.");

        assertThat(answer(QuestionId.SAMPLING_NEXT, sampling(halfway(4), 1), ES).text()).isEqualTo("Muestrea 1 árbol "
                + "más para llegar al mínimo de 5 y calcular el plan de aclareo.");
        assertThat(answer(QuestionId.SAMPLING_NEXT, sampling(halfway(4), 1), EN).text()).isEqualTo("Sample 1 more "
                + "tree to reach the minimum of 5 and calculate the thinning plan.");
    }

    @Test
    @DisplayName("next with enough trees and no prescription yet: check the plan screen, in es and en")
    void nextUnissued() {
        var facts = sampling(unissued(), 0);

        var es = answer(QuestionId.SAMPLING_NEXT, facts, ES);
        assertThat(es.text()).isEqualTo("Tu muestreo ya alcanza el mínimo, pero el plan de aclareo aún no se calcula: "
                + "revisa en la pantalla del plan qué le falta.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("5/5", "árboles muestreados", "muestra representativa", FactTone.PRIMARY),
                new AnswerFact("Sin plan", "aclareo de esta campaña", "5 árboles muestreados", FactTone.HIGHLIGHT));
        assertThat(es.actions()).isEmpty();
        assertThat(answer(QuestionId.SAMPLING_NEXT, facts, EN).text()).isEqualTo("Your sampling already reaches the "
                + "minimum, but the thinning plan is not calculated yet: check on the plan screen what it is "
                + "missing.");
    }

    @Test
    @DisplayName("next before the window: wait for it to open, in es and en")
    void nextBefore() {
        var facts = sampling(beforeWindow(), 0);

        var es = answer(QuestionId.SAMPLING_NEXT, facts, ES);
        assertThat(es.text()).isEqualTo("Espera a que se abra la ventana de aclareo el 5 de noviembre; el plan indica "
                + "quitar el 32 % de los frutos.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("5", "días para abrir", "abre el 5 nov",
                FactTone.HIGHLIGHT));
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.SAMPLING_NEXT, facts, EN);
        assertThat(en.text()).isEqualTo("Wait for the thinning window to open on November 5; the plan says to remove "
                + "32 % of the fruits.");
        assertThat(en.facts().get(1)).isEqualTo(new AnswerFact("5", "days until it opens", "opens Nov 5",
                FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("next with the window open: thin, the days left and the \"Registrar aclareo\" button, in es and en")
    void nextOpen() {
        var facts = sampling(representative(), 0);

        var es = answer(QuestionId.SAMPLING_NEXT, facts, ES);
        assertThat(es.text()).isEqualTo("Aclarea: la ventana está abierta hasta el 11 de noviembre (quedan 12 días) y "
                + "el plan indica quitar el 32 % de los frutos. Cuando termines, registra el aclareo.");
        assertThat(es.facts()).containsExactly(
                new AnswerFact("5/5", "árboles muestreados", "muestra representativa", FactTone.PRIMARY),
                new AnswerFact("12", "días de ventana", "ventana abierta", FactTone.HIGHLIGHT));
        assertThat(es.sources()).extracting(source -> source.label())
                .containsExactly("Basado en 5 árboles, ronda del 30 de octubre");
        assertThat(es.actions()).containsExactly(new AnswerAction("DEEPLINK", "Registrar aclareo",
                "thinning/execute?plotId=" + PLOT.plotId()));

        var en = answer(QuestionId.SAMPLING_NEXT, facts, EN);
        assertThat(en.text()).isEqualTo("Thin: the window is open until November 11 (12 days left) and the plan says "
                + "to remove 32 % of the fruits. When you finish, record the thinning.");
        assertThat(en.facts().get(1)).isEqualTo(new AnswerFact("12", "days of window left", "window open",
                FactTone.HIGHLIGHT));
        assertThat(en.actions()).containsExactly(new AnswerAction("DEEPLINK", "Record thinning",
                "thinning/execute?plotId=" + PLOT.plotId()));
    }

    @Test
    @DisplayName("next on the last day of the window: one day left, in es and en")
    void nextOpenLastDay() {
        var facts = sampling(issued(0.59, 0.40, 32.0, LocalDate.of(2026, 10, 21), TODAY, null), 0);

        var es = answer(QuestionId.SAMPLING_NEXT, facts, ES);
        assertThat(es.text()).isEqualTo("Aclarea: la ventana está abierta hasta el 31 de octubre (queda 1 día) y el "
                + "plan indica quitar el 32 % de los frutos. Cuando termines, registra el aclareo.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("1", "día de ventana", "ventana abierta",
                FactTone.HIGHLIGHT));
        assertThat(answer(QuestionId.SAMPLING_NEXT, facts, EN).text()).isEqualTo("Thin: the window is open until "
                + "October 31 (1 day left) and the plan says to remove 32 % of the fruits. When you finish, record "
                + "the thinning.");
    }

    @Test
    @DisplayName("next after the window closed without a thinning: check with the technician, in es and en")
    void nextClosed() {
        var facts = sampling(closedWindow(), 0);

        var es = answer(QuestionId.SAMPLING_NEXT, facts, ES);
        assertThat(es.text()).isEqualTo("Tu ventana de aclareo cerró el 20 de octubre. Si ya aclareaste, regístralo; "
                + "si no, consúltalo con tu técnico antes de hacerlo fuera de las fechas del plan.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("Cerrada", "ventana de aclareo", "cerró el 20 oct",
                FactTone.HIGHLIGHT));
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.SAMPLING_NEXT, facts, EN);
        assertThat(en.text()).isEqualTo("Your thinning window closed on October 20. If you already thinned, record "
                + "it; if not, check with your technician before thinning outside the dates of the plan.");
        assertThat(en.facts().get(1)).isEqualTo(new AnswerFact("Closed", "thinning window", "closed Oct 20",
                FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("next once the thinning is recorded: nothing else to do, in es and en")
    void nextDone() {
        var facts = sampling(done(), 0);

        var es = answer(QuestionId.SAMPLING_NEXT, facts, ES);
        assertThat(es.text()).isEqualTo("Ya registraste el aclareo el 25 de octubre: el muestreo de esta campaña no "
                + "pide nada más.");
        assertThat(es.facts().get(1)).isEqualTo(new AnswerFact("Registrado", "aclareo de esta campaña", "el 25 oct",
                FactTone.HIGHLIGHT));
        assertThat(es.actions()).isEmpty();

        var en = answer(QuestionId.SAMPLING_NEXT, facts, EN);
        assertThat(en.text()).isEqualTo("You already recorded the thinning on October 25: the sampling of this "
                + "campaign asks for nothing else.");
        assertThat(en.facts().get(1)).isEqualTo(new AnswerFact("Recorded", "thinning this campaign", "on Oct 25",
                FactTone.HIGHLIGHT));
    }

    @Test
    @DisplayName("a sampling without its date is cited by its trees only")
    void undatedSource() {
        var plan = new ThinningPlanData(false, null, 0.57, null, null, null, 3, null, null, null, null, null, null);

        assertThat(answer(QuestionId.SAMPLING_ENOUGH_TREES, sampling(plan, 2), ES).sources())
                .extracting(source -> source.label()).containsExactly("Basado en 3 árboles muestreados");
        assertThat(answer(QuestionId.SAMPLING_ENOUGH_TREES, sampling(plan, 2), EN).sources())
                .extracting(source -> source.label()).containsExactly("Based on 3 sampled trees");
    }

    @Test
    @DisplayName("every variant resolves in es and en, has two cards and passes the validator as its own rephrasing")
    void everyTemplatePassesAsItsOwnRephrasing() {
        var variants = new HashSet<String>();
        for (var facts : List.of(sampling(representative(), 0), sampling(within(), 0), sampling(beforeWindow(), 0),
                sampling(closedWindow(), 0), sampling(done(), 0), sampling(halfway(3), 2),
                sampling(unissued(), 0))) {
            for (var question : QUESTIONS) {
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
                    answer.actions().forEach(action -> texts.add(action.label()));
                    assertThat(texts).as(where).allSatisfy(text -> assertThat(text).doesNotContain("assistant.")
                            .doesNotContain("{").doesNotContain("}").isNotBlank());
                    assertThat(answer.facts()).as(where).hasSize(2);
                    assertThat(AnswerTextValidator.validate(answer.text(), answer.text(), answer.facts(), language))
                            .as(where).isEqualTo(Verdict.ACCEPTED);
                }
            }
        }
        // enough_trees: 3, meaning: 2, next: 6
        assertThat(variants).hasSize(11);
    }

    @Test
    @DisplayName("the data version changes with the day, the plan and the trees missing")
    void dataVersion() {
        var version = AnswerComposer.dataVersionOf(ScreenKey.SAMPLING_SUMMARY, sampling(representative(), 0));

        assertThat(AnswerComposer.dataVersionOf(ScreenKey.SAMPLING_SUMMARY, sampling(representative(), 0)))
                .isEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.SAMPLING_SUMMARY, sampling(within(), 0)))
                .isNotEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.SAMPLING_SUMMARY, sampling(representative(), 1)))
                .isNotEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.SAMPLING_SUMMARY,
                new SamplingFacts(TODAY.plusDays(1), representative(), 0))).isNotEqualTo(version);
        assertThat(AnswerComposer.dataVersionOf(ScreenKey.SAMPLING_SUMMARY, sampling(null, 5)))
                .isNotEqualTo(version);
    }

    private AnswerLocalizer.Localized answer(QuestionId question, SamplingFacts facts, AnswerLanguage language) {
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
