package com.arcadiadevs.viora.platform.thinning.domain.services;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.SamplingRoundSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.SamplingRound;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.TreeTag;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Domain Service evaluating whether cumulative field samplings fulfill statistical representativeness.
 */
public final class SamplingCoverageEvaluator {

    /**
     * Minimum number of evaluated representative trees required to authorize an agronomic thinning prescription.
     */
    public static final int MIN_REPRESENTATIVE_TREES = 5;

    private SamplingCoverageEvaluator() {
    }

    /**
     * Calculates the count of unique evaluated trees across all sampling rounds.
     *
     * @param rounds the sampling rounds associated with the prescription
     * @return unique count of trees evaluated
     */
    public static int countUniqueEvaluatedTrees(List<SamplingRound> rounds) {
        if (rounds == null || rounds.isEmpty()) {
            return 0;
        }
        Set<TreeTag> uniqueTags = new HashSet<>();
        for (SamplingRound round : rounds) {
            for (TreeSamplingRecord record : round.samplingRecords()) {
                uniqueTags.add(record.treeTag());
            }
        }
        return uniqueTags.size();
    }

    /**
     * Calculates the count of unique evaluated trees across all sampling round snapshots.
     *
     * @param roundSnapshots the sampling round snapshots
     * @return unique count of trees evaluated
     */
    public static int countUniqueEvaluatedTreesFromSnapshots(List<SamplingRoundSnapshot> roundSnapshots) {
        if (roundSnapshots == null || roundSnapshots.isEmpty()) {
            return 0;
        }
        Set<TreeTag> uniqueTags = new HashSet<>();
        for (var round : roundSnapshots) {
            for (var record : round.samplingRecords()) {
                uniqueTags.add(record.treeTag());
            }
        }
        return uniqueTags.size();
    }

    /**
     * Determines whether the evaluated rounds satisfy the minimum statistical representativeness threshold.
     *
     * @param rounds the sampling rounds
     * @return {@code true} if unique evaluated trees $\ge 5$; {@code false} otherwise
     */
    public static boolean isRepresentative(List<SamplingRound> rounds) {
        return countUniqueEvaluatedTrees(rounds) >= MIN_REPRESENTATIVE_TREES;
    }

    /**
     * Determines whether the evaluated round snapshots satisfy the minimum statistical representativeness threshold.
     *
     * @param roundSnapshots the sampling round snapshots
     * @return {@code true} if unique evaluated trees $\ge 5$; {@code false} otherwise
     */
    public static boolean isRepresentativeFromSnapshots(List<SamplingRoundSnapshot> roundSnapshots) {
        return countUniqueEvaluatedTreesFromSnapshots(roundSnapshots) >= MIN_REPRESENTATIVE_TREES;
    }

    /**
     * Computes the number of additional trees needed to reach representativeness.
     *
     * @param rounds the sampling rounds
     * @return non-negative integer of trees needed
     */
    public static int treesNeeded(List<SamplingRound> rounds) {
        int evaluated = countUniqueEvaluatedTrees(rounds);
        return Math.max(0, MIN_REPRESENTATIVE_TREES - evaluated);
    }

    /**
     * Computes the number of additional trees needed to reach representativeness from snapshots.
     *
     * @param roundSnapshots the sampling round snapshots
     * @return non-negative integer of trees needed
     */
    public static int treesNeededFromSnapshots(List<SamplingRoundSnapshot> roundSnapshots) {
        int evaluated = countUniqueEvaluatedTreesFromSnapshots(roundSnapshots);
        return Math.max(0, MIN_REPRESENTATIVE_TREES - evaluated);
    }

    /**
     * Computes the total number of shoots evaluated across all rounds.
     *
     * @param rounds the sampling rounds
     * @return total counted shoots
     */
    public static int countTotalShoots(List<SamplingRound> rounds) {
        if (rounds == null || rounds.isEmpty()) {
            return 0;
        }
        int totalShoots = 0;
        for (SamplingRound round : rounds) {
            for (TreeSamplingRecord record : round.samplingRecords()) {
                totalShoots += record.shootFruitCount().shootCount();
            }
        }
        return totalShoots;
    }

    /**
     * Computes the total number of shoots evaluated across all round snapshots.
     *
     * @param roundSnapshots the sampling round snapshots
     * @return total counted shoots
     */
    public static int countTotalShootsFromSnapshots(List<SamplingRoundSnapshot> roundSnapshots) {
        if (roundSnapshots == null || roundSnapshots.isEmpty()) {
            return 0;
        }
        int totalShoots = 0;
        for (var round : roundSnapshots) {
            for (var record : round.samplingRecords()) {
                totalShoots += record.shootFruitCount().shootCount();
            }
        }
        return totalShoots;
    }

    /**
     * Computes the average fruit set density per linear canopy meter based on shoots.
     *
     * <p>Standard agronomic conversion for olive: 1 representative shoot is approximately 0.20 m (20 cm) of canopy length.</p>
     *
     * @param rounds the sampling rounds
     * @return mean fruits per linear meter (rounded to 2 decimal places)
     */
    public static double computeMeanFruitsPerMeter(List<SamplingRound> rounds) {
        if (rounds == null || rounds.isEmpty()) {
            return 0.0;
        }
        int totalFruits = 0;
        int totalShoots = 0;
        for (SamplingRound round : rounds) {
            for (TreeSamplingRecord record : round.samplingRecords()) {
                totalFruits += record.shootFruitCount().fruitSetCount();
                totalShoots += record.shootFruitCount().shootCount();
            }
        }
        if (totalShoots == 0) {
            return 0.0;
        }
        // fruits per shoot / 0.20m canopy length = fruits per meter
        double fruitsPerShoot = (double) totalFruits / totalShoots;
        double fruitsPerMeter = fruitsPerShoot / 0.20;
        return Math.round(fruitsPerMeter * 100.0) / 100.0;
    }

    /**
     * Computes the average fruit set density per linear canopy meter based on shoots from snapshots.
     *
     * @param roundSnapshots the sampling round snapshots
     * @return mean fruits per linear meter (rounded to 2 decimal places)
     */
    public static double computeMeanFruitsPerMeterFromSnapshots(List<SamplingRoundSnapshot> roundSnapshots) {
        if (roundSnapshots == null || roundSnapshots.isEmpty()) {
            return 0.0;
        }
        int totalFruits = 0;
        int totalShoots = 0;
        for (var round : roundSnapshots) {
            for (var record : round.samplingRecords()) {
                totalFruits += record.shootFruitCount().fruitSetCount();
                totalShoots += record.shootFruitCount().shootCount();
            }
        }
        if (totalShoots == 0) {
            return 0.0;
        }
        double fruitsPerShoot = (double) totalFruits / totalShoots;
        double fruitsPerMeter = fruitsPerShoot / 0.20;
        return Math.round(fruitsPerMeter * 100.0) / 100.0;
    }
}
