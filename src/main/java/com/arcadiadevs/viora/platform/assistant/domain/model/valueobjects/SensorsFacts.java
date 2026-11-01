package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.assistant.domain.services.SensorsQuestions;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The figures of a plot the sensors screen reads: its linked nodes, their latest reading and how much the plot read
 * over the last days.
 *
 * @param nodes         nodes linked to the plot
 * @param latestReading latest reading of the plot, only when recent; null otherwise
 * @param coverage      days with readings over the last week
 */
public record SensorsFacts(
        List<ClimateData.SensorNode> nodes,
        ClimateData.Reading latestReading,
        ClimateData.Coverage coverage,
        List<ClimateData.PausedNode> pausedNodes
) implements ScreenFacts {

    public SensorsFacts {
        nodes = nodes == null ? List.of() : List.copyOf(nodes);
        pausedNodes = pausedNodes == null ? List.of() : List.copyOf(pausedNodes);
    }

    /**
     * The figures without paused nodes.
     */
    public SensorsFacts(List<ClimateData.SensorNode> nodes, ClimateData.Reading latestReading,
                        ClimateData.Coverage coverage) {
        this(nodes, latestReading, coverage, null);
    }

    /**
     * Whether a reading of the plot is simulated: unless every linked node is measured, it is.
     *
     * @return true with a virtual node
     */
    public boolean simulated() {
        return nodes.stream().anyMatch(ClimateData.SensorNode::simulated);
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.SENSORS;
    }

    @Override
    public String canonical() {
        return "SENSORS|" + nodes.stream().map(ClimateData.SensorNode::canonical).collect(Collectors.joining(","))
                + "|" + (latestReading == null ? null : latestReading.canonical()) + "|"
                + (coverage == null ? null : coverage.canonical())
                // Appended only when present, so the answers written before pauses existed keep their version
                + (pausedNodes.isEmpty() ? "" : "|" + pausedNodes.stream().map(ClimateData.PausedNode::canonical)
                        .collect(Collectors.joining(",")));
    }

    @Override
    public List<CatalogQuestion> questions() {
        return SensorsQuestions.catalog(this);
    }

    @Override
    public Optional<AnswerDraft> answer(QuestionId questionId, PlotId plotId) {
        return SensorsQuestions.answer(questionId, this);
    }
}
