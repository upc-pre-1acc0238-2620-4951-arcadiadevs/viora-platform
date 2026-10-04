package com.arcadiadevs.viora.platform.telemetry.domain.model.entities;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.MitigationStepId;

import java.time.Instant;

/**
 * Immutable snapshot representing the state of a {@link MitigationStep} entity.
 *
 * @param id             the step unique identity
 * @param instructionKey the i18n translation key for the step instruction
 * @param completed      whether the mitigation step has been performed
 * @param completedAt    the timestamp when the step was completed, or null
 */
public record MitigationStepSnapshot(
        MitigationStepId id,
        String instructionKey,
        boolean completed,
        Instant completedAt
) {
}
