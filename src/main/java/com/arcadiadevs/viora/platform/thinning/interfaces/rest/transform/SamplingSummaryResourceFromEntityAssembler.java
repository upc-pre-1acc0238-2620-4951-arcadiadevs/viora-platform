package com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingDetailedResult;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingStatisticalSummary;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingTreeObservation;
import com.arcadiadevs.viora.platform.thinning.domain.services.SamplingCoverageEvaluator;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.SamplingDetailedResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.SamplingSummaryResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.SamplingTreeResource;

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
        int totalFruits = SamplingCoverageEvaluator.countTotalFruitsFromSnapshots(rounds);
        double meanFruits = SamplingCoverageEvaluator.computeMeanFruitsPerShootFromSnapshots(rounds);
        boolean isRepresentative = SamplingCoverageEvaluator.isRepresentativeFromSnapshots(rounds);
        int needed = SamplingCoverageEvaluator.treesNeededFromSnapshots(rounds);

        return new SamplingSummaryResource(
                snapshot.plotId().plotId(),
                snapshot.campaignYear().value(),
                uniqueTrees,
                totalShoots,
                totalFruits,
                ThinningRounding.load(meanFruits),
                isRepresentative,
                needed,
                ThinningRounding.LOAD_UNIT
        );
    }

    /**
     * Converts the query-service statistical summary into the existing GET sampling resource.
     *
     * @param summary statistical sampling summary
     * @return the presentation DTO
     */
    public static SamplingSummaryResource toResourceFromDomain(SamplingStatisticalSummary summary) {
        if (summary == null) {
            return null;
        }
        return new SamplingSummaryResource(
                summary.plotId().plotId(),
                summary.campaignYear().value(),
                summary.sampledTreesCount(),
                summary.sampledShootsCount(),
                summary.sampledFruitSetCount(),
                ThinningRounding.load(summary.meanFruitsPerShoot()),
                summary.isRepresentative(),
                summary.treesNeeded(),
                ThinningRounding.LOAD_UNIT
        );
    }

    /**
     * Converts the detailed query result into a REST resource.
     *
     * @param detailedResult detailed sampling result
     * @return the detailed presentation DTO
     */
    public static SamplingDetailedResource toDetailedResource(SamplingDetailedResult detailedResult) {
        if (detailedResult == null) {
            return null;
        }
        var summary = detailedResult.summary();
        var trees = detailedResult.observations().stream()
                .map(SamplingSummaryResourceFromEntityAssembler::toTreeResource)
                .toList();
        return new SamplingDetailedResource(
                summary.plotId().plotId(),
                summary.campaignYear().value(),
                summary.sampledTreesCount(),
                summary.sampledShootsCount(),
                summary.sampledFruitSetCount(),
                ThinningRounding.load(summary.meanFruitsPerShoot()),
                summary.isRepresentative(),
                summary.treesNeeded(),
                ThinningRounding.LOAD_UNIT,
                trees
        );
    }

    private static SamplingTreeResource toTreeResource(SamplingTreeObservation observation) {
        return new SamplingTreeResource(
                observation.roundId().roundId(),
                observation.treeTag().value(),
                observation.shootCount(),
                observation.fruitSetCount(),
                observation.trunkDiameterMm(),
                observation.samplingDate()
        );
    }
}
