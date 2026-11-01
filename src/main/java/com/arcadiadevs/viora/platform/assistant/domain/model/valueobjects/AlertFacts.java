package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.assistant.domain.services.AlertQuestions;

import java.util.List;
import java.util.Optional;

/**
 * The figures of a plot the alert screen reads: the most severe open heat, frost or hydric alert of the plot, how
 * many plots of the producer share it and, for heat and frost, the forecast that tells when it ends.
 *
 * @param thresholds  the alert thresholds Telemetry publishes
 * @param incident    the alert the screen is about; null without one
 * @param plotsAtRisk active plots of the producer with an open alert of the same type, this one included
 * @param activePlots active plots of the producer
 * @param forecast    the seven-day forecast; null when unavailable or not read (no alert, or a hydric one)
 */
public record AlertFacts(
        ClimateData.Thresholds thresholds,
        ClimateData.Incident incident,
        int plotsAtRisk,
        int activePlots,
        ClimateData.Forecast forecast
) implements ScreenFacts {

    @Override
    public ScreenKey screen() {
        return ScreenKey.ALERT;
    }

    @Override
    public String canonical() {
        return "ALERT|" + (thresholds == null ? null : thresholds.canonical()) + "|"
                + (incident == null ? null : incident.canonical()) + "|" + plotsAtRisk + "|" + activePlots + "|"
                + (forecast == null ? null : forecast.canonical());
    }

    @Override
    public List<CatalogQuestion> questions() {
        return AlertQuestions.catalog(this);
    }

    @Override
    public Optional<AnswerDraft> answer(QuestionId questionId, PlotId plotId) {
        return AlertQuestions.answer(questionId, this);
    }
}
