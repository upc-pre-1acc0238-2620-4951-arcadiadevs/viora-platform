package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.SensorsFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.util.List;
import java.util.Optional;

import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.label;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.question;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.round;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.shared;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.text;
import static com.arcadiadevs.viora.platform.assistant.domain.services.ScreenTexts.whole;

/**
 * The questions of the sensors screen (P85, P139-P141): which ones it offers and why one is disabled, and the
 * template each one is answered with. A reading is labeled "dato simulado" unless every linked node is measured.
 */
public final class SensorsQuestions {

    private SensorsQuestions() {
    }

    /**
     * Evaluates the questions of the screen against the figures of the plot. What the nodes say needs a linked node
     * reading recently, as {@code climate.sensors_say}; the reliability needs a linked node; how to link one is
     * always offered. {@code why_paused} is always disabled: Telemetry has no way to pause a node yet, so no node can
     * be paused (docs/asistente-decisiones.md, Pantalla Sensores).
     *
     * @param facts the figures of the plot
     * @return the questions in display order
     */
    public static List<CatalogQuestion> catalog(SensorsFacts facts) {
        var nodesMissing = facts.nodes().isEmpty() ? CatalogText.of("assistant.reason.nodes_missing") : null;
        CatalogText whatSayMissing = nodesMissing;
        if (whatSayMissing == null && facts.latestReading() == null) {
            whatSayMissing = CatalogText.of("assistant.reason.readings_missing");
        }
        return List.of(
                question(QuestionId.SENSORS_WHAT_SAY, label(QuestionId.SENSORS_WHAT_SAY, "label"), "sensor",
                        whatSayMissing),
                question(QuestionId.SENSORS_WHY_PAUSED, label(QuestionId.SENSORS_WHY_PAUSED, "label"), "alert",
                        CatalogText.of("assistant.reason.no_paused_nodes")),
                question(QuestionId.SENSORS_RELIABILITY, label(QuestionId.SENSORS_RELIABILITY, "label"), "history",
                        nodesMissing),
                question(QuestionId.SENSORS_HOW_LINK, label(QuestionId.SENSORS_HOW_LINK, "label"), "lote", null));
    }

    /**
     * Writes the answer to a question of the screen. {@code why_paused} has no answer.
     *
     * @param questionId the question
     * @param facts      the figures of the plot
     * @return the answer, or empty when the question is not of the screen or its figures are missing
     */
    public static Optional<AnswerDraft> answer(QuestionId questionId, SensorsFacts facts) {
        if (questionId.equals(QuestionId.SENSORS_WHAT_SAY)) {
            return whatSay(facts);
        } else if (questionId.equals(QuestionId.SENSORS_RELIABILITY)) {
            return reliability(facts);
        } else if (questionId.equals(QuestionId.SENSORS_HOW_LINK)) {
            return Optional.of(howLink(facts));
        }
        return Optional.empty();
    }

    /**
     * "¿Qué dicen mis sensores?": the latest reading of the plot, as {@code climate.sensors_say} gives it, with the
     * texts of this screen.
     */
    private static Optional<AnswerDraft> whatSay(SensorsFacts facts) {
        var reading = facts.latestReading();
        if (reading == null || facts.nodes().isEmpty()) {
            return Optional.empty();
        }
        boolean simulated = facts.simulated();
        int nodes = facts.nodes().size();
        double temperature = round(reading.temperature(), 1);
        long humidity = whole(reading.humidity());
        double soil = round(reading.soilMoisture(), 1);
        var text = text("sensors.what_say", simulated ? "text.simulated" : "text.measured", nodes, temperature,
                humidity, soil);
        return Optional.of(new AnswerDraft(text, List.of(
                new FactDraft(shared("value.measure", temperature, "°C"), shared("card.air_temperature.label"),
                        originPill(simulated), FactTone.PRIMARY),
                new FactDraft(shared("value.measure", soil, "%"), shared("card.soil_moisture.label"),
                        shared("card.air_humidity.pill", humidity), FactTone.HIGHLIGHT)),
                nodesSource(simulated, nodes), List.of()));
    }

    /**
     * "¿Qué tan confiables son estas lecturas?": where the readings come from and on how many of the last days the
     * plot had readings. Telemetry keeps the readings per plot, so the coverage is that of the plot.
     */
    private static Optional<AnswerDraft> reliability(SensorsFacts facts) {
        var q = "sensors.reliability";
        var coverage = facts.coverage();
        if (coverage == null || facts.nodes().isEmpty()) {
            return Optional.empty();
        }
        boolean simulated = facts.simulated();
        int nodes = facts.nodes().size();
        var origin = simulated ? text(q, "origin.simulated") : text(q, "origin.measured", nodes);
        var days = coverage.daysWithReadings() > 0 && coverage.lastReadingOn() != null
                ? text(q, "coverage.days", coverage.daysWithReadings(), coverage.windowDays(),
                        TextDate.of(coverage.lastReadingOn()))
                : text(q, "coverage.none", coverage.windowDays());
        return Optional.of(new AnswerDraft(text(q, "text", origin, days), List.of(
                new FactDraft(shared("value.fraction", coverage.daysWithReadings(), coverage.windowDays()),
                        shared("card.days_with_readings.label"), shared("card.last_days.pill", coverage.windowDays()),
                        FactTone.PRIMARY),
                new FactDraft(simulated ? shared("card.origin.simulated") : shared("card.origin.measured"),
                        shared("card.origin.label"), originPill(simulated), FactTone.HIGHLIGHT)),
                nodesSource(simulated, nodes), List.of()));
    }

    /**
     * "¿Cómo vinculo un nodo?": the nodes the plot already has and what Telemetry asks to link one (a name unique in
     * the plot, its type and, for a soil probe, its depth). No button: the app has no agreed route to the node form.
     */
    private static AnswerDraft howLink(SensorsFacts facts) {
        var q = "sensors.how_link";
        int nodes = facts.nodes().size();
        int probes = (int) facts.nodes().stream().filter(node -> ClimateData.SOIL_PROBE.equals(node.type())).count();
        int microclimate = nodes - probes;
        var state = nodes == 0 ? text(q, "state.none") : text(q, "state.some", nodes);
        return new AnswerDraft(text(q, "text", state, text(q, "steps")), List.of(
                new FactDraft(shared("value.integer", microclimate), shared("card.microclimate.label", microclimate),
                        shared("card.this_plot.pill"), FactTone.PRIMARY),
                new FactDraft(shared("value.integer", probes), shared("card.soil_probes.label", probes),
                        shared("card.this_plot.pill"), FactTone.HIGHLIGHT)),
                shared("source.linked_nodes"), List.of());
    }

    private static CatalogText originPill(boolean simulated) {
        return simulated ? shared("card.simulated.pill") : shared("card.measured.pill");
    }

    private static CatalogText nodesSource(boolean simulated, int nodes) {
        return simulated ? shared("source.virtual_node") : shared("source.nodes", nodes);
    }
}
