package com.arcadiadevs.viora.platform.thinning.domain.services;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.SamplingRoundSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.SamplingRound;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingBatchId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class SamplingCoverageEvaluatorTest {

    @Test
    @DisplayName("Should correctly evaluate statistical representativeness and mean fruits per shoot")
    void shouldEvaluateRepresentativenessAndDensity() {
        var actorId = new UserId(UUID.randomUUID().toString());

        // Round 1: 3 trees, each with 10 shoots, each shoot 10 fruits -> 30 shoots, 300 fruits
        List<TreeSamplingRecord> round1Trees = List.of(
                TreeSamplingRecord.create("T-01", 10, 100, 150.0, LocalDate.now(ProducerTimeZone.ZONE)),
                TreeSamplingRecord.create("T-02", 10, 100, 150.0, LocalDate.now(ProducerTimeZone.ZONE)),
                TreeSamplingRecord.create("T-03", 10, 100, 150.0, LocalDate.now(ProducerTimeZone.ZONE))
        );
        SamplingRound round1 = SamplingRound.create(actorId, new SamplingBatchId("batch-1"), round1Trees);

        List<SamplingRound> rounds = List.of(round1);

        assertThat(SamplingCoverageEvaluator.countUniqueEvaluatedTrees(rounds)).isEqualTo(3);
        assertThat(SamplingCoverageEvaluator.isRepresentative(rounds)).isFalse();
        assertThat(SamplingCoverageEvaluator.treesNeeded(rounds)).isEqualTo(2);
        assertThat(SamplingCoverageEvaluator.countTotalShoots(rounds)).isEqualTo(30);

        // 300 fruits / 30 shoots = 10 fruits per shoot
        assertThat(SamplingCoverageEvaluator.computeMeanFruitsPerShoot(rounds)).isEqualTo(10.0);

        // Round 2: 2 more trees -> total 5 trees
        List<TreeSamplingRecord> round2Trees = List.of(
                TreeSamplingRecord.create("T-04", 10, 100, 150.0, LocalDate.now(ProducerTimeZone.ZONE)),
                TreeSamplingRecord.create("T-05", 10, 100, 150.0, LocalDate.now(ProducerTimeZone.ZONE))
        );
        SamplingRound round2 = SamplingRound.create(actorId, new SamplingBatchId("batch-2"), round2Trees);

        List<SamplingRound> roundsCombined = List.of(round1, round2);

        assertThat(SamplingCoverageEvaluator.countUniqueEvaluatedTrees(roundsCombined)).isEqualTo(5);
        assertThat(SamplingCoverageEvaluator.isRepresentative(roundsCombined)).isTrue();
        assertThat(SamplingCoverageEvaluator.treesNeeded(roundsCombined)).isEqualTo(0);
    }

    @Test
    @DisplayName("a tree registered again in a later round is corrected: only its latest record counts in the load")
    void latestRecordOfEachTreeCounts() {
        var actorId = new UserId(UUID.randomUUID().toString());
        var day = LocalDate.of(2026, 10, 28);
        var first = SamplingRound.create(actorId, new SamplingBatchId("batch-1"), List.of(
                TreeSamplingRecord.create("T-01", 10, 10, 150.0, day),
                TreeSamplingRecord.create("T-02", 10, 10, 150.0, day),
                TreeSamplingRecord.create("T-03", 10, 99, 150.0, day),
                TreeSamplingRecord.create("T-04", 10, 10, 150.0, day),
                TreeSamplingRecord.create("T-05", 10, 10, 150.0, day))).snapshot();
        // T-03 was typed wrong (99 fruits) and registered again with 30 the next day
        var second = SamplingRound.create(actorId, new SamplingBatchId("batch-2"), List.of(
                TreeSamplingRecord.create("T-03", 10, 30, 150.0, day.plusDays(1)))).snapshot();
        var firstSent = at(first, "2026-10-28T15:00:00Z");
        var secondSent = at(second, "2026-10-29T15:00:00Z");
        // The order of the list does not matter: the round sent last wins
        var snapshots = List.of(secondSent, firstSent);
        var rounds = snapshots.stream().map(SamplingRound::fromSnapshot).toList();

        assertThat(SamplingCoverageEvaluator.countUniqueEvaluatedTreesFromSnapshots(snapshots)).isEqualTo(5);
        assertThat(SamplingCoverageEvaluator.countTotalShootsFromSnapshots(snapshots)).isEqualTo(50);
        assertThat(SamplingCoverageEvaluator.countTotalFruitsFromSnapshots(snapshots)).isEqualTo(70);
        assertThat(SamplingCoverageEvaluator.computeMeanFruitsPerShootFromSnapshots(snapshots)).isEqualTo(1.4);
        assertThat(SamplingCoverageEvaluator.computeMeanFruitsPerShoot(rounds)).isEqualTo(1.4);
        assertThat(SamplingCoverageEvaluator.countTotalFruits(rounds)).isEqualTo(70);
        assertThat(SamplingCoverageEvaluator.latestRecordsFromSnapshots(snapshots))
                .extracting(record -> record.treeTag().value(), record -> record.shootFruitCount().fruitSetCount())
                .containsExactly(tuple("T-01", 10), tuple("T-02", 10), tuple("T-03", 30), tuple("T-04", 10),
                        tuple("T-05", 10));
    }

    private static SamplingRoundSnapshot at(SamplingRoundSnapshot round, String sentAt) {
        return new SamplingRoundSnapshot(round.id(), round.actorId(), round.batchId(), round.isRepresentative(),
                round.samplingRecords(), Instant.parse(sentAt));
    }
}
