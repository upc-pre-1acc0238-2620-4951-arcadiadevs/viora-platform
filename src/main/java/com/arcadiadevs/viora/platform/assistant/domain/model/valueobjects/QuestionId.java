package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * Identifier of a closed assistant question, namespaced by its screen (for example {@code plan.if_less}). Words after
 * the first may carry digits, as in {@code chill.reach_30}.
 *
 * @param value the identifier
 */
public record QuestionId(String value) {

    private static final Pattern FORMAT = Pattern.compile("[a-z]+(_[a-z]+)*\\.[a-z]+(_[a-z0-9]+)*");

    public static final QuestionId HOME_WEEK_PLAN = new QuestionId("home.week_plan");
    public static final QuestionId HOME_CAMPAIGN_VS_PREVIOUS = new QuestionId("home.campaign_vs_previous");
    public static final QuestionId HOME_ALERTS_AFFECTING = new QuestionId("home.alerts_affecting");
    public static final QuestionId HOME_HARVEST_ESTIMATE = new QuestionId("home.harvest_estimate");
    public static final QuestionId ALTERNATION_WHY_INDEX = new QuestionId("alternation.why_index");
    public static final QuestionId ALTERNATION_BREAK_CYCLE = new QuestionId("alternation.break_cycle");
    public static final QuestionId ALTERNATION_VS_CAMPAIGNS = new QuestionId("alternation.vs_campaigns");
    public static final QuestionId ALTERNATION_NEXT_YEAR = new QuestionId("alternation.next_year");
    public static final QuestionId PLAN_WHY_PERCENT = new QuestionId("plan.why_percent");
    public static final QuestionId PLAN_IF_LESS = new QuestionId("plan.if_less");
    public static final QuestionId PLAN_IF_NONE = new QuestionId("plan.if_none");
    public static final QuestionId PLAN_BEST_TIME = new QuestionId("plan.best_time");
    public static final QuestionId ALERT_WHAT_TO_DO = new QuestionId("alert.what_to_do");
    public static final QuestionId ALERT_FRUITSET_EFFECT = new QuestionId("alert.fruitset_effect");
    public static final QuestionId ALERT_WHEN_ENDS = new QuestionId("alert.when_ends");
    public static final QuestionId ALERT_PLOTS_AT_RISK = new QuestionId("alert.plots_at_risk");
    public static final QuestionId CLIMATE_IRRIGATE = new QuestionId("climate.irrigate");
    public static final QuestionId CLIMATE_FRUITSET_EFFECT = new QuestionId("climate.fruitset_effect");
    public static final QuestionId CLIMATE_HEAT_ENDS = new QuestionId("climate.heat_ends");
    public static final QuestionId CLIMATE_SENSORS_SAY = new QuestionId("climate.sensors_say");
    public static final QuestionId CHILL_MISSING = new QuestionId("chill.missing");
    public static final QuestionId CHILL_REACH_30 = new QuestionId("chill.reach_30");
    public static final QuestionId CHILL_VS_LAST_WINTER = new QuestionId("chill.vs_last_winter");
    public static final QuestionId CHILL_IF_INCOMPLETE = new QuestionId("chill.if_incomplete");
    public static final QuestionId PLOT_HOW_IS = new QuestionId("plot.how_is");
    public static final QuestionId PLOT_TODAY = new QuestionId("plot.today");
    public static final QuestionId PLOT_LAST_SAMPLING = new QuestionId("plot.last_sampling");
    public static final QuestionId PLOT_NEXT_YEAR_HARVEST = new QuestionId("plot.next_year_harvest");
    public static final QuestionId SENSORS_WHAT_SAY = new QuestionId("sensors.what_say");
    public static final QuestionId SENSORS_WHY_PAUSED = new QuestionId("sensors.why_paused");
    public static final QuestionId SENSORS_RELIABILITY = new QuestionId("sensors.reliability");
    public static final QuestionId SENSORS_HOW_LINK = new QuestionId("sensors.how_link");
    public static final QuestionId SAMPLING_ENOUGH_TREES = new QuestionId("sampling.enough_trees");
    public static final QuestionId SAMPLING_MEANING = new QuestionId("sampling.meaning");
    public static final QuestionId SAMPLING_NEXT = new QuestionId("sampling.next");
    public static final QuestionId SAMPLING_CORRECT_TREE = new QuestionId("sampling.correct_tree");
    public static final QuestionId THINNING_HARVEST_CHANGE = new QuestionId("thinning.harvest_change");
    public static final QuestionId THINNING_WHY_TARGET = new QuestionId("thinning.why_target");
    public static final QuestionId THINNING_NEXT_SAMPLING = new QuestionId("thinning.next_sampling");
    public static final QuestionId CAMPAIGN_VS_PREVIOUS = new QuestionId("campaign.vs_previous");
    public static final QuestionId CAMPAIGN_CALIBER_CHANGE = new QuestionId("campaign.caliber_change");
    public static final QuestionId CAMPAIGN_PLAN_NEXT = new QuestionId("campaign.plan_next");

    /**
     * Compact constructor validating the {@code screen.question} format.
     */
    public QuestionId {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("assistant.question.invalid");
        }
    }
}
