package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.util.List;

/**
 * Immutable domain result containing the sampling summary and per-observation detail.
 *
 * @param summary       statistical sampling summary
 * @param observations  ordered tree observations across sampling rounds
 */
public record SamplingDetailedResult(
        SamplingStatisticalSummary summary,
        List<SamplingTreeObservation> observations
) {

    /**
     * Compact constructor creating a defensive copy of the observations.
     *
     * @param summary      statistical sampling summary
     * @param observations ordered tree observations
     */
    public SamplingDetailedResult {
        if (summary == null) {
            throw new IllegalArgumentException("thinning.sampling_summary.null");
        }
        observations = observations == null ? List.of() : List.copyOf(observations);
    }
}
