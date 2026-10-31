package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import com.arcadiadevs.viora.platform.assistant.domain.services.PlanQuestions;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * The figures of a plot the thinning plan screen reads. A figure the plot does not have yet is null (or zero).
 *
 * @param campaignYear              current campaign year
 * @param preThinningFruitsPerShoot load of the representative sampling, before thinning
 * @param targetFruitsPerShoot      load the tree sustains, from the prescription
 * @param percentageToRemove        percentage of fruits to remove the plan computed
 * @param windowOpensOn             first recommended thinning day
 * @param windowClosesOn            last recommended thinning day
 * @param sampledTrees              distinct trees sampled in the campaign
 * @param prescriptionIssued        whether the prescription of the campaign was issued; only the catalog reads it
 * @param issuedPercentage          percentage of the issued prescription; null while not issued; only the catalog
 *                                  reads it
 * @param treesNeeded               trees still missing for a representative sampling; only the catalog reads it
 */
public record PlanFacts(
        int campaignYear,
        Double preThinningFruitsPerShoot,
        Double targetFruitsPerShoot,
        Double percentageToRemove,
        LocalDate windowOpensOn,
        LocalDate windowClosesOn,
        int sampledTrees,
        boolean prescriptionIssued,
        Double issuedPercentage,
        int treesNeeded
) implements ScreenFacts {

    @Override
    public ScreenKey screen() {
        return ScreenKey.PLAN;
    }

    @Override
    public String canonical() {
        return "PLAN|" + campaignYear + "|" + preThinningFruitsPerShoot + "|" + targetFruitsPerShoot + "|"
                + percentageToRemove + "|" + windowOpensOn + "|" + windowClosesOn + "|" + sampledTrees;
    }

    @Override
    public List<CatalogQuestion> questions() {
        return PlanQuestions.catalog(this);
    }

    @Override
    public Optional<AnswerDraft> answer(QuestionId questionId, PlotId plotId) {
        return PlanQuestions.answer(questionId, this, plotId);
    }
}
