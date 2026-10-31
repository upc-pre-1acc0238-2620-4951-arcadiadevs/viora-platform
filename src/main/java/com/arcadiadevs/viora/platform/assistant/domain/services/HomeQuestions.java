package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ThinningPlanData;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
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
 * The questions of the home screen (P110-P113): which ones it offers and why one is disabled, and the template each
 * one is answered with. Every answer is about one plot or, without plot, about every active plot of the producer;
 * a field of a single plot reads as that plot.
 */
public final class HomeQuestions {

    /** Days of "this week", today included. */
    static final int WEEK_DAYS = 7;

    private HomeQuestions() {
    }

    /**
     * Evaluates the questions of the screen against the figures of the plot or the field. The week plan needs an
     * active plot, the comparison two campaigns of the same plots and the estimate an estimated caliber. The alerts
     * are always offered; without any they say so.
     *
     * @param facts the figures of the plot or the field
     * @return the questions in display order
     */
    public static List<CatalogQuestion> catalog(HomeFacts facts) {
        return List.of(
                question(QuestionId.HOME_WEEK_PLAN, label(QuestionId.HOME_WEEK_PLAN, "label"), "calendar",
                        facts.activePlots() > 0 ? null : CatalogText.of("assistant.reason.plots_missing")),
                question(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, label(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, "label"),
                        "history", facts.campaignComparison().isPresent()
                                ? null : CatalogText.of("assistant.reason.campaigns_missing")),
                question(QuestionId.HOME_ALERTS_AFFECTING,
                        label(QuestionId.HOME_ALERTS_AFFECTING, facts.fieldScope() ? "label.field" : "label"),
                        "alert", null),
                question(QuestionId.HOME_HARVEST_ESTIMATE, label(QuestionId.HOME_HARVEST_ESTIMATE, "label"),
                        "harvest", facts.caliberEstimates().isEmpty()
                                ? CatalogText.of("assistant.reason.caliber_missing") : null));
    }

    /**
     * Writes the answer to a question of the screen.
     *
     * @param questionId the question
     * @param facts      the figures of the plot or the field
     * @return the answer, or empty when the question is not of the screen or its figures are missing
     */
    public static Optional<AnswerDraft> answer(QuestionId questionId, HomeFacts facts) {
        if (questionId.equals(QuestionId.HOME_WEEK_PLAN)) {
            return weekPlan(facts);
        } else if (questionId.equals(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS)) {
            return campaignVsPrevious(facts);
        } else if (questionId.equals(QuestionId.HOME_ALERTS_AFFECTING)) {
            return Optional.of(alertsAffecting(facts));
        } else if (questionId.equals(QuestionId.HOME_HARVEST_ESTIMATE)) {
            return harvestEstimate(facts);
        }
        return Optional.empty();
    }

    /**
     * "¿Qué hago esta semana?": three sentences, in this order: the thinning (an open window, then one opening this
     * week, then a sampling left halfway), the hottest forecast day of the week against the heat threshold, and the
     * open alerts.
     */
    private static Optional<AnswerDraft> weekPlan(HomeFacts facts) {
        var q = "home.week_plan";
        if (facts.activePlots() == 0) {
            return Optional.empty();
        }
        boolean single = facts.singlePlot();
        var thinning = weekThinning(facts);

        var hottest = hottest(facts);
        final CatalogText weather;
        final FactDraft weatherCard;
        if (hottest.isEmpty()) {
            weather = text(q, "forecast.none");
            weatherCard = new FactDraft(shared("card.no_forecast.value"), shared("card.week_max.label"),
                    shared("card.no_forecast.pill"), FactTone.PRIMARY);
        } else {
            var day = hottest.get().day();
            double max = round(day.max(), 1);
            weather = text(q, day.heatRisk() ? "forecast.heat" : "forecast.mild", max, TextDate.of(day.date()),
                    round(facts.thresholds().heatWarning(), 1));
            weatherCard = new FactDraft(shared("value.measure", max, "°C"), shared("card.week_max.label"),
                    shared("card.on.pill", TextDate.shortOf(day.date())), FactTone.PRIMARY);
        }

        var alerts = facts.openAlerts();
        int plotsWithAlerts = facts.plotsWithAlerts();
        var severe = ClimateData.mostSevere(alerts);
        final CatalogText alertsText;
        if (severe.isEmpty()) {
            alertsText = text(q, single ? "alerts.none" : "alerts.none.field");
        } else if (single) {
            alertsText = text(q, "alerts.some", alerts.size(), AlertQuestions.alertName(severe.get()),
                    AlertQuestions.severityName(severe.get()));
        } else {
            alertsText = text(q, "alerts.some.field", alerts.size(), AlertQuestions.alertName(severe.get()),
                    AlertQuestions.severityName(severe.get()), plotsWithAlerts);
        }
        var alertsCard = new FactDraft(shared("value.integer", alerts.size()),
                shared("card.open_alerts.label", alerts.size()),
                single ? shared("card.this_plot.pill") : shared("card.in_plots.pill", plotsWithAlerts),
                FactTone.HIGHLIGHT);

        final CatalogText source;
        if (facts.plots().stream().anyMatch(plot -> plot.plan() != null)) {
            source = single ? shared("source.plan") : shared("source.plans");
        } else if (hottest.isPresent()) {
            source = forecastSource(hottest.get().forecast());
        } else {
            source = single ? shared("source.open_alerts.plot") : shared("source.open_alerts");
        }
        return Optional.of(new AnswerDraft(text(q, "text", thinning, weather, alertsText),
                List.of(weatherCard, alertsCard), source, List.of()));
    }

    /** The thinning sentence: open window, window opening this week, sampling left halfway, or nothing pending. */
    private static CatalogText weekThinning(HomeFacts facts) {
        var q = "home.week_plan";
        var today = facts.today();
        boolean single = facts.singlePlot();
        int active = facts.activePlots();
        var weekEnd = today.plusDays(WEEK_DAYS - 1L);
        var open = plansIn(facts, ThinningWindow.OPEN).stream()
                .sorted(Comparator.comparing(ThinningPlanData::windowClosesOn))
                .toList();
        var opening = plansIn(facts, ThinningWindow.BEFORE).stream()
                .filter(plan -> !plan.windowOpensOn().isAfter(weekEnd))
                .sorted(Comparator.comparing(ThinningPlanData::windowOpensOn))
                .toList();
        var halfway = facts.plots().stream()
                .filter(plot -> ThinningWindow.of(today, plot.plan()) == ThinningWindow.SAMPLING
                        && plot.treesNeeded() > 0)
                .toList();
        if (!open.isEmpty()) {
            var first = open.getFirst();
            return single
                    ? text(q, "thinning.open", TextDate.of(first.windowClosesOn()), whole(first.percentageToRemove()))
                    : text(q, "thinning.open.field", open.size(), active, TextDate.of(first.windowClosesOn()));
        }
        if (!opening.isEmpty()) {
            var first = opening.getFirst();
            return single
                    ? text(q, "thinning.opening", TextDate.of(first.windowOpensOn()),
                            whole(first.percentageToRemove()))
                    : text(q, "thinning.opening.field", opening.size(), active, TextDate.of(first.windowOpensOn()));
        }
        if (!halfway.isEmpty()) {
            return single
                    ? text(q, "thinning.sampling", halfway.getFirst().treesNeeded())
                    : text(q, "thinning.sampling.field", halfway.size(), active);
        }
        return single ? text(q, "thinning.none") : text(q, "thinning.none.field", active);
    }

    private static List<ThinningPlanData> plansIn(HomeFacts facts, ThinningWindow state) {
        return facts.plots().stream().map(HomeFacts.PlotHome::plan)
                .filter(plan -> ThinningWindow.of(facts.today(), plan) == state)
                .toList();
    }

    /** The hottest forecast day of the plots (the earliest on a tie), with the forecast it comes from. */
    private static Optional<Hottest> hottest(HomeFacts facts) {
        if (facts.thresholds() == null) {
            return Optional.empty();
        }
        return facts.plots().stream()
                .map(HomeFacts.PlotHome::forecast)
                .filter(Objects::nonNull)
                .flatMap(forecast -> forecast.days().stream().map(day -> new Hottest(forecast, day)))
                .min(Comparator.comparingDouble((Hottest hottest) -> -hottest.day().max())
                        .thenComparing(hottest -> hottest.day().date()));
    }

    private record Hottest(ClimateData.Forecast forecast, ClimateData.ForecastDay day) {
    }

    /**
     * "¿Cómo va mi campaña frente a la anterior?": the last harvest against the one before it. For one plot it closes
     * with its bearing index; for the field it sums only the plots with both harvests and says how many are left out.
     */
    private static Optional<AnswerDraft> campaignVsPrevious(HomeFacts facts) {
        var q = "home.campaign_vs_previous";
        var found = facts.campaignComparison();
        if (found.isEmpty()) {
            return Optional.empty();
        }
        var comparison = found.get();
        long lastKg = whole(comparison.lastKg());
        long previousKg = whole(comparison.previousKg());
        long difference = whole(Math.abs(comparison.lastKg() - comparison.previousKg()) / comparison.previousKg()
                * 100.0);
        String trend = difference == 0L ? "equal" : comparison.lastKg() > comparison.previousKg() ? "above" : "below";
        int compared = comparison.comparedPlots();
        boolean single = facts.singlePlot();
        final CatalogText closing;
        final CatalogText pill;
        final CatalogText source;
        if (single) {
            var plot = facts.plots().getFirst();
            int campaigns = plot.yields().size();
            var bandText = band(plot.bearingCategory());
            closing = plot.bearingIndex() != null && bandText.isPresent()
                    ? text(q, "bearing", round(plot.bearingIndex(), 2), bandText.get())
                    : text(q, "bearing.missing");
            pill = shared("card.campaigns.pill", campaigns);
            source = shared("source.campaigns", campaigns);
        } else {
            closing = text(q, "left_out", facts.activePlots() - compared);
            pill = shared("card.compared_plots.pill", compared);
            source = shared("source.campaigns.field", compared);
        }
        String valueKey = switch (trend) {
            case "above" -> "value.plus_percent";
            case "below" -> "value.minus_percent";
            default -> "value.percent";
        };
        String lastYear = String.valueOf(comparison.lastYear());
        String previousYear = String.valueOf(comparison.previousYear());
        return Optional.of(new AnswerDraft(
                text(q, "text." + trend + (single ? "" : ".field"), lastYear, lastKg,
                        difference, previousYear, previousKg, closing, compared),
                List.of(
                        new FactDraft(shared("value.kg", lastKg), shared("card.harvest.label", lastYear),
                                shared("card.previous_harvest.pill", previousYear, previousKg), FactTone.PRIMARY),
                        new FactDraft(shared(valueKey, difference), shared("card.vs_previous.label"), pill,
                                FactTone.HIGHLIGHT)),
                source, List.of()));
    }

    /**
     * "¿Qué alertas afectan a este lote?" (or "a mis lotes"): the open heat, frost and hydric alerts and the most
     * severe one with its value against the threshold. Always answerable: without alerts it says so.
     */
    private static AnswerDraft alertsAffecting(HomeFacts facts) {
        var q = "home.alerts_affecting";
        var alerts = facts.openAlerts();
        int plotsWithAlerts = facts.plotsWithAlerts();
        int active = facts.activePlots();
        boolean single = facts.singlePlot();
        var severe = ClimateData.mostSevere(alerts);
        if (severe.isEmpty()) {
            var stateCard = single
                    ? new FactDraft(shared("card.no_alerts.value"), shared("card.plot_state.label"),
                            shared("card.this_plot.pill"), FactTone.HIGHLIGHT)
                    : new FactDraft(shared("value.integer", active), shared("card.field_plots.label", active),
                            shared("card.own_plots.pill"), FactTone.HIGHLIGHT);
            return new AnswerDraft(text(q, single ? "text.none" : "text.none.field"),
                    List.of(new FactDraft(shared("value.integer", 0), shared("card.open_alerts.label", 0),
                            shared("card.alert_types.pill"), FactTone.PRIMARY), stateCard),
                    single ? shared("source.open_alerts.plot") : shared("source.open_alerts"), List.of());
        }
        var incident = severe.get();
        double value = round(incident.value(), 1);
        double threshold = round(incident.threshold(), 1);
        var name = AlertQuestions.alertName(incident);
        var severity = AlertQuestions.severityName(incident);
        var text = single
                ? text(q, "text.some", alerts.size(), name, severity, value, incident.unit(), threshold)
                : text(q, "text.some.field", alerts.size(), name, severity, value, incident.unit(), threshold,
                        plotsWithAlerts, active);
        var countCard = new FactDraft(shared("value.integer", alerts.size()),
                shared("card.open_alerts.label", alerts.size()),
                single
                        ? shared("card.since.pill", TextDate.shortOf(incident.triggeredOn()))
                        : shared("card.in_plots.pill", plotsWithAlerts),
                FactTone.HIGHLIGHT);
        return new AnswerDraft(text, List.of(AlertQuestions.valueCard(incident), countCard),
                single ? AlertQuestions.alertSource(incident) : shared("source.open_alerts"), List.of());
    }

    /**
     * "¿Cuánto cosecharé este año?": the yield and the caliber the thinning context projected with the load the
     * thinning left. For the field, the range of the plots with an estimated yield and how many still miss it.
     */
    private static Optional<AnswerDraft> harvestEstimate(HomeFacts facts) {
        var estimates = facts.caliberEstimates();
        if (estimates.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(facts.singlePlot()
                ? plotHarvest(estimates.getFirst(), facts.plots().getFirst().yields().size())
                : fieldHarvest(facts, estimates));
    }

    private static AnswerDraft plotHarvest(ThinningPlanData plan, int harvestCampaigns) {
        var q = "home.harvest_estimate";
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
            yieldCard = noYieldCard();
            text = text(q, "text.no_yield", fruitsPerKg, plan.commercialSizeGrade());
        }
        final CatalogText source;
        if (plan.executedOn() != null) {
            source = shared("source.thinning", TextDate.of(plan.executedOn()));
        } else if (harvestCampaigns > 0) {
            source = shared("source.campaigns", harvestCampaigns);
        } else {
            source = shared("source.plan");
        }
        return new AnswerDraft(text, List.of(yieldCard, caliberCard), source, List.of());
    }

    private static AnswerDraft fieldHarvest(HomeFacts facts, List<ThinningPlanData> estimates) {
        var q = "home.harvest_estimate";
        int active = facts.activePlots();
        var yields = estimates.stream().map(ThinningPlanData::estimatedKgPerHaAfterThinning)
                .filter(Objects::nonNull)
                .map(ScreenTexts::whole)
                .sorted()
                .toList();
        final CatalogText text;
        final FactDraft yieldCard;
        final int counted;
        if (yields.isEmpty()) {
            counted = estimates.size();
            text = text(q, "text.field.no_yield", counted, active);
            yieldCard = noYieldCard();
        } else {
            counted = yields.size();
            long min = yields.getFirst();
            long max = yields.getLast();
            int without = active - counted;
            text = text(q, min == max ? "text.field.single" : "text.field.range", counted, active, min, max,
                    without);
            yieldCard = new FactDraft(min == max ? shared("value.integer", min) : shared("value.integer_range", min,
                    max), shared("card.yield.label"), shared("card.calibrating.pill"), FactTone.PRIMARY);
        }
        return new AnswerDraft(text, List.of(yieldCard,
                new FactDraft(shared("value.integer", counted), shared("card.estimated_plots.label", counted),
                        shared("card.of_active.pill", active), FactTone.HIGHLIGHT)),
                shared("source.thinning.field"), List.of());
    }

    private static FactDraft noYieldCard() {
        return new FactDraft(shared("card.no_yield.value"), shared("card.no_yield.label"),
                shared("card.no_yield.pill"), FactTone.PRIMARY);
    }
}
