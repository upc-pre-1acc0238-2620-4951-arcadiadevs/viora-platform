package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.assistant.domain.services.PlotDetailQuestions;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The figures of a plot the plot detail screen reads: the thinning plan of the campaign, its bearing index, its open
 * alerts and today's forecast. A figure the plot does not have yet is null (or empty).
 *
 * @param today            the current day in the producers' time zone, which the thinning window and the forecast
 *                         of today are judged against
 * @param campaignYear     current campaign year
 * @param plan             thinning plan of the campaign; null when the plot was not sampled
 * @param treesNeeded      trees still missing for a representative sampling
 * @param bearingIndex     latest Biennial Bearing Index; null while it cannot be calculated yet
 * @param bearingCategory  alternation band of that index
 * @param harvestCampaigns campaigns with a registered harvest
 * @param openAlerts       open heat, frost and hydric alerts of the plot
 * @param thresholds       the alert thresholds Telemetry publishes
 * @param forecast         the seven-day forecast; null when unavailable
 */
public record PlotDetailFacts(
        LocalDate today,
        int campaignYear,
        ThinningPlanData plan,
        int treesNeeded,
        Double bearingIndex,
        String bearingCategory,
        int harvestCampaigns,
        List<ClimateData.Incident> openAlerts,
        ClimateData.Thresholds thresholds,
        ClimateData.Forecast forecast
) implements ScreenFacts {

    public PlotDetailFacts {
        if (today == null) {
            throw new IllegalArgumentException("assistant.plot_detail.invalid");
        }
        openAlerts = openAlerts == null ? List.of() : openAlerts.stream()
                .filter(incident -> ClimateData.ALERT_TYPES.contains(incident.type()))
                .toList();
    }

    /**
     * Whether a tree was sampled in the campaign.
     *
     * @return true with a sampled tree
     */
    public boolean sampled() {
        return plan != null && plan.sampledTrees() > 0;
    }

    @Override
    public ScreenKey screen() {
        return ScreenKey.PLOT_DETAIL;
    }

    @Override
    public String canonical() {
        return "PLOT_DETAIL|" + today + "|" + campaignYear + "|" + (plan == null ? null : plan.canonical()) + "|"
                + treesNeeded + "|" + bearingIndex + "|" + bearingCategory + "|" + harvestCampaigns + "|"
                + openAlerts.stream().map(ClimateData.Incident::canonical).collect(Collectors.joining(",")) + "|"
                + (thresholds == null ? null : thresholds.canonical()) + "|"
                + (forecast == null ? null : forecast.canonical());
    }

    @Override
    public List<CatalogQuestion> questions() {
        return PlotDetailQuestions.catalog(this);
    }

    @Override
    public Optional<AnswerDraft> answer(QuestionId questionId, PlotId plotId) {
        return PlotDetailQuestions.answer(questionId, this);
    }
}
