package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.assistant.domain.services.ThinningDoneQuestions;

import java.util.List;
import java.util.Optional;

/**
 * The figures of a plot the screen shown after its thinning is recorded reads: the outcome of the confirmed thinning
 * of the campaign.
 *
 * @param campaignYear current campaign year
 * @param outcome      outcome of the confirmed thinning; null before the thinning is confirmed
 */
public record ThinningDoneFacts(int campaignYear, ThinningOutcomeData outcome) implements ScreenFacts {

    @Override
    public ScreenKey screen() {
        return ScreenKey.THINNING_DONE;
    }

    @Override
    public String canonical() {
        return "THINNING_DONE|" + campaignYear + "|" + (outcome == null ? null : outcome.canonical());
    }

    @Override
    public List<CatalogQuestion> questions() {
        return ThinningDoneQuestions.catalog(this);
    }

    @Override
    public Optional<AnswerDraft> answer(QuestionId questionId, PlotId plotId) {
        return ThinningDoneQuestions.answer(questionId, this);
    }
}
