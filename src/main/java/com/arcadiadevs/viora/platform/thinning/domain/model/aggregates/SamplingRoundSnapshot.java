package com.arcadiadevs.viora.platform.thinning.domain.model.aggregates;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.RoundId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingBatchId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.UserId;

import java.util.List;

/**
 * Immutable snapshot representing the state of a field sampling round entity.
 *
 * @param id               unique identifier of the sampling round
 * @param actorId          identifier of the user who performed the sampling
 * @param batchId          field sampling batch identifier
 * @param isRepresentative flag indicating if this round satisfies statistical coverage
 * @param samplingRecords  list of tree evaluations conducted in this round
 */
public record SamplingRoundSnapshot(
        RoundId id,
        UserId actorId,
        SamplingBatchId batchId,
        Boolean isRepresentative,
        List<TreeSamplingRecordSnapshot> samplingRecords
) {
    /**
     * Compact constructor creating an immutable defensive copy of the records list.
     */
    public SamplingRoundSnapshot {
        samplingRecords = (samplingRecords == null) ? List.of() : List.copyOf(samplingRecords);
    }
}
