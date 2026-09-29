package com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.services.SamplingCoverageEvaluator;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.SamplingSummaryResource;

/**
 * REST resource assembler mapping {@link FruitThinningPrescription} aggregate to presentation {@link SamplingSummaryResource}.
 */
public final class SamplingSummaryResourceFromEntityAssembler {

    private SamplingSummaryResourceFromEntityAssembler() {
    }

    /**
     * Converts a prescription aggregate to a sampling summary presentation resource.
     *
     * @param prescription the thinning prescription aggregate
     * @return the presentation DTO
     */
    public static SamplingSummaryResource toResource(FruitThinningPrescription prescription) {
        if (prescription == null) {
            return null;
        }
        var snapshot = prescription.snapshot();
        var rounds = snapshot.samplingRounds();

        int uniqueTrees = SamplingCoverageEvaluator.countUniqueEvaluatedTreesFromSnapshots(rounds);
        int totalShoots = SamplingCoverageEvaluator.countTotalShootsFromSnapshots(rounds);
        double meanFruits = SamplingCoverageEvaluator.computeMeanFruitsPerMeterFromSnapshots(rounds);
        boolean isRepresentative = SamplingCoverageEvaluator.isRepresentativeFromSnapshots(rounds);
        int needed = SamplingCoverageEvaluator.treesNeededFromSnapshots(rounds);

        return new SamplingSummaryResource(
                snapshot.plotId().plotId(),
                snapshot.campaignYear().value(),
                uniqueTrees,
                totalShoots,
                meanFruits,
                isRepresentative,
                needed
        );
    }
}
