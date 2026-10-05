package com.arcadiadevs.viora.platform.thinning.domain.model.entities;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.SamplingRoundSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.TreeSamplingRecordSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.RoundId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingBatchId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.UserId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Domain entity representing a field sampling round containing individual tree sampling records.
 */
public class SamplingRound {

    private final RoundId id;
    private final UserId actorId;
    private final SamplingBatchId batchId;
    private boolean isRepresentative;
    private final List<TreeSamplingRecord> samplingRecords;
    private final java.time.Instant createdAt;

    /**
     * Constructs a SamplingRound entity.
     *
     * @param id               round identifier
     * @param actorId          user who conducted sampling
     * @param batchId          field sampling batch identifier
     * @param isRepresentative flag indicating if round fulfills statistical representativeness
     * @param samplingRecords  evaluated trees
     * @param createdAt        creation timestamp
     */
    public SamplingRound(
            RoundId id,
            UserId actorId,
            SamplingBatchId batchId,
            boolean isRepresentative,
            List<TreeSamplingRecord> samplingRecords,
            java.time.Instant createdAt
    ) {
        this.id = id;
        this.actorId = actorId;
        this.batchId = batchId;
        this.isRepresentative = isRepresentative;
        this.samplingRecords = (samplingRecords == null) ? new ArrayList<>() : new ArrayList<>(samplingRecords);
        this.createdAt = createdAt != null ? createdAt : java.time.Instant.now();
    }

    /**
     * Constructs a SamplingRound entity defaulting createdAt to current time.
     *
     * @param id               round identifier
     * @param actorId          user who conducted sampling
     * @param batchId          field sampling batch identifier
     * @param isRepresentative flag indicating if round fulfills statistical representativeness
     * @param samplingRecords  evaluated trees
     */
    public SamplingRound(
            RoundId id,
            UserId actorId,
            SamplingBatchId batchId,
            boolean isRepresentative,
            List<TreeSamplingRecord> samplingRecords
    ) {
        this(id, actorId, batchId, isRepresentative, samplingRecords, java.time.Instant.now());
    }

    /**
     * Factory method creating a new sampling round.
     *
     * @param actorId         user who conducted sampling
     * @param batchId         field sampling batch identifier
     * @param samplingRecords evaluated trees
     * @return consistent SamplingRound
     */
    public static SamplingRound create(
            UserId actorId,
            SamplingBatchId batchId,
            List<TreeSamplingRecord> samplingRecords
    ) {
        return new SamplingRound(
                new RoundId(),
                actorId,
                batchId,
                false,
                samplingRecords,
                java.time.Instant.now()
        );
    }

    /**
     * Reconstitutes an entity from snapshot.
     *
     * @param snapshot the snapshot to restore from
     * @return reconstituted SamplingRound
     */
    public static SamplingRound fromSnapshot(SamplingRoundSnapshot snapshot) {
        List<TreeSamplingRecord> records = new ArrayList<>();
        if (snapshot.samplingRecords() != null) {
            for (TreeSamplingRecordSnapshot recordSnapshot : snapshot.samplingRecords()) {
                records.add(TreeSamplingRecord.fromSnapshot(recordSnapshot));
            }
        }
        return new SamplingRound(
                snapshot.id(),
                snapshot.actorId(),
                snapshot.batchId(),
                Boolean.TRUE.equals(snapshot.isRepresentative()),
                records,
                snapshot.createdAt() != null ? snapshot.createdAt() : java.time.Instant.now()
        );
    }

    /**
     * Creates an immutable snapshot of this sampling round.
     *
     * @return snapshot representation
     */
    public SamplingRoundSnapshot snapshot() {
        List<TreeSamplingRecordSnapshot> recordSnapshots = new ArrayList<>();
        for (TreeSamplingRecord record : samplingRecords) {
            recordSnapshots.add(record.snapshot());
        }
        return new SamplingRoundSnapshot(
                id,
                actorId,
                batchId,
                isRepresentative,
                recordSnapshots,
                createdAt
        );
    }

    public java.time.Instant createdAt() {
        return createdAt;
    }

    public RoundId id() {
        return id;
    }

    public UserId actorId() {
        return actorId;
    }

    public SamplingBatchId batchId() {
        return batchId;
    }

    public boolean isRepresentative() {
        return isRepresentative;
    }

    public void setRepresentative(boolean representative) {
        this.isRepresentative = representative;
    }

    public List<TreeSamplingRecord> samplingRecords() {
        return Collections.unmodifiableList(samplingRecords);
    }
}
