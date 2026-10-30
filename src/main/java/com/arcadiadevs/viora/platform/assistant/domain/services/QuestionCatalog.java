package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Owner of the closed question catalog: which questions each screen offers, in which order, with which icon,
 * and whether the data of the plot is enough to answer them. A question without enough data is offered disabled
 * with the reason, so the model is never asked to invent a figure.
 */
public final class QuestionCatalog {

    /** Points the {@code plan.if_less} scenario thins less than the prescription. */
    public static final int LESS_THINNING_POINTS = 10;

    private static final String PREFIX = "assistant.question.";

    /** Questions of each screen, in display order. */
    private static final Map<ScreenKey, List<QuestionId>> QUESTIONS = Map.of(
            ScreenKey.HOME, List.of(QuestionId.HOME_WEEK_PLAN, QuestionId.HOME_CAMPAIGN_VS_PREVIOUS,
                    QuestionId.HOME_ALERTS_AFFECTING, QuestionId.HOME_HARVEST_ESTIMATE),
            ScreenKey.ALTERNATION, List.of(QuestionId.ALTERNATION_WHY_INDEX, QuestionId.ALTERNATION_BREAK_CYCLE,
                    QuestionId.ALTERNATION_VS_CAMPAIGNS, QuestionId.ALTERNATION_NEXT_YEAR),
            ScreenKey.PLAN, List.of(QuestionId.PLAN_WHY_PERCENT, QuestionId.PLAN_IF_LESS, QuestionId.PLAN_IF_NONE,
                    QuestionId.PLAN_BEST_TIME),
            ScreenKey.ALERT, List.of(QuestionId.ALERT_WHAT_TO_DO, QuestionId.ALERT_FRUITSET_EFFECT,
                    QuestionId.ALERT_WHEN_ENDS, QuestionId.ALERT_PLOTS_AT_RISK),
            ScreenKey.CLIMATE, List.of(QuestionId.CLIMATE_IRRIGATE, QuestionId.CLIMATE_FRUITSET_EFFECT,
                    QuestionId.CLIMATE_HEAT_ENDS, QuestionId.CLIMATE_SENSORS_SAY));

    private QuestionCatalog() {
    }

    /**
     * Finds the screen a question belongs to.
     *
     * @param questionId the question
     * @return its screen, or empty for a question the catalog does not offer
     */
    public static Optional<ScreenKey> screenOf(QuestionId questionId) {
        return QUESTIONS.entrySet().stream()
                .filter(entry -> entry.getValue().contains(questionId))
                .map(Map.Entry::getKey)
                .findFirst();
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
            case HOME -> home(facts.home());
            case ALTERNATION -> alternation(facts);
            case PLAN -> plan(facts);
            case ALERT -> alert(facts.climate());
            case CLIMATE -> climate(facts.climate());
        };
    }

    /**
     * The HOME screen, about one plot or the producer's whole field: the week plan needs an active plot, the
     * comparison two campaigns of the same plots and the estimate an estimated caliber. The alerts question is
     * always offered; without alerts it says so.
     */
    private static List<CatalogQuestion> home(HomeData home) {
        var alertsLabel = home.fieldScope()
                ? label(QuestionId.HOME_ALERTS_AFFECTING, "label.field")
                : label(QuestionId.HOME_ALERTS_AFFECTING, "label");
        return List.of(
                question(QuestionId.HOME_WEEK_PLAN, label(QuestionId.HOME_WEEK_PLAN, "label"), "calendar",
                        home.activePlots() > 0 ? null : CatalogText.of("assistant.reason.plots_missing")),
                question(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, label(QuestionId.HOME_CAMPAIGN_VS_PREVIOUS, "label"),
                        "history", home.campaignComparison().isPresent()
                                ? null : CatalogText.of("assistant.reason.campaigns_missing")),
                question(QuestionId.HOME_ALERTS_AFFECTING, alertsLabel, "alert", null),
                question(QuestionId.HOME_HARVEST_ESTIMATE, label(QuestionId.HOME_HARVEST_ESTIMATE, "label"),
                        "harvest", home.caliberEstimates().isEmpty()
                                ? CatalogText.of("assistant.reason.caliber_missing") : null));
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

    /**
     * The ALERT screen is about the most severe open heat, frost or hydric alert of the plot: without one every
     * question is disabled. The forecast tells when a heat or frost alert ends, never a hydric one.
     */
    private static List<CatalogQuestion> alert(ClimateData climate) {
        var incident = climate.incident();
        if (incident == null) {
            var missing = CatalogText.of("assistant.reason.alert_missing");
            return List.of(
                    question(QuestionId.ALERT_WHAT_TO_DO, label(QuestionId.ALERT_WHAT_TO_DO, "label"), "alert",
                            missing),
                    question(QuestionId.ALERT_FRUITSET_EFFECT, label(QuestionId.ALERT_FRUITSET_EFFECT, "label"),
                            "fruit", missing),
                    question(QuestionId.ALERT_WHEN_ENDS, label(QuestionId.ALERT_WHEN_ENDS, "label"), "calendar",
                            missing),
                    question(QuestionId.ALERT_PLOTS_AT_RISK, label(QuestionId.ALERT_PLOTS_AT_RISK, "label"), "lote",
                            missing));
        }
        CatalogText whenEndsMissing = null;
        if (!ClimateData.HEAT_WAVE.equals(incident.type()) && !ClimateData.FROST_WARNING.equals(incident.type())) {
            whenEndsMissing = CatalogText.of("assistant.reason.forecast_soil");
        } else if (climate.forecast() == null) {
            whenEndsMissing = CatalogText.of("assistant.reason.forecast_missing");
        }
        return List.of(
                question(QuestionId.ALERT_WHAT_TO_DO, label(QuestionId.ALERT_WHAT_TO_DO, "label." + incident.type()),
                        "alert", null),
                question(QuestionId.ALERT_FRUITSET_EFFECT, label(QuestionId.ALERT_FRUITSET_EFFECT, "label"), "fruit",
                        null),
                question(QuestionId.ALERT_WHEN_ENDS, label(QuestionId.ALERT_WHEN_ENDS, "label"), "calendar",
                        whenEndsMissing),
                question(QuestionId.ALERT_PLOTS_AT_RISK, label(QuestionId.ALERT_PLOTS_AT_RISK, "label"), "lote",
                        null));
    }

    /**
     * The CLIMATE screen: irrigation only with a real (not virtual) soil probe reading recently (contract, section
     * 0), the forecast questions with a forecast, the sensors with a linked node reading recently.
     */
    private static List<CatalogQuestion> climate(ClimateData climate) {
        boolean recent = climate.latestReading() != null;
        CatalogText irrigateMissing = null;
        if (climate.realProbes().isEmpty()) {
            irrigateMissing = CatalogText.of("assistant.reason.real_probe_missing");
        } else if (!recent) {
            irrigateMissing = CatalogText.of("assistant.reason.readings_missing");
        }
        var forecastMissing = climate.forecast() == null ? CatalogText.of("assistant.reason.forecast_missing") : null;
        CatalogText sensorsMissing = null;
        if (climate.nodes().isEmpty()) {
            sensorsMissing = CatalogText.of("assistant.reason.nodes_missing");
        } else if (!recent) {
            sensorsMissing = CatalogText.of("assistant.reason.readings_missing");
        }
        return List.of(
                question(QuestionId.CLIMATE_IRRIGATE, label(QuestionId.CLIMATE_IRRIGATE, "label"), "water",
                        irrigateMissing),
                question(QuestionId.CLIMATE_FRUITSET_EFFECT, label(QuestionId.CLIMATE_FRUITSET_EFFECT, "label"),
                        "fruit", forecastMissing),
                question(QuestionId.CLIMATE_HEAT_ENDS, label(QuestionId.CLIMATE_HEAT_ENDS, "label"), "temp",
                        forecastMissing),
                question(QuestionId.CLIMATE_SENSORS_SAY, label(QuestionId.CLIMATE_SENSORS_SAY, "label"), "sensor",
                        sensorsMissing));
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
