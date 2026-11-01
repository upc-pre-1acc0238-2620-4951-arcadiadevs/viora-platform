package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CampaignClosedFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.band;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.label;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.question;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.round;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.shared;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.text;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.whole;

/**
 * The questions of the closed campaign screen (P73, P148-P150): which ones it offers and why one is disabled, and the
 * template each one is answered with. The closed campaign is the latest one with a recorded harvest, whose year the
 * labels name.
 */
public final class CampaignClosedQuestions {

    /** Bands with alternation, where the thinning by load is the measure of the plan. */
    private static final Set<String> ALTERNATING = Set.of("MODERATE_ALTERNATION", "SEVERE_ALTERNATION");

    /** Timeliness values with their own sentence; any other reads as a recorded thinning. */
    private static final Set<String> TIMELINESS = Set.of("OPTIMAL", "LATE");

    private CampaignClosedQuestions() {
    }

    /**
     * Evaluates the questions of the screen against the figures of the plot. Without a harvest nothing can be
     * answered; the comparison also needs an earlier campaign. {@code caliber_change} is always disabled: no context
     * the assistant reads has the caliber of each campaign (docs/asistente-decisiones.md, Pantalla Campaña cerrada).
     *
     * @param facts the figures of the plot
     * @return the questions in display order
     */
    public static List<CatalogQuestion> catalog(CampaignClosedFacts facts) {
        var caliber = question(QuestionId.CAMPAIGN_CALIBER_CHANGE, label(QuestionId.CAMPAIGN_CALIBER_CHANGE, "label"),
                "fruit", CatalogText.of("assistant.reason.caliber_history_unavailable"));
        var closed = facts.closedCampaignYear();
        if (closed.isEmpty()) {
            var missing = CatalogText.of("assistant.reason.harvest_missing");
            return List.of(
                    question(QuestionId.CAMPAIGN_VS_PREVIOUS,
                            label(QuestionId.CAMPAIGN_VS_PREVIOUS, "label_without_year"), "history", missing),
                    caliber,
                    question(QuestionId.CAMPAIGN_PLAN_NEXT,
                            label(QuestionId.CAMPAIGN_PLAN_NEXT, "label_without_year"), "calendar", missing));
        }
        int year = closed.get();
        boolean comparable = facts.yieldsKg().size() >= 2;
        return List.of(
                question(QuestionId.CAMPAIGN_VS_PREVIOUS,
                        label(QuestionId.CAMPAIGN_VS_PREVIOUS, "label", String.valueOf(year)), "history",
                        comparable ? null : CatalogText.of("assistant.reason.campaigns_missing")),
                caliber,
                question(QuestionId.CAMPAIGN_PLAN_NEXT,
                        label(QuestionId.CAMPAIGN_PLAN_NEXT, "label", String.valueOf(year + 1)), "calendar", null));
    }

    /**
     * Writes the answer to a question of the screen. {@code caliber_change} has no answer.
     *
     * @param questionId the question
     * @param facts      the figures of the plot
     * @return the answer, or empty when the question is not of the screen or its figures are missing
     */
    public static Optional<AnswerDraft> answer(QuestionId questionId, CampaignClosedFacts facts) {
        if (questionId.equals(QuestionId.CAMPAIGN_VS_PREVIOUS)) {
            return versusPrevious(facts);
        } else if (questionId.equals(QuestionId.CAMPAIGN_PLAN_NEXT)) {
            return planNext(facts);
        }
        return Optional.empty();
    }

    /**
     * "¿Cómo fue la campaña frente a las anteriores?": the harvest of the closed campaign against the mean of the
     * earlier ones, as {@code alternation.vs_campaigns} compares them. With the surface of the plot both are also
     * given in tonnes per hectare, the unit of the contract, over the polygon area the agronomic dossier uses; the
     * percentage is always computed from the kilograms, so the rounding of the tonnes never moves it.
     */
    private static Optional<AnswerDraft> versusPrevious(CampaignClosedFacts facts) {
        var q = "campaign.vs_previous";
        var yields = facts.yieldsKg();
        if (yields.size() < 2) {
            return Optional.empty();
        }
        int lastYear = yields.lastKey();
        double last = yields.get(lastYear);
        var earlier = yields.headMap(lastYear);
        double average = earlier.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        if (average <= 0.0) {
            return Optional.empty();
        }
        long difference = whole(Math.abs(last - average) / average * 100.0);
        String trend = difference == 0L ? "EQUAL" : last > average ? "ABOVE" : "BELOW";
        long lastKg = whole(last);
        long averageKg = whole(average);
        int previous = earlier.size();
        String year = String.valueOf(lastYear);
        String variant = trend.toLowerCase(Locale.ROOT);
        final CatalogText text;
        final FactDraft harvestCard;
        final CatalogText source;
        Double area = facts.areaHectares();
        if (area != null && area > 0.0) {
            double lastTons = round(last / 1000.0 / area, 1);
            double averageTons = round(average / 1000.0 / area, 1);
            text = text(q, "text." + variant, year, lastTons, lastKg, difference, previous, averageTons);
            harvestCard = new FactDraft(shared("value.t_per_ha", lastTons), shared("card.harvest.label", year),
                    shared("card.average_t_per_ha.pill", averageTons), FactTone.PRIMARY);
            // The tonnes per hectare come from the polygon area, and the source says so
            source = shared("source.plot_area", round(area, 2));
        } else {
            text = text(q, "text.kg." + variant, year, lastKg, difference, previous, averageKg);
            harvestCard = new FactDraft(shared("value.kg", lastKg), shared("card.harvest.label", year),
                    shared("card.average.pill", averageKg), FactTone.PRIMARY);
            source = shared("source.campaigns", yields.size());
        }
        String valueKey = switch (trend) {
            case "ABOVE" -> "value.plus_percent";
            case "BELOW" -> "value.minus_percent";
            default -> "value.percent";
        };
        return Optional.of(new AnswerDraft(text, List.of(harvestCard,
                new FactDraft(shared(valueKey, difference), shared("card.vs_average.label"),
                        shared("card.previous.pill", previous), FactTone.HIGHLIGHT)),
                source, List.of()));
    }

    /**
     * "¿Qué planifico para la próxima campaña?": only what Viora already knows. The alternation band of the bearing
     * index; whether the closed campaign was thinned, and on time, as Thinning recorded it; and the steps of the
     * thinning flow for the next campaign (full bloom, sampling, the window of the variety profile). It never
     * projects the next harvest: no context does.
     */
    private static Optional<AnswerDraft> planNext(CampaignClosedFacts facts) {
        var q = "campaign.plan_next";
        var closed = facts.closedCampaignYear();
        if (closed.isEmpty()) {
            return Optional.empty();
        }
        String closedText = String.valueOf(closed.get());
        String nextText = String.valueOf(closed.get() + 1);
        CatalogText bearing = null;
        FactDraft secondCard = null;
        var bandText = band(facts.bearingCategory());
        if (facts.bearingIndex() != null && bandText.isPresent()) {
            double index = round(facts.bearingIndex(), 2);
            bearing = text(q, ALTERNATING.contains(facts.bearingCategory()) ? "bearing.alternating" : "bearing.regular",
                    index, bandText.get());
            secondCard = new FactDraft(shared("value.decimal", index), shared("card.bbi.label"), bandText.get(),
                    FactTone.HIGHLIGHT);
        }
        var outcome = facts.outcome();
        final CatalogText thinning;
        if (outcome != null) {
            String timeliness = outcome.timeliness() != null && TIMELINESS.contains(outcome.timeliness())
                    ? outcome.timeliness() : "done";
            thinning = text(q, "thinning." + timeliness, closedText, TextDate.of(outcome.executedOn()));
            if (secondCard == null) {
                secondCard = new FactDraft(shared("card.done.value"), shared("card.closed_thinning.label", closedText),
                        shared("card.done.pill", TextDate.shortOf(outcome.executedOn())), FactTone.HIGHLIGHT);
            }
        } else {
            thinning = text(q, "thinning.none", closedText);
            if (secondCard == null) {
                secondCard = new FactDraft(shared("card.not_thinned.value"),
                        shared("card.closed_thinning.label", closedText), shared("card.no_plan.pill"),
                        FactTone.HIGHLIGHT);
            }
        }
        final CatalogText steps;
        if (outcome != null && outcome.windowOpensDaysAfterBloom() != null
                && outcome.windowClosesDaysAfterBloom() != null) {
            steps = text(q, "steps.window", nextText, outcome.windowOpensDaysAfterBloom(),
                    outcome.windowClosesDaysAfterBloom());
        } else {
            steps = text(q, "steps", nextText);
        }
        var text = bearing == null
                ? text(q, "text.short", thinning, steps)
                : text(q, "text", bearing, thinning, steps);
        return Optional.of(new AnswerDraft(text, List.of(
                new FactDraft(shared("card.date.value", nextText), shared("card.next_campaign.label"),
                        shared("card.after_bloom.pill"), FactTone.PRIMARY),
                secondCard), shared("source.campaigns", facts.yieldsKg().size()), List.of()));
    }
}
