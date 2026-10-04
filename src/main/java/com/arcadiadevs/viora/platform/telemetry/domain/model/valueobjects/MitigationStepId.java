package com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object representing the unique identifier of a mitigation step entity.
 *
 * @param stepId the UUID string identifier
 */
public record MitigationStepId(String stepId) {

    /**
     * Default constructor generating a new random UUID.
     */
    public MitigationStepId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Compact constructor validating that the stepId is a non-blank, valid UUID.
     *
     * @throws IllegalArgumentException if the stepId is null, blank, or malformed
     */
    public MitigationStepId {
        if (stepId == null || stepId.isBlank()) {
            throw new IllegalArgumentException("mitigation_step.id.null_or_empty");
        }
        try {
            stepId = UUID.fromString(stepId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("mitigation_step.id.invalid_uuid", exception);
        }
    }
}
