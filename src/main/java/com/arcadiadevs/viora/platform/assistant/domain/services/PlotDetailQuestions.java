package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotDetailFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.band;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.forecastSource;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.label;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.question;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.round;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.shared;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.text;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.whole;

/**
 * The questions of the plot detail screen (P26, P136-P138): which ones it offers and why one is disabled, and the
 * template each one is answered with. Every answer is about the one plot of the screen.
 */
public final class PlotDetailQuestions {

    private PlotDetailQuestions() {
    }

    /**
     * Evaluates the questions of the screen against the figures of the plot. The state of the plot needs its bearing
     * index or a sampling of the campaign, the last sampling a sampled tree and the harvest an estimated caliber.
     * Today's tasks are always offered; without any pending they say so.
     *
     * @param facts the figures of the plot
     * @return the questions in display order
     */
    public static List<CatalogQuestion> catalog(PlotDetailFacts facts) {
        var plan = facts.plan();
        return List.of(
                question(QuestionId.PLOT_HOW_IS, label(QuestionId.PLOT_HOW_IS, "label"), "lote",
                        hasIndex(facts) || facts.sampled()
                                ? null : CatalogText.of("assistant.reason.plot_data_missing")),
                question(QuestionId.PLOT_TODAY, label(QuestionId.PLOT_TODAY, "label"), "calendar", null),
                question(QuestionId.PLOT_LAST_SAMPLING, label(QuestionId.PLOT_LAST_SAMPLING, "label"), "fruit",
                        facts.sampled() ? null : CatalogText.of("assistant.reason.sampling_missing")),
                question(QuestionId.PLOT_NEXT_YEAR_HARVEST,
                        label(QuestionId.PLOT_NEXT_YEAR_HARVEST, "label", String.valueOf(facts.campaignYear() + 1)),
                        "harvest", plan != null && plan.caliberEstimated()
                                ? null : CatalogText.of("assistant.reason.caliber_missing")));
    }

    /**
     * Writes the answer to a question of the screen.
     *
     * @param questionId the question
     * @param facts      the figures of the plot
     * @return the answer, or empty when the question is not of the screen or its figures are missing
     */
    public static Optional<AnswerDraft> answer(QuestionId questionId, PlotDetailFacts facts) {
        if (questionId.equals(QuestionId.PLOT_HOW_IS)) {
            return howIs(facts);
        } else if (questionId.equals(QuestionId.PLOT_TODAY)) {
            return Optional.of(today(facts));
        } else if (questionId.equals(QuestionId.PLOT_LAST_SAMPLING)) {
            return lastSampling(facts);
        } else if (questionId.equals(QuestionId.PLOT_NEXT_YEAR_HARVEST)) {
            return nextYearHarvest(facts);
        }
        return Optional.empty();
    }

    /**
     * "¿Cómo está este lote?": two sentences, the bearing index with its band and where the thinning of the campaign
     * stands (contract: "vecería 0,51 + 12 días", the days left in the open window). Either may be missing, not both.
     */
    private static Optional<AnswerDraft> howIs(PlotDetailFacts facts) {
        var q = "plot.how_is";
        var today = facts.today();
        var plan = facts.plan();
        var state = ThinningWindow.of(today, plan);
        var bandText = band(facts.bearingCategory());
        boolean hasIndex = hasIndex(facts);
        if (!hasIndex && state == ThinningWindow.NONE) {
            return Optional.empty();
        }
        final CatalogText indexText;
        final FactDraft indexCard;
        if (hasIndex) {
            double index = round(facts.bearingIndex(), 2);
            indexText = text(q, "index", index, bandText.get());
            indexCard = new FactDraft(shared("value.decimal", index), shared("card.bbi.label"), bandText.get(),
                    FactTone.PRIMARY);
        } else {
            indexText = text(q, "index.none");
            indexCard = new FactDraft(shared("card.no_index.value"), shared("card.bbi.label"),
                    shared("card.no_index.pill"), FactTone.PRIMARY);
        }
        final CatalogText source;
        if (hasIndex && facts.harvestCampaigns() > 0) {
            source = shared("source.campaigns", facts.harvestCampaigns());
        } else if (facts.sampled()) {
            source = samplingSource(plan);
        } else {
            source = shared("source.plan");
        }
        return Optional.of(new AnswerDraft(
                text(q, "text", indexText, howIsThinning(today, plan, state, facts.treesNeeded())),
                List.of(indexCard, state.card(today, plan)), source, List.of()));
    }

    private static CatalogText howIsThinning(LocalDate today, ThinningPlanData plan, ThinningWindow state,
                                             int treesNeeded) {
        var q = "plot.how_is";
        return switch (state) {
            case OPEN -> text(q, "thinning.open", ThinningWindow.daysLeft(today, plan),
                    whole(plan.percentageToRemove()), TextDate.of(plan.windowClosesOn()));
            case BEFORE -> text(q, "thinning.before", TextDate.of(plan.windowOpensOn()),
                    whole(plan.percentageToRemove()));
            case CLOSED -> text(q, "thinning.closed", TextDate.of(plan.windowClosesOn()));
            case DONE -> text(q, "thinning.done", TextDate.of(plan.executedOn()));
            case SAMPLING -> treesNeeded > 0
                    ? text(q, "thinning.sampling", treesNeeded)
                    : text(q, "thinning.unissued");
            case NONE -> text(q, "thinning.none");
        };
    }

    /**
     * "¿Qué hago hoy en este lote?": three sentences, in this order: the thinning (the open window, then one still to
     * open, then a sampling left halfway), today's forecast against the heat and frost thresholds, and the open
     * alerts. Always answerable: without anything pending it says so.
     */
    private static AnswerDraft today(PlotDetailFacts facts) {
        var q = "plot.today";
        var today = facts.today();
        var plan = facts.plan();
        var state = ThinningWindow.of(today, plan);
        CatalogText thinning = switch (state) {
            case OPEN -> text(q, "thinning.open", TextDate.of(plan.windowClosesOn()),
                    whole(plan.percentageToRemove()));
            case BEFORE -> text(q, "thinning.before", TextDate.of(plan.windowOpensOn()));
            case SAMPLING -> facts.treesNeeded() > 0
                    ? text(q, "thinning.sampling", facts.treesNeeded())
                    : text(q, "thinning.none");
            case CLOSED, DONE, NONE -> text(q, "thinning.none");
        };

        var thresholds = facts.thresholds();
        var forecastDay = facts.forecast() == null || thresholds == null
                ? Optional.<ClimateData.ForecastDay>empty()
                : facts.forecast().days().stream().filter(day -> today.equals(day.date())).findFirst();
        final CatalogText weather;
        final FactDraft weatherCard;
        if (forecastDay.isEmpty()) {
            weather = text(q, "forecast.none");
            weatherCard = new FactDraft(shared("card.no_forecast.value"), shared("card.today_max.label"),
                    shared("card.no_forecast.pill"), FactTone.PRIMARY);
        } else {
            var day = forecastDay.get();
            double max = round(day.max(), 1);
            double min = round(day.min(), 1);
            if (day.heatRisk()) {
                weather = text(q, "forecast.heat", max, round(thresholds.heatWarning(), 1));
            } else if (day.frostRisk()) {
                weather = text(q, "forecast.frost", min, round(thresholds.frostWarning(), 1));
            } else {
                weather = text(q, "forecast.mild", min, max);
            }
            weatherCard = new FactDraft(shared("value.measure", max, "°C"), shared("card.today_max.label"),
                    shared("card.today_min.pill", min), FactTone.PRIMARY);
        }

        var alerts = facts.openAlerts();
        var severe = ClimateData.mostSevere(alerts);
        var alertsText = severe.isEmpty()
                ? text(q, "alerts.none")
                : text(q, "alerts.some", alerts.size(), shared("alert." + severe.get().type()),
                        shared("severity." + severe.get().severity()));
        var alertsCard = new FactDraft(shared("value.integer", alerts.size()),
                shared("card.open_alerts.label", alerts.size()), shared("card.this_plot.pill"), FactTone.HIGHLIGHT);

        final CatalogText source;
        if (state != ThinningWindow.NONE) {
            source = shared("source.plan");
        } else if (forecastDay.isPresent()) {
            source = forecastSource(facts.forecast());
        } else {
            source = shared("source.open_alerts.plot");
        }
        return new AnswerDraft(text(q, "text", thinning, weather, alertsText), List.of(weatherCard, alertsCard),
                source, List.of());
    }

    /**
     * "¿Qué dijo el último muestreo?": the sampling of the campaign as the thinning context sums it up, the trees and
     * the day of the latest round; with the prescription issued, the measured load against the target and the
     * percentage it leads to, otherwise the trees still missing. The thinning context exposes the rounds summed up,
     * never one by one, so the load is that of the whole sampling.
     */
    private static Optional<AnswerDraft> lastSampling(PlotDetailFacts facts) {
        var q = "plot.last_sampling";
        var plan = facts.plan();
        if (!facts.sampled()) {
            return Optional.empty();
        }
        int trees = plan.sampledTrees();
        final CatalogText latest;
        final CatalogText treesPill;
        if (plan.lastSampledOn() == null) {
            latest = text(q, "latest.undated");
            treesPill = shared("card.undated.pill");
        } else {
            latest = text(q, "latest", TextDate.of(plan.lastSampledOn()));
            treesPill = shared("card.latest_round.pill", TextDate.shortOf(plan.lastSampledOn()));
        }
        var treesCard = new FactDraft(shared("value.integer", trees), shared("card.sampled_trees.label", trees),
                treesPill, FactTone.PRIMARY);
        final CatalogText text;
        final FactDraft secondCard;
        if (plan.hasLoads()) {
            double load = round(plan.preThinningFruitsPerShoot(), 2);
            double target = round(plan.targetFruitsPerShoot(), 2);
            text = text(q, "text.issued", trees, latest, load, target, whole(plan.percentageToRemove()));
            secondCard = new FactDraft(shared("value.decimal", load), shared("card.load.label"),
                    shared("card.target.pill", target), FactTone.HIGHLIGHT);
        } else if (facts.treesNeeded() > 0) {
            text = text(q, "text.halfway", trees, latest, facts.treesNeeded());
            secondCard = new FactDraft(shared("value.integer", facts.treesNeeded()),
                    shared("card.trees_needed.label", facts.treesNeeded()), shared("card.for_plan.pill"),
                    FactTone.HIGHLIGHT);
        } else {
            text = text(q, "text.pending", trees, latest);
            secondCard = new FactDraft(shared("card.no_plan.value"), shared("card.no_plan.label"),
                    shared("card.sampling.pill", trees), FactTone.HIGHLIGHT);
        }
        return Optional.of(new AnswerDraft(text, List.of(treesCard, secondCard), samplingSource(plan), List.of()));
    }

    /**
     * "¿Cuánto cosecharé en 2027?": the yield and the caliber the thinning context projected with the load the
     * confirmed thinning left. The label names the next year, as the contract does, but the figure is the harvest
     * of the current campaign: there is no projection beyond it.
     */
    private static Optional<AnswerDraft> nextYearHarvest(PlotDetailFacts facts) {
        var q = "plot.next_year_harvest";
        var plan = facts.plan();
        if (plan == null || !plan.caliberEstimated()) {
            return Optional.empty();
        }
        long fruitsPerKg = whole(plan.mostLikelyFruitsPerKg());
        var caliberCard = new FactDraft(shared("value.integer", fruitsPerKg), shared("card.caliber.label"),
                shared("card.grade.pill", plan.commercialSizeGrade()), FactTone.HIGHLIGHT);
        final FactDraft yieldCard;
        final CatalogText text;
        if (plan.estimatedKgPerHaAfterThinning() != null) {
            long kgPerHa = whole(plan.estimatedKgPerHaAfterThinning());
            yieldCard = new FactDraft(shared("value.integer", kgPerHa), shared("card.yield.label"),
                    shared("card.calibrating.pill"), FactTone.PRIMARY);
            text = text(q, "text.yield", kgPerHa, fruitsPerKg, plan.commercialSizeGrade());
        } else {
            yieldCard = new FactDraft(shared("card.no_yield.value"), shared("card.no_yield.label"),
                    shared("card.no_yield.pill"), FactTone.PRIMARY);
            text = text(q, "text.no_yield", fruitsPerKg, plan.commercialSizeGrade());
        }
        final CatalogText source;
        if (plan.executedOn() != null) {
            source = shared("source.thinning", TextDate.of(plan.executedOn()));
        } else if (facts.harvestCampaigns() > 0) {
            source = shared("source.campaigns", facts.harvestCampaigns());
        } else {
            source = shared("source.plan");
        }
        return Optional.of(new AnswerDraft(text, List.of(yieldCard, caliberCard), source, List.of()));
    }

    /** Whether the plot has a bearing index in a band the answers can name. */
    private static boolean hasIndex(PlotDetailFacts facts) {
        return facts.bearingIndex() != null && band(facts.bearingCategory()).isPresent();
    }

    /** "Basado en 5 árboles, ronda del 29 de octubre". */
    static CatalogText samplingSource(ThinningPlanData plan) {
        return plan.lastSampledOn() == null
                ? shared("source.sampling_undated", plan.sampledTrees())
                : shared("source.sampling", plan.sampledTrees(), TextDate.of(plan.lastSampledOn()));
    }
}
