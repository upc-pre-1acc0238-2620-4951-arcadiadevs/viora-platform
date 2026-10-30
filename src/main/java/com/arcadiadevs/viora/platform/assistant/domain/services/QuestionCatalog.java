package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;

import java.util.List;

/**
 * Owner of the closed question catalog: which questions each screen offers, in which order, with which icon,
 * and whether the data of the plot is enough to answer them. A question without enough data is offered disabled
 * with the reason, so the model is never asked to invent a figure.
 */
public final class QuestionCatalog {

    /** Points the {@code plan.if_less} scenario thins less than the prescription. */
    static final int LESS_THINNING_POINTS = 10;

    private static final String PREFIX = "assistant.question.";

    private QuestionCatalog() {
    }

    /**
     * Evaluates the questions of a screen against the data of the plot.
     *
     * @param screen the screen
     * @param facts  the data of the plot
     * @return the questions in display order
     */
    public static List<CatalogQuestion> questionsFor(ScreenKey screen, PlotFacts facts) {
        return switch (screen) {
            case ALTERNATION -> alternation(facts);
            case PLAN -> plan(facts);
        };
    }

    private static List<CatalogQuestion> alternation(PlotFacts facts) {
        boolean hasIndex = facts.bearingIndex() != null;
        var indexMissing = hasIndex ? null : CatalogText.of("assistant.reason.bbi_missing");
        var whyIndexLabel = hasIndex
                ? label(QuestionId.ALTERNATION_WHY_INDEX, "label", facts.bearingIndex())
                : label(QuestionId.ALTERNATION_WHY_INDEX, "label_without_index");
        boolean comparable = facts.historicalCampaigns() >= 2;
        return List.of(
                question(QuestionId.ALTERNATION_WHY_INDEX, whyIndexLabel, "calendar", indexMissing),
                question(QuestionId.ALTERNATION_BREAK_CYCLE, label(QuestionId.ALTERNATION_BREAK_CYCLE, "label"),
                        "pruning", indexMissing),
                question(QuestionId.ALTERNATION_VS_CAMPAIGNS, label(QuestionId.ALTERNATION_VS_CAMPAIGNS, "label"),
                        "history", comparable ? null : CatalogText.of("assistant.reason.campaigns_missing")),
                question(QuestionId.ALTERNATION_NEXT_YEAR,
                        label(QuestionId.ALTERNATION_NEXT_YEAR, "label", String.valueOf(facts.campaignYear() + 1)),
                        "harvest",
                        facts.caliberEstimated() ? null : CatalogText.of("assistant.reason.caliber_missing")));
    }

    private static List<CatalogQuestion> plan(PlotFacts facts) {
        CatalogText reason = planMissing(facts);
        CatalogText whyPercent;
        CatalogText ifLess;
        if (reason == null) {
            int percent = (int) Math.round(facts.percentageToRemove());
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

    /** Why the plan questions cannot be answered, or null when the prescription is issued. */
    private static CatalogText planMissing(PlotFacts facts) {
        if (facts.prescriptionIssued() && facts.percentageToRemove() != null) {
            return null;
        }
        if (facts.sampledTrees() > 0 && facts.treesNeeded() > 0) {
            return facts.treesNeeded() == 1
                    ? CatalogText.of("assistant.reason.trees_missing.one")
                    : CatalogText.of("assistant.reason.trees_missing.other", facts.treesNeeded());
        }
        return CatalogText.of("assistant.reason.plan_missing");
    }

    private static CatalogText label(QuestionId id, String variant, Object... args) {
        return CatalogText.of(PREFIX + id.value() + "." + variant, args);
    }

    private static CatalogQuestion question(QuestionId id, CatalogText label, String icon, CatalogText reason) {
        return new CatalogQuestion(id, label, icon, reason == null, reason);
    }
}
