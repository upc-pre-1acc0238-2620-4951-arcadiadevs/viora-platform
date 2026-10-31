package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.assistant.domain.services.ClimateQuestions;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The figures of a plot the climate screen reads: the forecast, the linked nodes and their latest reading.
 *
 * @param thresholds    the alert thresholds Telemetry publishes
 * @param forecast      the seven-day forecast; null when unavailable
 * @param nodes         nodes linked to the plot
 * @param latestReading latest reading of the plot, only when recent; null otherwise
 */
public record ClimateFacts(
        ClimateData.Thresholds thresholds,
        ClimateData.Forecast forecast,
        List<ClimateData.SensorNode> nodes,
        ClimateData.Reading latestReading
) implements ScreenFacts {

    public ClimateFacts {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
    }

    /**
     * Soil probes whose readings are measured, not simulated.
     *
     * @return the real probes
     */
    public List<ClimateData.SensorNode> realProbes() {
        return nodes.stream()
                .filter(node -> ClimateData.SOIL_PROBE.equals(node.type()) && !node.simulated())
                .toList();
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.CLIMATE;
    }

    @Override
    public String canonical() {
        return "CLIMATE|" + (thresholds == null ? null : thresholds.canonical()) + "|"
                + (forecast == null ? null : forecast.canonical()) + "|"
                + nodes.stream().map(ClimateData.SensorNode::canonical).collect(Collectors.joining(",")) + "|"
                + (latestReading == null ? null : latestReading.canonical());
    }

    @Override
    public List<CatalogQuestion> questions() {
        return ClimateQuestions.catalog(this);
    }

    @Override
    public Optional<AnswerDraft> answer(QuestionId questionId, PlotId plotId) {
        return ClimateQuestions.answer(questionId, this);
    }
}
