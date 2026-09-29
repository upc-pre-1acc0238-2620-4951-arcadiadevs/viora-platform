package com.arcadiadevs.viora.platform.thinning.domain.model.aggregates;

import com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.SamplingRoundCompletedEvent;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingBatchId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FruitThinningPrescriptionTest {

    @Test
    @DisplayName("Should initialize aggregate in SAMPLING_IN_PROGRESS status")
    void shouldInitializeAggregateCorrectly() {
        var plotId = new PlotId(UUID.randomUUID().toString());
        var year = new CampaignYear(2026);

        var prescription = FruitThinningPrescription.createForPlot(plotId, year, 1L);
        var snapshot = prescription.snapshot();

        assertThat(snapshot.plotId()).isEqualTo(plotId);
        assertThat(snapshot.campaignYear()).isEqualTo(year);
        assertThat(snapshot.status()).isEqualTo(PrescriptionStatus.SAMPLING_IN_PROGRESS);
        assertThat(snapshot.samplingRounds()).isEmpty();
    }

    @Test
    @DisplayName("Should ingest batch, deduplicate clientBatchId, and emit event when reaching 5 trees")
    void shouldIngestBatchAndEmitEventOnFiveTrees() {
        var plotId = new PlotId(UUID.randomUUID().toString());
        var year = new CampaignYear(2026);
        var actorId = new UserId(UUID.randomUUID().toString());

        var prescription = FruitThinningPrescription.createForPlot(plotId, year, 1L);

        List<TreeSamplingRecord> recordsBatch1 = List.of(
                TreeSamplingRecord.create("T-01", 10, 100, 150.0, LocalDate.now()),
                TreeSamplingRecord.create("T-02", 12, 120, 160.0, LocalDate.now()),
                TreeSamplingRecord.create("T-03", 8, 80, 140.0, LocalDate.now())
        );

        prescription.ingestSamplingsBatch(actorId, new SamplingBatchId("batch-1"), recordsBatch1);

        assertThat(prescription.snapshot().samplingRounds()).hasSize(1);
        assertThat(prescription.snapshot().samplingRounds().get(0).isRepresentative()).isFalse();
        assertThat(prescription.domainEvents()).isEmpty();

        // Idempotent replay of batch-1 should be ignored
        prescription.ingestSamplingsBatch(actorId, new SamplingBatchId("batch-1"), recordsBatch1);
        assertThat(prescription.snapshot().samplingRounds()).hasSize(1);

        // Batch 2 with 2 more trees reaching 5 unique trees
        List<TreeSamplingRecord> recordsBatch2 = List.of(
                TreeSamplingRecord.create("T-04", 10, 110, 155.0, LocalDate.now()),
                TreeSamplingRecord.create("T-05", 14, 150, 170.0, LocalDate.now())
        );

        prescription.ingestSamplingsBatch(actorId, new SamplingBatchId("batch-2"), recordsBatch2);

        assertThat(prescription.snapshot().samplingRounds()).hasSize(2);
        assertThat(prescription.snapshot().samplingRounds().get(1).isRepresentative()).isTrue();
        assertThat(prescription.domainEvents()).hasSize(1);
        var event = (SamplingRoundCompletedEvent) prescription.domainEvents().iterator().next();
        assertThat(event.prescriptionId()).isEqualTo(prescription.snapshot().id().prescriptionId());
        assertThat(event.plotId()).isEqualTo(plotId.plotId());
        assertThat(event.evaluatedTrees()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should throw exception if duplicate tree tags exist in same batch")
    void shouldThrowExceptionOnDuplicateTreeTags() {
        var plotId = new PlotId(UUID.randomUUID().toString());
        var year = new CampaignYear(2026);
        var actorId = new UserId(UUID.randomUUID().toString());

        var prescription = FruitThinningPrescription.createForPlot(plotId, year, 1L);

        List<TreeSamplingRecord> duplicateBatch = List.of(
                TreeSamplingRecord.create("T-01", 10, 100, 150.0, LocalDate.now()),
                TreeSamplingRecord.create("T-01", 12, 120, 160.0, LocalDate.now())
        );

        assertThatThrownBy(() -> prescription.ingestSamplingsBatch(actorId, new SamplingBatchId("batch-dup"), duplicateBatch))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("thinning.tree_tag.duplicate");
    }
}
