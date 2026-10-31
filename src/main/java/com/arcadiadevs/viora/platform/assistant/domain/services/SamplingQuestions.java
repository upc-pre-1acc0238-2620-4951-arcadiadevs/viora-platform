package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ActionDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.SamplingFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

import static com.arcadiadevs.viora.platform.assistant.domain.services.PlotDetailQuestions.samplingSource;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.THINNING_EXECUTE_ROUTE;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.label;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.question;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.round;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.shared;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.text;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.whole;

/**
 * The questions of the sampling summary screen (P54, P142-P144): which ones it offers and why one is disabled, and
 * the template each one is answered with.
 */
public final class SamplingQuestions {

    /**
     * Distinct trees Thinning needs for a representative sampling ({@code SamplingCoverageEvaluator
     * .MIN_REPRESENTATIVE_TREES}); a test keeps both equal. Thinning only exposes the trees still missing, so the
     * minimum is shown from here.
     */
    static final int MIN_REPRESENTATIVE_TREES = 5;

    private SamplingQuestions() {
    }

    /**
     * Evaluates the questions of the screen against the figures of the plot. Every question needs a tree sampled this
     * campaign; what the load means needs the issued prescription. {@code correct_tree} is always disabled: Thinning
     * cannot correct a sampled tree yet, and sampling it again adds its counts to the load a second time
     * (docs/asistente-decisiones.md, Pantalla Resumen de muestreo).
     *
     * @param facts the figures of the plot
     * @return the questions in display order
     */
    public static List<CatalogQuestion> catalog(SamplingFacts facts) {
        var samplingMissing = facts.sampled() ? null : CatalogText.of("assistant.reason.sampling_missing");
        CatalogText meaningMissing = samplingMissing;
        if (meaningMissing == null && !facts.plan().hasLoads()) {
            meaningMissing = CatalogText.of("assistant.reason.plan_missing");
        }
        return List.of(
                question(QuestionId.SAMPLING_ENOUGH_TREES, label(QuestionId.SAMPLING_ENOUGH_TREES, "label"), "lote",
                        samplingMissing),
                question(QuestionId.SAMPLING_MEANING, label(QuestionId.SAMPLING_MEANING, "label"), "fruit",
                        meaningMissing),
                question(QuestionId.SAMPLING_NEXT, label(QuestionId.SAMPLING_NEXT, "label"), "calendar",
                        samplingMissing),
                question(QuestionId.SAMPLING_CORRECT_TREE, label(QuestionId.SAMPLING_CORRECT_TREE, "label"),
                        "pruning", CatalogText.of("assistant.reason.tree_correction_unavailable")));
    }

    /**
     * Writes the answer to a question of the screen. {@code correct_tree} has no answer.
     *
     * @param questionId the question
     * @param facts      the figures of the plot
     * @param plotId     the plot, for the route of the button
     * @return the answer, or empty when the question is not of the screen or its figures are missing
     */
    public static Optional<AnswerDraft> answer(QuestionId questionId, SamplingFacts facts, PlotId plotId) {
        if (!facts.sampled()) {
            return Optional.empty();
        }
        if (questionId.equals(QuestionId.SAMPLING_ENOUGH_TREES)) {
            return Optional.of(enoughTrees(facts));
        } else if (questionId.equals(QuestionId.SAMPLING_MEANING)) {
            return meaning(facts);
        } else if (questionId.equals(QuestionId.SAMPLING_NEXT)) {
            return Optional.of(next(facts, plotId));
        }
        return Optional.empty();
    }

    /**
     * "¿Son suficientes árboles?": the trees sampled this campaign against the minimum Thinning asks for a
     * representative sampling and, once the prescription is issued, the load against the target (contract: "5/5 y
     * 0,59 frutos por brote vs objetivo 0,40; mínimo 5 árboles").
     */
    private static AnswerDraft enoughTrees(SamplingFacts facts) {
        var q = "sampling.enough_trees";
        var plan = facts.plan();
        int trees = plan.sampledTrees();
        int needed = facts.treesNeeded();
        final CatalogText text;
        final FactDraft loadCard;
        if (needed > 0) {
            text = text(q, "text.short", trees, MIN_REPRESENTATIVE_TREES, needed);
            loadCard = noPlanCard(trees);
        } else if (plan.hasLoads()) {
            double load = round(plan.preThinningFruitsPerShoot(), 2);
            double target = round(plan.targetFruitsPerShoot(), 2);
            text = text(q, "text.enough", trees, MIN_REPRESENTATIVE_TREES, load, target);
            loadCard = new FactDraft(shared("value.decimal", load), shared("card.load.label"),
                    shared("card.target.pill", target), FactTone.HIGHLIGHT);
        } else {
            text = text(q, "text.enough.unissued", trees, MIN_REPRESENTATIVE_TREES);
            loadCard = noPlanCard(trees);
        }
        return new AnswerDraft(text, List.of(treesCard(plan, needed), loadCard), samplingSource(plan), List.of());
    }

    /**
     * "¿Qué significa este resultado?": the measured load against the load the tree can sustain, how many times it is
     * over it, and the percentage the prescription removes for that. Needs the issued prescription.
     */
    private static Optional<AnswerDraft> meaning(SamplingFacts facts) {
        var q = "sampling.meaning";
        var plan = facts.plan();
        if (!plan.hasLoads()) {
            return Optional.empty();
        }
        double load = round(plan.preThinningFruitsPerShoot(), 2);
        double target = round(plan.targetFruitsPerShoot(), 2);
        if (target <= 0) {
            return Optional.empty();
        }
        // From the figures shown, in decimal, so 0,59 against 0,40 reads 1,48 as the producer would work it out
        double ratio = BigDecimal.valueOf(load).divide(BigDecimal.valueOf(target), 2, RoundingMode.HALF_UP)
                .doubleValue();
        long percent = whole(plan.percentageToRemove());
        var text = plan.preThinningFruitsPerShoot() > plan.targetFruitsPerShoot()
                ? text(q, "text.over", load, target, ratio, percent)
                : text(q, "text.within", load, target, percent);
        return Optional.of(new AnswerDraft(text, List.of(
                new FactDraft(shared("value.decimal", load), shared("card.load.label"),
                        shared("card.target.pill", target), FactTone.PRIMARY),
                new FactDraft(shared("value.percent", percent), shared("card.percent_to_remove.label"),
                        shared("card.per_plan.pill"), FactTone.HIGHLIGHT)),
                samplingSource(plan), List.of()));
    }

    /**
     * "¿Qué hago ahora?": the next step after the sampling, by where the thinning stands: sample the trees still
     * missing, wait for the window, thin while it is open (with the "Registrar aclareo" button, as on PLAN), or
     * nothing once the thinning is recorded.
     */
    private static AnswerDraft next(SamplingFacts facts, PlotId plotId) {
        var q = "sampling.next";
        var plan = facts.plan();
        var today = facts.today();
        var state = ThinningWindow.of(today, plan);
        List<ActionDraft> actions = List.of();
        final CatalogText text = switch (state) {
            case SAMPLING, NONE -> facts.treesNeeded() > 0
                    ? text(q, "text.sample", facts.treesNeeded(), MIN_REPRESENTATIVE_TREES)
                    : text(q, "text.unissued");
            case BEFORE -> text(q, "text.before", TextDate.of(plan.windowOpensOn()),
                    whole(plan.percentageToRemove()));
            case OPEN -> {
                actions = List.of(new ActionDraft(AnswerAction.DEEPLINK, shared("action.register_thinning"),
                        THINNING_EXECUTE_ROUTE + plotId.plotId()));
                yield text(q, "text.open", ThinningWindow.daysLeft(today, plan), TextDate.of(plan.windowClosesOn()),
                        whole(plan.percentageToRemove()));
            }
            case CLOSED -> text(q, "text.closed", TextDate.of(plan.windowClosesOn()));
            case DONE -> text(q, "text.done", TextDate.of(plan.executedOn()));
        };
        return new AnswerDraft(text, List.of(treesCard(plan, facts.treesNeeded()), state.card(today, plan)),
                samplingSource(plan), actions);
    }

    /** "5/5 · árboles muestreados · muestra representativa" or "… · faltan 2". */
    private static FactDraft treesCard(ThinningPlanData plan, int needed) {
        return new FactDraft(shared("value.fraction", plan.sampledTrees(), MIN_REPRESENTATIVE_TREES),
                shared("card.sampled_trees.label", plan.sampledTrees()),
                needed == 0 ? shared("card.representative.pill") : shared("card.trees_missing.pill", needed),
                FactTone.PRIMARY);
    }

    private static FactDraft noPlanCard(int trees) {
        return new FactDraft(shared("card.no_plan.value"), shared("card.no_plan.label"),
                shared("card.sampling.pill", trees), FactTone.HIGHLIGHT);
    }
}
