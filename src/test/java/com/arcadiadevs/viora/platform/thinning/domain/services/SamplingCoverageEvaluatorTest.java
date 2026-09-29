package com.arcadiadevs.viora.platform.thinning.domain.services;

import com.arcadiadevs.viora.platform.thinning.domain.model.entities.SamplingRound;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingBatchId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SamplingCoverageEvaluatorTest {

    @Test
    @DisplayName("Should correctly evaluate statistical representativeness and mean fruits per meter")
    void shouldEvaluateRepresentativenessAndDensity() {
        var actorId = new UserId(UUID.randomUUID().toString());

        // Round 1: 3 trees, each with 10 shoots, each shoot 10 fruits -> 30 shoots, 300 fruits
        List<TreeSamplingRecord> round1Trees = List.of(
                TreeSamplingRecord.create("T-01", 10, 100, 150.0, LocalDate.now()),
                TreeSamplingRecord.create("T-02", 10, 100, 150.0, LocalDate.now()),
                TreeSamplingRecord.create("T-03", 10, 100, 150.0, LocalDate.now())
        );
        SamplingRound round1 = SamplingRound.create(actorId, new SamplingBatchId("batch-1"), round1Trees);

        List<SamplingRound> rounds = List.of(round1);

        assertThat(SamplingCoverageEvaluator.countUniqueEvaluatedTrees(rounds)).isEqualTo(3);
        assertThat(SamplingCoverageEvaluator.isRepresentative(rounds)).isFalse();
        assertThat(SamplingCoverageEvaluator.treesNeeded(rounds)).isEqualTo(2);
        assertThat(SamplingCoverageEvaluator.countTotalShoots(rounds)).isEqualTo(30);

        // 300 fruits / 30 shoots = 10 fruits/shoot -> 10 / 0.20m = 50.0 fruits/meter
        assertThat(SamplingCoverageEvaluator.computeMeanFruitsPerMeter(rounds)).isEqualTo(50.0);

        // Round 2: 2 more trees -> total 5 trees
        List<TreeSamplingRecord> round2Trees = List.of(
                TreeSamplingRecord.create("T-04", 10, 100, 150.0, LocalDate.now()),
                TreeSamplingRecord.create("T-05", 10, 100, 150.0, LocalDate.now())
        );
        SamplingRound round2 = SamplingRound.create(actorId, new SamplingBatchId("batch-2"), round2Trees);

        List<SamplingRound> roundsCombined = List.of(round1, round2);

        assertThat(SamplingCoverageEvaluator.countUniqueEvaluatedTrees(roundsCombined)).isEqualTo(5);
        assertThat(SamplingCoverageEvaluator.isRepresentative(roundsCombined)).isTrue();
        assertThat(SamplingCoverageEvaluator.treesNeeded(roundsCombined)).isEqualTo(0);
    }
}
