package com.arcadiadevs.viora.platform.thinning.domain.model.commands;

import java.util.List;

/**
 * Domain command requesting ingestion of a field sampling batch.
 *
 * @param plotId        the plot identifier
 * @param actorId       the sampling user identifier
 * @param campaignYear  the agricultural campaign year
 * @param clientBatchId the client-side idempotency batch identifier
 * @param samples       the list of tree evaluations
 */
public record IngestFieldSamplingsBatchCommand(
        String plotId,
        String actorId,
        Integer campaignYear,
        String clientBatchId,
        List<TreeSampleItem> samples
) {

    /**
     * Compact constructor validating strictly non-null arguments.
     */
    public IngestFieldSamplingsBatchCommand {
        if (plotId == null) {
            throw new IllegalArgumentException("thinning.plot.id.null_or_empty");
        }
        if (actorId == null) {
            throw new IllegalArgumentException("thinning.actor.id.null_or_empty");
        }
        if (campaignYear == null) {
            throw new IllegalArgumentException("thinning.campaign_year.null");
        }
        if (clientBatchId == null) {
            throw new IllegalArgumentException("thinning.client_batch.id.null_or_empty");
        }
        if (samples == null || samples.isEmpty()) {
            throw new IllegalArgumentException("thinning.samples.empty");
        }
        samples = List.copyOf(samples);
    }
}
