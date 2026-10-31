package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.ForecastDay;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData.SensorNode;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.forecastSource;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.label;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.question;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.round;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.shared;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.text;

/**
 * The questions of the climate screen (P90, P130-P132): which ones it offers and why one is disabled, and the
 * template each one is answered with. Irrigation is only answered from a real (not virtual) soil probe, and the
 * answer leaves the decision to the producer; a virtual node's reading is always labeled "dato simulado".
 */
public final class ClimateQuestions {

    private static final String CRITICAL = "CRITICAL";
    private static final String WARNING = "WARNING";
    private static final String OK = "OK";

    private ClimateQuestions() {
    }

    /**
     * Evaluates the questions of the screen against the figures of the plot: irrigation only with a real soil probe
     * reading recently (contract, section 0), the forecast questions with a forecast, the sensors with a linked node
     * reading recently.
     *
     * @param facts the figures of the plot
     * @return the questions in display order
     */
    public static List<CatalogQuestion> catalog(ClimateFacts facts) {
        boolean recent = facts.latestReading() != null;
        CatalogText irrigateMissing = null;
        if (facts.realProbes().isEmpty()) {
            irrigateMissing = CatalogText.of("assistant.reason.real_probe_missing");
        } else if (!recent) {
            irrigateMissing = CatalogText.of("assistant.reason.readings_missing");
        }
        var forecastMissing = facts.forecast() == null ? CatalogText.of("assistant.reason.forecast_missing") : null;
        CatalogText sensorsMissing = null;
        if (facts.nodes().isEmpty()) {
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

    /**
     * Writes the answer to a question of the screen.
     *
     * @param questionId the question
     * @param facts      the figures of the plot
     * @return the answer, or empty when the question is not of the screen or its figures are missing
     */
    public static Optional<AnswerDraft> answer(QuestionId questionId, ClimateFacts facts) {
        if (questionId.equals(QuestionId.CLIMATE_IRRIGATE)) {
            return irrigate(facts);
        } else if (questionId.equals(QuestionId.CLIMATE_FRUITSET_EFFECT)) {
            return fruitsetEffect(facts);
        } else if (questionId.equals(QuestionId.CLIMATE_HEAT_ENDS)) {
            return heatEnds(facts);
        } else if (questionId.equals(QuestionId.CLIMATE_SENSORS_SAY)) {
            return sensorsSay(facts);
        }
        return Optional.empty();
    }

    /**
     * "¿Debo regar esta semana?": the reading of the real probe against its thresholds, as Telemetry judges it. The
     * answer states the reading and leaves the decision to the producer: it never orders irrigation.
     */
    private static Optional<AnswerDraft> irrigate(ClimateFacts facts) {
        var reading = facts.latestReading();
        var probe = facts.realProbes().stream()
                .filter(node -> node.depthCm() != null && node.criticalMoisture() != null
                        && node.warningMoisture() != null)
                .findFirst();
        if (reading == null || probe.isEmpty()) {
            return Optional.empty();
        }
        var state = CRITICAL.equals(probe.get().moistureRisk()) ? CRITICAL
                : WARNING.equals(probe.get().moistureRisk()) ? WARNING : OK;
        int depth = probe.get().depthCm();
        double moisture = round(reading.soilMoisture(), 1);
        double warning = round(probe.get().warningMoisture(), 1);
        double critical = round(probe.get().criticalMoisture(), 1);
        return Optional.of(new AnswerDraft(
                text("climate.irrigate", "text." + state.toLowerCase(Locale.ROOT), depth, moisture, warning, critical),
                List.of(
                        new FactDraft(shared("value.measure", moisture, "%"), shared("card.soil_moisture.label"),
                                shared("card.probe.pill", depth), FactTone.PRIMARY),
                        new FactDraft(shared("value.measure", warning, "%"), shared("card.warning_moisture.label"),
                                shared("card.critical_moisture.pill", critical), FactTone.HIGHLIGHT)),
                shared("source.probe", depth), List.of()));
    }

    /**
     * "¿Cómo afecta el clima al cuajado?": the platform has no model of the effect of heat on fruit set, so the
     * answer gives only the hottest forecast day and the days at the heat threshold, and says the effect is not
     * estimated (pending agronomist validation).
     */
    private static Optional<AnswerDraft> fruitsetEffect(ClimateFacts facts) {
        if (facts.forecast() == null || facts.forecast().days().isEmpty() || facts.thresholds() == null) {
            return Optional.empty();
        }
        var days = facts.forecast().days();
        // The first of the hottest days
        var hottest = days.stream().max(Comparator.comparingDouble(ForecastDay::max)
                .thenComparing(ForecastDay::date, Comparator.reverseOrder())).orElseThrow();
        int heatDays = (int) days.stream().filter(ForecastDay::heatRisk).count();
        double max = round(hottest.max(), 1);
        double threshold = facts.thresholds().heatWarning();
        return Optional.of(new AnswerDraft(
                text("climate.fruitset_effect", heatDays > 0 ? "text.above" : "text.below", max,
                        TextDate.of(hottest.date()), heatDays, days.size(), threshold),
                List.of(
                        new FactDraft(shared("value.measure", max, "°C"), shared("card.hottest.label"),
                                shared("card.on.pill", TextDate.shortOf(hottest.date())), FactTone.PRIMARY),
                        new FactDraft(shared("value.integer", heatDays), shared("card.heat_days.label", heatDays),
                                shared("card.threshold.pill", threshold, "°C"), FactTone.HIGHLIGHT)),
                forecastSource(facts.forecast()), List.of()));
    }

    /**
     * "¿Cuándo baja el calor?": the days the forecast reaches the heat threshold, as Telemetry flagged them, and the
     * first day after the last of them.
     */
    private static Optional<AnswerDraft> heatEnds(ClimateFacts facts) {
        if (facts.forecast() == null || facts.forecast().days().isEmpty() || facts.thresholds() == null) {
            return Optional.empty();
        }
        var q = "climate.heat_ends";
        var span = ForecastSpan.of(facts.forecast().days(), ForecastDay::heatRisk);
        double threshold = facts.thresholds().heatWarning();
        final CatalogText text;
        final CatalogText endValue;
        switch (span.state()) {
            case ENDS -> {
                text = text(q, "text.ends", TextDate.of(span.lastRiskOn()), span.riskDays(), span.days(), threshold,
                        TextDate.of(span.endsOn()));
                endValue = shared("card.ends.value", TextDate.shortOf(span.endsOn()));
            }
            case BEYOND -> {
                text = text(q, "text.beyond", TextDate.of(span.lastRiskOn()), span.riskDays(), span.days(),
                        threshold);
                endValue = shared("card.ends.beyond");
            }
            default -> {
                text = text(q, "text.clear", span.days(), threshold);
                endValue = shared("card.ends.clear");
            }
        }
        return Optional.of(new AnswerDraft(text, List.of(
                new FactDraft(shared("value.integer", span.riskDays()), shared("card.heat_days.label", span.riskDays()),
                        shared("card.threshold.pill", threshold, "°C"), FactTone.PRIMARY),
                new FactDraft(endValue, shared("card.ends.label"), shared("card.forecast_days.pill", span.days()),
                        FactTone.HIGHLIGHT)),
                forecastSource(facts.forecast()), List.of()));
    }

    /**
     * "¿Qué dicen mis sensores?": the latest reading of the plot. Unless every linked node is measured, the reading
     * is labeled "dato simulado" (contract, section 3.3).
     */
    private static Optional<AnswerDraft> sensorsSay(ClimateFacts facts) {
        var reading = facts.latestReading();
        if (reading == null || facts.nodes().isEmpty()) {
            return Optional.empty();
        }
        boolean simulated = facts.nodes().stream().anyMatch(SensorNode::simulated);
        int nodes = facts.nodes().size();
        double temperature = round(reading.temperature(), 1);
        long humidity = (long) round(reading.humidity(), 0);
        double soil = round(reading.soilMoisture(), 1);
        return Optional.of(new AnswerDraft(
                text("climate.sensors_say", simulated ? "text.simulated" : "text.measured", nodes, temperature,
                        humidity, soil),
                List.of(
                        new FactDraft(shared("value.measure", temperature, "°C"), shared("card.air_temperature.label"),
                                simulated ? shared("card.simulated.pill") : shared("card.measured.pill"),
                                FactTone.PRIMARY),
                        new FactDraft(shared("value.measure", soil, "%"), shared("card.soil_moisture.label"),
                                shared("card.air_humidity.pill", humidity), FactTone.HIGHLIGHT)),
                simulated ? shared("source.virtual_node") : shared("source.nodes", nodes), List.of()));
    }
}
