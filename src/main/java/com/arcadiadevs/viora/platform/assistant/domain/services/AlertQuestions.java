package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AlertFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.ForecastDay;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.forecastSource;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.label;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.question;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.round;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.shared;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.text;

/**
 * The questions of the alert screen (T15, P123-P125): which ones it offers and why one is disabled, and the template
 * each one is answered with. Every question is about the same open alert of the plot, with the threshold Telemetry
 * raised it with; the answers point to the alert's own steps and add no advice of their own.
 */
public final class AlertQuestions {

    private AlertQuestions() {
    }

    /**
     * Evaluates the questions of the screen against the figures of the plot. Without an open heat, frost or hydric
     * alert every question is disabled. The forecast tells when a heat or frost alert ends, never a hydric one.
     *
     * @param facts the figures of the plot
     * @return the questions in display order
     */
    public static List<CatalogQuestion> catalog(AlertFacts facts) {
        var incident = facts.incident();
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
        if (!forecastTells(incident)) {
            whenEndsMissing = CatalogText.of("assistant.reason.forecast_soil");
        } else if (facts.forecast() == null) {
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
     * Writes the answer to a question of the screen.
     *
     * @param questionId the question
     * @param facts      the figures of the plot
     * @return the answer, or empty when the question is not of the screen or its figures are missing
     */
    public static Optional<AnswerDraft> answer(QuestionId questionId, AlertFacts facts) {
        var incident = facts.incident();
        if (incident == null) {
            return Optional.empty();
        }
        if (questionId.equals(QuestionId.ALERT_WHAT_TO_DO)) {
            return Optional.of(whatToDo(incident));
        } else if (questionId.equals(QuestionId.ALERT_FRUITSET_EFFECT)) {
            return Optional.of(fruitsetEffect(incident));
        } else if (questionId.equals(QuestionId.ALERT_WHEN_ENDS)) {
            return whenEnds(facts, incident);
        } else if (questionId.equals(QuestionId.ALERT_PLOTS_AT_RISK)) {
            return plotsAtRisk(facts, incident);
        }
        return Optional.empty();
    }

    /** "¿Qué hago ante este golpe de calor?": the breach against the threshold and the mitigation steps done. */
    private static AnswerDraft whatToDo(ClimateData.Incident incident) {
        var q = "alert.what_to_do";
        var value = round(incident.value(), 1);
        var threshold = round(incident.threshold(), 1);
        var text = incident.steps() > 0
                ? text(q, "text", alertName(incident), value, incident.unit(), threshold, severityName(incident),
                        incident.completedSteps(), incident.steps())
                : text(q, "text.no_steps", alertName(incident), value, incident.unit(), threshold,
                        severityName(incident));
        return new AnswerDraft(text, List.of(valueCard(incident), severityCard(incident)), alertSource(incident),
                List.of());
    }

    /**
     * "¿Cómo afecta al cuajado?": the platform has no model of the effect of an alert on fruit set, so the answer
     * gives only the breach and says the effect is not estimated (pending agronomist validation).
     */
    private static AnswerDraft fruitsetEffect(ClimateData.Incident incident) {
        var text = text("alert.fruitset_effect", "text", alertName(incident), round(incident.value(), 1),
                incident.unit(), round(incident.threshold(), 1));
        return new AnswerDraft(text, List.of(valueCard(incident), severityCard(incident)), alertSource(incident),
                List.of());
    }

    /**
     * "¿Cuándo termina?": until when the forecast keeps the heat or frost of the alert, from the flags Telemetry set
     * on each forecast day. A hydric alert has no answer: the forecast does not anticipate soil moisture.
     */
    private static Optional<AnswerDraft> whenEnds(AlertFacts facts, ClimateData.Incident incident) {
        var forecast = facts.forecast();
        if (forecast == null || forecast.days().isEmpty() || facts.thresholds() == null) {
            return Optional.empty();
        }
        final Predicate<ForecastDay> atRisk;
        final double threshold;
        if (ClimateData.HEAT_WAVE.equals(incident.type())) {
            atRisk = ForecastDay::heatRisk;
            threshold = facts.thresholds().heatWarning();
        } else if (ClimateData.FROST_WARNING.equals(incident.type())) {
            atRisk = ForecastDay::frostRisk;
            threshold = facts.thresholds().frostWarning();
        } else {
            return Optional.empty();
        }
        var q = "alert.when_ends";
        var span = ForecastSpan.of(forecast.days(), atRisk);
        var name = alertName(incident);
        final CatalogText text;
        final CatalogText endValue;
        switch (span.state()) {
            case ENDS -> {
                text = text(q, "text.ends", name, TextDate.of(span.lastRiskOn()), span.riskDays(), span.days(),
                        threshold, TextDate.of(span.endsOn()));
                endValue = shared("card.ends.value", TextDate.shortOf(span.endsOn()));
            }
            case BEYOND -> {
                text = text(q, "text.beyond", name, TextDate.of(span.lastRiskOn()), span.riskDays(), span.days(),
                        threshold);
                endValue = shared("card.ends.beyond");
            }
            default -> {
                text = text(q, "text.clear", name, span.days(), threshold);
                endValue = shared("card.ends.clear");
            }
        }
        return Optional.of(new AnswerDraft(text, List.of(
                new FactDraft(shared("value.integer", span.riskDays()), shared("card.risk_days.label", span.riskDays()),
                        shared("card.threshold.pill", threshold, "°C"), FactTone.PRIMARY),
                new FactDraft(endValue, shared("card.ends.label"), shared("card.forecast_days.pill", span.days()),
                        FactTone.HIGHLIGHT)),
                forecastSource(forecast), List.of()));
    }

    /**
     * "¿Qué otros lotes están en riesgo?": how many active plots of the producer have an open alert of the same
     * type, this one included. Only the producer's own plots are counted.
     */
    private static Optional<AnswerDraft> plotsAtRisk(AlertFacts facts, ClimateData.Incident incident) {
        int atRisk = facts.plotsAtRisk();
        int active = facts.activePlots();
        if (atRisk < 1 || active < atRisk) {
            return Optional.empty();
        }
        var q = "alert.plots_at_risk";
        var name = alertName(incident);
        final CatalogText text;
        if (active == 1) {
            text = text(q, "text.single", name);
        } else if (atRisk == 1) {
            text = text(q, "text.only", name, active);
        } else {
            text = text(q, "text.many", name, active, atRisk);
        }
        return Optional.of(new AnswerDraft(text, List.of(
                new FactDraft(shared("value.integer", atRisk), shared("card.plots_at_risk.label", atRisk), name,
                        FactTone.PRIMARY),
                new FactDraft(shared("value.integer", active), shared("card.active_plots.label", active),
                        shared("card.own_plots.pill"), FactTone.HIGHLIGHT)),
                shared("source.open_alerts"), List.of()));
    }

    private static boolean forecastTells(ClimateData.Incident incident) {
        return ClimateData.HEAT_WAVE.equals(incident.type()) || ClimateData.FROST_WARNING.equals(incident.type());
    }

    /** "golpe de calor", "helada", "estrés hídrico". */
    private static CatalogText alertName(ClimateData.Incident incident) {
        return shared("alert." + incident.type());
    }

    private static CatalogText severityName(ClimateData.Incident incident) {
        return shared("severity." + incident.severity());
    }

    /** The value that breached the threshold, with the threshold as pill. */
    private static FactDraft valueCard(ClimateData.Incident incident) {
        return new FactDraft(shared("value.measure", round(incident.value(), 1), incident.unit()),
                shared("card.alert_value.label." + incident.type()),
                shared("card.threshold.pill", round(incident.threshold(), 1), incident.unit()), FactTone.PRIMARY);
    }

    /** The level of the alert, with the day it was raised as pill. */
    private static FactDraft severityCard(ClimateData.Incident incident) {
        return new FactDraft(shared("card.severity.value." + incident.severity()), shared("card.severity.label"),
                shared("card.since.pill", TextDate.shortOf(incident.triggeredOn())), FactTone.HIGHLIGHT);
    }

    private static CatalogText alertSource(ClimateData.Incident incident) {
        return shared("source.alert", TextDate.of(incident.triggeredOn()));
    }
}
