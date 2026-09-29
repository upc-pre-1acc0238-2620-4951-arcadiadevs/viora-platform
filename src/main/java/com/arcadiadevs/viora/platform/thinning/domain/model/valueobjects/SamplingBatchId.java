package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Value Object representing the domain identifier of a field sampling batch.
 *
 * <p>Captures the provenance and external session identity of a batch collected by a field
 * technician or olive producer, providing business-level idempotency and auditability.</p>
 *
 * @param batchId the string batch identifier
 */
public record SamplingBatchId(String batchId) {

    /**
     * Compact constructor validating that batchId is non-null and not blank.
     */
    public SamplingBatchId {
        if (batchId == null || batchId.trim().isEmpty()) {
            throw new IllegalArgumentException("thinning.client_batch.id.null_or_empty");
        }
        batchId = batchId.trim();
    }
}
