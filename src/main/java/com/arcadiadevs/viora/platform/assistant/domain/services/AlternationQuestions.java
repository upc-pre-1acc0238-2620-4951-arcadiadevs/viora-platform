package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ActionDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AlternationFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AlternationFacts.Campaign;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.SAMPLING_ROUTE;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.THINNING_PLAN_ROUTE;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.band;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.label;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.percentOf;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.question;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.round;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.text;

/**
 * The questions of the alternation screen (P40, P114-P119): which ones it offers and why one is disabled, and the
 * template each one is answered with.
 */
public final class AlternationQuestions {

    private AlternationQuestions() {
    }

    /**
     * Evaluates the questions of the screen against the figures of the plot.
     *
     * @param facts the figures of the plot
     * @return the questions in display order
     */
    public static List<CatalogQuestion> catalog(AlternationFacts facts) {
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

    /**
     * Writes the answer to a question of the screen.
     *
     * @param questionId the question
     * @param facts      the figures of the plot
     * @param plotId     the plot, for the routes of the buttons
     * @return the answer, or empty when the question is not of the screen or its figures are missing
     */
    public static Optional<AnswerDraft> answer(QuestionId questionId, AlternationFacts facts, PlotId plotId) {
        if (questionId.equals(QuestionId.ALTERNATION_WHY_INDEX)) {
            return whyIndex(facts);
        } else if (questionId.equals(QuestionId.ALTERNATION_BREAK_CYCLE)) {
            return breakCycle(facts, plotId);
        } else if (questionId.equals(QuestionId.ALTERNATION_VS_CAMPAIGNS)) {
            return versusCampaigns(facts);
        } else if (questionId.equals(QuestionId.ALTERNATION_NEXT_YEAR)) {
            return nextYear(facts);
        }
        return Optional.empty();
    }

    private static Optional<AnswerDraft> whyIndex(AlternationFacts facts) {
        var campaigns = facts.campaigns();
        var band = band(facts.bearingCategory());
        if (facts.bearingIndex() == null || campaigns.size() < 2 || band.isEmpty()) {
            return Optional.empty();
        }
        var best = campaigns.stream().max(Comparator.comparingDouble(Campaign::tonnesPerHa)).orElseThrow();
        var lowest = campaigns.stream().min(Comparator.comparingDouble(Campaign::tonnesPerHa)).orElseThrow();
        var q = "alternation.why_index";
        var index = round(facts.bearingIndex(), 2);
        var bestValue = round(best.tonnesPerHa(), 1);
        var lowestValue = round(lowest.tonnesPerHa(), 1);
        var bestYear = String.valueOf(best.year());
        var lowestYear = String.valueOf(lowest.year());
        return Optional.of(new AnswerDraft(
                text(q, "text", index, band.get(), bestValue, bestYear, lowestValue, lowestYear),
                List.of(
                        new FactDraft(text(q, "fact1.value", index), text(q, "fact1.label"),
                                text(q, "fact1.pill", band.get()), FactTone.PRIMARY),
                        new FactDraft(text(q, "fact2.value", bestValue, lowestValue), text(q, "fact2.label"),
                                text(q, "fact2.pill", bestYear, lowestYear), FactTone.HIGHLIGHT)),
                text("alternation", "source", campaigns.size()),
                List.of()));
    }

    private static Optional<AnswerDraft> breakCycle(AlternationFacts facts, PlotId plotId) {
        var campaigns = facts.campaigns();
        if (facts.bearingIndex() == null || campaigns.size() < 2) {
            return Optional.empty();
        }
        var last = campaigns.getLast();
        var average = average(campaigns);
        if (average <= 0.0) {
            return Optional.empty();
        }
        var high = last.tonnesPerHa() > average;
        var variant = "alternation.break_cycle." + (high ? "high" : "low");
        var lastValue = round(last.tonnesPerHa(), 1);
        var averageValue = round(average, 1);
        var lastYear = String.valueOf(last.year());
        // The campaign after the last recorded one is the one that tends to be the opposite kind
        var targetYear = String.valueOf(last.year() + 1);
        var action = high
                ? new ActionDraft(AnswerAction.DEEPLINK, text("action", "register_sampling"), SAMPLING_ROUTE + plotId.plotId())
                : new ActionDraft(AnswerAction.DEEPLINK, text("action", "view_plan"), THINNING_PLAN_ROUTE + plotId.plotId());
        return Optional.of(new AnswerDraft(
                text(variant, "text", lastYear, lastValue, averageValue, targetYear),
                List.of(
                        new FactDraft(text("alternation.break_cycle", "fact1.value", lastValue),
                                text("alternation.break_cycle", "fact1.label", lastYear),
                                text("alternation.break_cycle", "fact1.pill", averageValue), FactTone.PRIMARY),
                        new FactDraft(text("alternation.break_cycle", "fact2.value", percentOf(last.tonnesPerHa(), average)),
                                text("alternation.break_cycle", "fact2.label"),
                                text(variant, "fact2.pill"), FactTone.HIGHLIGHT)),
                text("alternation", "source", campaigns.size()),
                List.of(action)));
    }

    private static Optional<AnswerDraft> versusCampaigns(AlternationFacts facts) {
        var campaigns = facts.campaigns();
        if (campaigns.size() < 2) {
            return Optional.empty();
        }
        var last = campaigns.getLast();
        var previous = campaigns.subList(0, campaigns.size() - 1);
        var previousAverage = average(previous);
        if (previousAverage <= 0.0) {
            return Optional.empty();
        }
        var difference = percentOf(last.tonnesPerHa(), previousAverage);
        var variant = "alternation.vs_campaigns." + (difference >= 0 ? "above" : "below");
        var lastValue = round(last.tonnesPerHa(), 1);
        var averageValue = round(previousAverage, 1);
        var lastYear = String.valueOf(last.year());
        return Optional.of(new AnswerDraft(
                text(variant, "text", lastYear, lastValue, Math.abs(difference), averageValue),
                List.of(
                        new FactDraft(text("alternation.vs_campaigns", "fact1.value", lastValue),
                                text("alternation.vs_campaigns", "fact1.label", lastYear),
                                text("alternation.vs_campaigns", "fact1.pill", averageValue), FactTone.PRIMARY),
                        new FactDraft(text("alternation.vs_campaigns", "fact2.value", difference),
                                text("alternation.vs_campaigns", "fact2.label"),
                                text("alternation.vs_campaigns", "fact2.pill", previous.size()), FactTone.HIGHLIGHT)),
                text("alternation", "source", campaigns.size()),
                List.of()));
    }

    /**
     * The only question the server cannot answer with a forecast: there is no yield model, so it reports the
     * pattern of the plot itself. Alternate bearing swaps the kind of year every campaign, so the kind of the year
     * asked about follows from the last recorded campaign and the number of years between them; the figure is the
     * average of the campaigns of that kind.
     */
    private static Optional<AnswerDraft> nextYear(AlternationFacts facts) {
        var campaigns = facts.campaigns();
        if (campaigns.size() < 2) {
            return Optional.empty();
        }
        var last = campaigns.getLast();
        var average = average(campaigns);
        var target = facts.campaignYear() + 1;
        var lastIsHigh = last.tonnesPerHa() > average;
        var targetIsHigh = (target - last.year()) % 2 == 0 ? lastIsHigh : !lastIsHigh;
        var ofThatKind = campaigns.stream()
                .filter(campaign -> targetIsHigh == (campaign.tonnesPerHa() > average))
                .toList();
        var kindAverage = round(ofThatKind.isEmpty() ? average : average(ofThatKind), 1);
        var variant = "alternation.next_year." + (targetIsHigh ? "high" : "low");
        var nextYear = String.valueOf(target);
        var lastYear = String.valueOf(last.year());
        return Optional.of(new AnswerDraft(
                text(variant, "text", nextYear, kindAverage),
                List.of(
                        new FactDraft(text("alternation.next_year", "fact1.value", kindAverage),
                                text(variant, "fact1.label"),
                                text("alternation.next_year", "fact1.pill", nextYear), FactTone.PRIMARY),
                        new FactDraft(text("alternation.next_year", "fact2.value", round(last.tonnesPerHa(), 1)),
                                text("alternation.next_year", "fact2.label"),
                                text("alternation.next_year", "fact2.pill", lastYear), FactTone.HIGHLIGHT)),
                text("alternation", "source", campaigns.size()),
                List.of()));
    }

    private static double average(List<Campaign> campaigns) {
        return campaigns.stream().mapToDouble(Campaign::tonnesPerHa).average().orElse(0.0);
    }
}
