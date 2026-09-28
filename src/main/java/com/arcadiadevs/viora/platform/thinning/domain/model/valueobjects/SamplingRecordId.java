package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value Object representing the domain identifier of an individual tree sampling record entity.
 *
 * @param recordId the string UUID value
 */
public record SamplingRecordId(String recordId) {

    /**
     * Compact constructor validating that recordId is a non-null valid UUID.
     */
    public SamplingRecordId {
        if (recordId == null || recordId.trim().isEmpty()) {
            throw new IllegalArgumentException("thinning.record.id.null_or_empty");
        }
        try {
            recordId = UUID.fromString(recordId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("thinning.record.id.invalid_uuid", e);
        }
    }

    /**
     * Constructs a new randomly generated SamplingRecordId.
     */
    public SamplingRecordId() {
        this(UUID.randomUUID().toString());
    }
}
