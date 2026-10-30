package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * Identifier of a closed assistant question, namespaced by its screen (for example {@code plan.if_less}).
 *
 * @param value the identifier
 */
public record QuestionId(String value) {

    private static final Pattern FORMAT = Pattern.compile("[a-z]+(_[a-z]+)*\\.[a-z]+(_[a-z]+)*");

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

    /**
     * Compact constructor validating the {@code screen.question} format.
     */
    public QuestionId {
        if (value == null || !FORMAT.matcher(value).matches()) {
            throw new IllegalArgumentException("assistant.question.invalid");
        }
    }
}
