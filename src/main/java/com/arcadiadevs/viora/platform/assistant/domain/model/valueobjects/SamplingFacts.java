package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.assistant.domain.services.SamplingQuestions;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The figures of a plot the sampling summary screen reads: the sampling and the thinning plan of the campaign.
 *
 * @param today       the current day in the producers' time zone, which the thinning window is judged against
 * @param plan        thinning plan of the campaign; null when the plot was not sampled
 * @param treesNeeded trees still missing for a representative sampling
 */
public record SamplingFacts(LocalDate today, ThinningPlanData plan, int treesNeeded) implements ScreenFacts {

    public SamplingFacts {
        if (today == null) {
            throw new IllegalArgumentException("assistant.sampling_summary.invalid");
        }
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
        return ScreenKey.SAMPLING_SUMMARY;
    }

    @Override
    public String canonical() {
        return "SAMPLING_SUMMARY|" + today + "|" + (plan == null ? null : plan.canonical()) + "|" + treesNeeded;
    }

    @Override
    public List<CatalogQuestion> questions() {
        return SamplingQuestions.catalog(this);
    }

    @Override
    public Optional<AnswerDraft> answer(QuestionId questionId, PlotId plotId) {
        return SamplingQuestions.answer(questionId, this, plotId);
    }
}
