package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ActionDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlanFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.THINNING_EXECUTE_ROUTE;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.THINNING_PLAN_ROUTE;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.label;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.percentOf;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.question;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.round;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.text;

/**
 * The questions of the thinning plan screen (P61, P120-P122): which ones it offers and why one is disabled, and the
 * template each one is answered with.
 */
public final class PlanQuestions {

    /** Points the {@code plan.if_less} scenario thins less than the prescription. */
    static final int LESS_THINNING_POINTS = 10;

    private PlanQuestions() {
    }

    /**
     * Evaluates the questions of the screen against the figures of the plot.
     *
     * @param facts the figures of the plot
     * @return the questions in display order
     */
    public static List<CatalogQuestion> catalog(PlanFacts facts) {
        CatalogText reason = planMissing(facts);
        CatalogText whyPercent;
        CatalogText ifLess;
        if (reason == null) {
            int percent = (int) Math.round(facts.issuedPercentage());
            int less = Math.max(0, percent - LESS_THINNING_POINTS);
            whyPercent = label(QuestionId.PLAN_WHY_PERCENT, "label", percent);
            ifLess = less > 0
                    ? label(QuestionId.PLAN_IF_LESS, "label", less)
                    : label(QuestionId.PLAN_IF_LESS, "label_without_percent");
        } else {
            whyPercent = label(QuestionId.PLAN_WHY_PERCENT, "label_without_percent");
            ifLess = label(QuestionId.PLAN_IF_LESS, "label_without_percent");
        }
        return List.of(
                question(QuestionId.PLAN_WHY_PERCENT, whyPercent, "prio", reason),
                question(QuestionId.PLAN_IF_LESS, ifLess, "pruning", reason),
                question(QuestionId.PLAN_IF_NONE, label(QuestionId.PLAN_IF_NONE, "label"), "alert", reason),
                question(QuestionId.PLAN_BEST_TIME, label(QuestionId.PLAN_BEST_TIME, "label"), "calendar", reason));
    }

    /**
     * Writes the answer to a question of the screen.
     *
     * @param questionId the question
     * @param facts      the figures of the plot
     * @param plotId     the plot, for the routes of the buttons
     * @return the answer, or empty when the question is not of the screen or its figures are missing
     */
    public static Optional<AnswerDraft> answer(QuestionId questionId, PlanFacts facts, PlotId plotId) {
        if (questionId.equals(QuestionId.PLAN_WHY_PERCENT)) {
            return whyPercent(facts, plotId);
        } else if (questionId.equals(QuestionId.PLAN_IF_LESS)) {
            return ifLess(facts, plotId);
        } else if (questionId.equals(QuestionId.PLAN_IF_NONE)) {
            return ifNone(facts, plotId);
        } else if (questionId.equals(QuestionId.PLAN_BEST_TIME)) {
            return bestTime(facts, plotId);
        }
        return Optional.empty();
    }

    /** Why the plan questions cannot be answered, or null when the prescription is issued. */
    private static CatalogText planMissing(PlanFacts facts) {
        if (facts.prescriptionIssued() && facts.issuedPercentage() != null) {
            return null;
        }
        if (facts.sampledTrees() > 0 && facts.treesNeeded() > 0) {
            return facts.treesNeeded() == 1
                    ? CatalogText.of("assistant.reason.trees_missing.one")
                    : CatalogText.of("assistant.reason.trees_missing.other", facts.treesNeeded());
        }
        return CatalogText.of("assistant.reason.plan_missing");
    }

    private static Optional<AnswerDraft> whyPercent(PlanFacts facts, PlotId plotId) {
        if (!hasPrescription(facts)) {
            return Optional.empty();
        }
        var q = "plan.why_percent";
        var load = round(facts.preThinningFruitsPerShoot(), 2);
        var target = round(facts.targetFruitsPerShoot(), 2);
        var percent = (int) Math.round(facts.percentageToRemove());
        return Optional.of(new AnswerDraft(
                text(q, "text", load, target, percent),
                List.of(
                        new FactDraft(text(q, "fact1.value", load), text(q, "fact1.label"),
                                text(q, "fact1.pill", target), FactTone.PRIMARY),
                        new FactDraft(text(q, "fact2.value", percent), text(q, "fact2.label"),
                                facts.windowClosesOn() == null ? text(q, "fact2.pill_plain")
                                        : text(q, "fact2.pill", facts.windowClosesOn()), FactTone.HIGHLIGHT)),
                planSource(facts),
                List.of(new ActionDraft(AnswerAction.DEEPLINK, text("action", "register_thinning"),
                        THINNING_EXECUTE_ROUTE + plotId.plotId()))));
    }

    private static Optional<AnswerDraft> ifLess(PlanFacts facts, PlotId plotId) {
        if (!hasPrescription(facts)) {
            return Optional.empty();
        }
        var q = "plan.if_less";
        var prescribed = (int) Math.round(facts.percentageToRemove());
        var thinned = Math.max(0, prescribed - LESS_THINNING_POINTS);
        var residualLoad = facts.preThinningFruitsPerShoot() * (1 - thinned / 100.0);
        var excess = percentOf(residualLoad, facts.targetFruitsPerShoot());
        if (excess <= 0) {
            return Optional.empty();
        }
        var target = round(facts.targetFruitsPerShoot(), 2);
        return Optional.of(new AnswerDraft(
                text(q, "text", thinned, round(residualLoad, 2), excess, target),
                List.of(
                        new FactDraft(text(q, "fact1.value", round(residualLoad, 2)), text(q, "fact1.label", thinned),
                                text(q, "fact1.pill", target), FactTone.PRIMARY),
                        new FactDraft(text(q, "fact2.value", excess), text(q, "fact2.label"),
                                text(q, "fact2.pill", prescribed), FactTone.HIGHLIGHT)),
                planSource(facts),
                List.of(new ActionDraft(AnswerAction.DEEPLINK, text("action", "view_plan"),
                        THINNING_PLAN_ROUTE + plotId.plotId()))));
    }

    private static Optional<AnswerDraft> ifNone(PlanFacts facts, PlotId plotId) {
        if (!hasPrescription(facts)) {
            return Optional.empty();
        }
        var q = "plan.if_none";
        var excess = percentOf(facts.preThinningFruitsPerShoot(), facts.targetFruitsPerShoot());
        if (excess <= 0) {
            return Optional.empty();
        }
        var load = round(facts.preThinningFruitsPerShoot(), 2);
        var target = round(facts.targetFruitsPerShoot(), 2);
        return Optional.of(new AnswerDraft(
                text(q, "text", load, excess, target),
                List.of(
                        new FactDraft(text(q, "fact1.value", load), text(q, "fact1.label"),
                                text(q, "fact1.pill", target), FactTone.PRIMARY),
                        new FactDraft(text(q, "fact2.value", excess), text(q, "fact2.label"),
                                text(q, "fact2.pill"), FactTone.HIGHLIGHT)),
                planSource(facts),
                List.of(new ActionDraft(AnswerAction.DEEPLINK, text("action", "view_plan"),
                        THINNING_PLAN_ROUTE + plotId.plotId()))));
    }

    /**
     * The window is told by its dates and its length, never by "days left", so the answer does not go out of date
     * as the days pass.
     */
    private static Optional<AnswerDraft> bestTime(PlanFacts facts, PlotId plotId) {
        var opens = facts.windowOpensOn();
        var closes = facts.windowClosesOn();
        if (!hasPrescription(facts) || opens == null || closes == null || closes.isBefore(opens)) {
            return Optional.empty();
        }
        var q = "plan.best_time";
        var days = (int) ChronoUnit.DAYS.between(opens, closes) + 1;
        var percent = (int) Math.round(facts.percentageToRemove());
        return Optional.of(new AnswerDraft(
                text(q, "text", opens, closes),
                List.of(
                        new FactDraft(text(q, "fact1.value", opens, closes), text(q, "fact1.label"),
                                text(q, "fact1.pill", days), FactTone.PRIMARY),
                        new FactDraft(text(q, "fact2.value", percent), text(q, "fact2.label"),
                                text(q, "fact2.pill", round(facts.targetFruitsPerShoot(), 2)), FactTone.HIGHLIGHT)),
                planSource(facts),
                List.of(new ActionDraft(AnswerAction.DEEPLINK, text("action", "register_thinning"),
                        THINNING_EXECUTE_ROUTE + plotId.plotId()))));
    }

    /** A prescription with something to remove: load, target and a percentage above zero. */
    private static boolean hasPrescription(PlanFacts facts) {
        return facts.preThinningFruitsPerShoot() != null && facts.targetFruitsPerShoot() != null
                && facts.percentageToRemove() != null && facts.percentageToRemove() > 0.0
                && facts.targetFruitsPerShoot() > 0.0 && facts.preThinningFruitsPerShoot() > 0.0;
    }

    private static CatalogText planSource(PlanFacts facts) {
        return facts.sampledTrees() > 0
                ? text("plan", "source", facts.sampledTrees())
                : text("plan", "source_plain");
    }
}
