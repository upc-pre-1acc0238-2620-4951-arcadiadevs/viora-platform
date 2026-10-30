package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ActionDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerAction;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFacts.Campaign;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Writes the answer to a closed question from the figures of the plot, with fixed sentences. The server computes
 * every figure; here nothing is estimated or invented, and a question whose figures are missing has no answer.
 *
 * <p>The sentences are the deterministic template of each question. The language model of a later step only
 * rephrases them; these stay as its fallback.</p>
 */
public final class AnswerComposer {

    private static final String KEY = "assistant.answer.";
    private static final String BAND_KEY = "assistant.band.";
    private static final String DIGEST = "SHA-256";

    private static final String THINNING_PLAN_ROUTE = "thinning/plan?plotId=";
    private static final String THINNING_EXECUTE_ROUTE = "thinning/execute?plotId=";
    private static final String SAMPLING_ROUTE = "thinning/sampling?plotId=";

    private AnswerComposer() {
    }

    /**
     * Fingerprint of the figures the questions of a screen read. It changes exactly when one of them changes, so an
     * answer written from an older fingerprint is stale.
     *
     * @param screen the screen
     * @param facts  the figures of the plot
     * @return the version
     */
    public static DataVersion dataVersionOf(ScreenKey screen, AnswerFacts facts) {
        var canonical = switch (screen) {
            case ALTERNATION -> "ALTERNATION|" + facts.campaignYear() + "|" + facts.areaHectares() + "|"
                    + (facts.yieldsKg() == null ? "" : facts.yieldsKg().entrySet().stream()
                    .map(entry -> entry.getKey() + "=" + entry.getValue()).collect(Collectors.joining(";")))
                    + "|" + facts.bearingIndex() + "|" + facts.bearingCategory();
            case PLAN -> "PLAN|" + facts.campaignYear() + "|" + facts.preThinningFruitsPerShoot() + "|"
                    + facts.targetFruitsPerShoot() + "|" + facts.percentageToRemove() + "|" + facts.windowOpensOn()
                    + "|" + facts.windowClosesOn() + "|" + facts.sampledTrees();
        };
        try {
            var digest = MessageDigest.getInstance(DIGEST).digest(canonical.getBytes(StandardCharsets.UTF_8));
            return new DataVersion("sha256:" + HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(DIGEST + " is always available", e);
        }
    }

    /**
     * Writes the answer to a question.
     *
     * @param questionId the question
     * @param facts      the figures of the plot
     * @param plotId     the plot, for the routes of the buttons
     * @return the answer, or empty when the figures it needs are missing
     */
    public static Optional<AnswerDraft> compose(QuestionId questionId, AnswerFacts facts, PlotId plotId) {
        if (questionId.equals(QuestionId.ALTERNATION_WHY_INDEX)) {
            return whyIndex(facts);
        } else if (questionId.equals(QuestionId.ALTERNATION_BREAK_CYCLE)) {
            return breakCycle(facts, plotId);
        } else if (questionId.equals(QuestionId.ALTERNATION_VS_CAMPAIGNS)) {
            return versusCampaigns(facts);
        } else if (questionId.equals(QuestionId.ALTERNATION_NEXT_YEAR)) {
            return nextYear(facts);
        } else if (questionId.equals(QuestionId.PLAN_WHY_PERCENT)) {
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

    // ---- ALTERNATION ---------------------------------------------------------------------------------------------

    private static Optional<AnswerDraft> whyIndex(AnswerFacts facts) {
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

    private static Optional<AnswerDraft> breakCycle(AnswerFacts facts, PlotId plotId) {
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

    private static Optional<AnswerDraft> versusCampaigns(AnswerFacts facts) {
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
    private static Optional<AnswerDraft> nextYear(AnswerFacts facts) {
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

    // ---- PLAN ----------------------------------------------------------------------------------------------------

    private static Optional<AnswerDraft> whyPercent(AnswerFacts facts, PlotId plotId) {
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

    private static Optional<AnswerDraft> ifLess(AnswerFacts facts, PlotId plotId) {
        if (!hasPrescription(facts)) {
            return Optional.empty();
        }
        var q = "plan.if_less";
        var prescribed = (int) Math.round(facts.percentageToRemove());
        var thinned = Math.max(0, prescribed - QuestionCatalog.LESS_THINNING_POINTS);
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

    private static Optional<AnswerDraft> ifNone(AnswerFacts facts, PlotId plotId) {
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
    private static Optional<AnswerDraft> bestTime(AnswerFacts facts, PlotId plotId) {
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

    // ---- Helpers -------------------------------------------------------------------------------------------------

    /** A prescription with something to remove: load, target and a percentage above zero. */
    private static boolean hasPrescription(AnswerFacts facts) {
        return facts.preThinningFruitsPerShoot() != null && facts.targetFruitsPerShoot() != null
                && facts.percentageToRemove() != null && facts.percentageToRemove() > 0.0
                && facts.targetFruitsPerShoot() > 0.0 && facts.preThinningFruitsPerShoot() > 0.0;
    }

    private static CatalogText planSource(AnswerFacts facts) {
        return facts.sampledTrees() > 0
                ? text("plan", "source", facts.sampledTrees())
                : text("plan", "source_plain");
    }

    private static Optional<CatalogText> band(String category) {
        if (category == null) {
            return Optional.empty();
        }
        return switch (category) {
            case "REGULAR" -> Optional.of(CatalogText.of(BAND_KEY + "regular"));
            case "MODERATE_ALTERNATION" -> Optional.of(CatalogText.of(BAND_KEY + "moderate"));
            case "SEVERE_ALTERNATION" -> Optional.of(CatalogText.of(BAND_KEY + "severe"));
            default -> Optional.empty();
        };
    }

    private static CatalogText text(String question, String part, Object... args) {
        return CatalogText.of(KEY + question + "." + part, args);
    }

    private static double average(List<Campaign> campaigns) {
        return campaigns.stream().mapToDouble(Campaign::tonnesPerHa).average().orElse(0.0);
    }

    /** By how many whole percent {@code value} is above (positive) or below (negative) {@code reference}. */
    private static int percentOf(double value, double reference) {
        return (int) Math.round((value / reference - 1.0) * 100.0);
    }

    /** Rounds half up, so the figure of the text and of the card are the same number. */
    private static double round(double value, int decimals) {
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
    }
}
