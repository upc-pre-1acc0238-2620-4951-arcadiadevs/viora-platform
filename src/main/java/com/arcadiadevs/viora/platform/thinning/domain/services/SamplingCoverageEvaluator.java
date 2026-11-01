package com.arcadiadevs.viora.platform.thinning.domain.services;


import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.SamplingRoundSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.TreeSamplingRecordSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.SamplingRound;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.TreeTag;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

/**
 * Domain Service evaluating whether cumulative field samplings fulfill statistical representativeness.
 *
 * <p>A tree is counted once, with its latest record: registering a tree tag again in a later round corrects it.
 * The rounds are taken in the order they were sent, so the record sent last wins. Without this rule a corrected
 * tree entered the load twice (the wrong record and the right one) while it counted once for the coverage.</p>
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
        long totalShoots = 0;
        for (TreeSamplingRecord record : latestRecords(rounds)) {
            totalShoots += record.shootFruitCount().shootCount();
        }
        return saturated(totalShoots);
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
        long totalShoots = 0;
        for (var record : latestRecordsFromSnapshots(roundSnapshots)) {
            totalShoots += record.shootFruitCount().shootCount();
        }
        return saturated(totalShoots);
    }

    /**
     * Computes the mean fruits per sampled shoot: all counted fruits over all counted shoots.
     *
     * <p>This is the load the producer actually counts. No shoot length is assumed: converting to
     * fruits per meter would need the real length of the sampled shoots, which is not measured.</p>
     *
     * @param rounds the sampling rounds
     * @return mean fruits per shoot (full precision; only the REST layer rounds)
     */
    public static double computeMeanFruitsPerShoot(List<SamplingRound> rounds) {
        if (rounds == null || rounds.isEmpty()) {
            return 0.0;
        }
        long totalFruits = 0;
        long totalShoots = 0;
        for (TreeSamplingRecord record : latestRecords(rounds)) {
            totalFruits += record.shootFruitCount().fruitSetCount();
            totalShoots += record.shootFruitCount().shootCount();
        }
        return totalShoots == 0 ? 0.0 : (double) totalFruits / totalShoots;
    }

    /**
     * Computes the mean fruits per sampled shoot from round snapshots.
     *
     * @param roundSnapshots the sampling round snapshots
     * @return mean fruits per shoot (full precision; only the REST layer rounds)
     */
    public static double computeMeanFruitsPerShootFromSnapshots(List<SamplingRoundSnapshot> roundSnapshots) {
        if (roundSnapshots == null || roundSnapshots.isEmpty()) {
            return 0.0;
        }
        long totalFruits = 0;
        long totalShoots = 0;
        for (var record : latestRecordsFromSnapshots(roundSnapshots)) {
            totalFruits += record.shootFruitCount().fruitSetCount();
            totalShoots += record.shootFruitCount().shootCount();
        }
        return totalShoots == 0 ? 0.0 : (double) totalFruits / totalShoots;
    }

    /**
     * Computes the total number of set fruits evaluated across all rounds.
     *
     * @param rounds the sampling rounds
     * @return total counted fruits
     */
    public static int countTotalFruits(List<SamplingRound> rounds) {
        if (rounds == null || rounds.isEmpty()) {
            return 0;
        }
        long totalFruits = 0;
        for (TreeSamplingRecord record : latestRecords(rounds)) {
            totalFruits += record.shootFruitCount().fruitSetCount();
        }
        return saturated(totalFruits);
    }

    /**
     * Computes the total number of set fruits evaluated across all round snapshots.
     *
     * @param roundSnapshots the sampling round snapshots
     * @return total counted fruits
     */
    public static int countTotalFruitsFromSnapshots(List<SamplingRoundSnapshot> roundSnapshots) {
        if (roundSnapshots == null || roundSnapshots.isEmpty()) {
            return 0;
        }
        long totalFruits = 0;
        for (var record : latestRecordsFromSnapshots(roundSnapshots)) {
            totalFruits += record.shootFruitCount().fruitSetCount();
        }
        return saturated(totalFruits);
    }

    /**
     * The record that counts for each tree: the latest one of its tag, taking the rounds in the order they were
     * sent (and, within a round, the tags are unique).
     *
     * @param rounds the sampling rounds
     * @return one record per tree tag, in the order each tree was first sampled
     */
    public static List<TreeSamplingRecord> latestRecords(List<SamplingRound> rounds) {
        if (rounds == null || rounds.isEmpty()) {
            return List.of();
        }
        var latest = new LinkedHashMap<TreeTag, TreeSamplingRecord>();
        rounds.stream()
                .sorted(Comparator.comparing(SamplingRound::createdAt, Comparator.nullsFirst(Comparator.naturalOrder())))
                .forEach(round -> round.samplingRecords().forEach(record -> latest.put(record.treeTag(), record)));
        return List.copyOf(latest.values());
    }

    /**
     * The record that counts for each tree, from round snapshots (see {@link #latestRecords(List)}).
     *
     * @param roundSnapshots the sampling round snapshots
     * @return one record per tree tag, in the order each tree was first sampled
     */
    public static List<TreeSamplingRecordSnapshot> latestRecordsFromSnapshots(
            List<SamplingRoundSnapshot> roundSnapshots) {
        if (roundSnapshots == null || roundSnapshots.isEmpty()) {
            return List.of();
        }
        var latest = new LinkedHashMap<TreeTag, TreeSamplingRecordSnapshot>();
        roundSnapshots.stream()
                .sorted(Comparator.comparing(SamplingRoundSnapshot::createdAt,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .forEach(round -> round.samplingRecords().forEach(record -> latest.put(record.treeTag(), record)));
        return List.copyOf(latest.values());
    }

    /** Sums are kept in a long so they never wrap around; a total beyond an int is reported as its largest value. */
    private static int saturated(long total) {
        return (int) Math.min(total, Integer.MAX_VALUE);
    }
}
