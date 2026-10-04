package com.arcadiadevs.viora.platform.telemetry.domain.model.entities;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.MitigationStepId;

import java.time.Instant;

/**
 * Pure domain entity representing an actionable mitigation task within an Agroclimatic Incident.
 *
 * <p>Encapsulated without persistence annotations or public setters.</p>
 */
public class MitigationStep {

    private final MitigationStepId id;
    private final String instructionKey;
    private boolean completed;
    private Instant completedAt;

    private MitigationStep(MitigationStepId id, String instructionKey, boolean completed, Instant completedAt) {
        this.id = id;
        this.instructionKey = instructionKey;
        this.completed = completed;
        this.completedAt = completedAt;
    }

    /**
     * Factory method creating a new pending mitigation step.
     *
     * @param instructionKey the i18n translation key describing the mitigation instruction
     * @return a new valid {@link MitigationStep}
     */
    public static MitigationStep create(String instructionKey) {
        if (instructionKey == null || instructionKey.isBlank()) {
            throw new IllegalArgumentException("mitigation_step.instruction_key.blank");
        }
        return new MitigationStep(new MitigationStepId(), instructionKey.trim(), false, null);
    }

    /**
     * Reconstitutes an existing mitigation step from an immutable snapshot.
     *
     * @param snapshot the snapshot source
     * @return reconstituted {@link MitigationStep}
     */
    public static MitigationStep reconstitute(MitigationStepSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("mitigation_step.snapshot.null");
        }
        return new MitigationStep(snapshot.id(), snapshot.instructionKey(), snapshot.completed(), snapshot.completedAt());
    }

    /**
     * Marks this mitigation step as completed.
     *
     * @param timestamp completion instant
     */
    public void complete(Instant timestamp) {
        this.completed = true;
        this.completedAt = (timestamp != null) ? timestamp : Instant.now();
    }

    /**
     * Marks this mitigation step as incomplete/pending.
     */
    public void undo() {
        this.completed = false;
        this.completedAt = null;
    }

    /**
     * Extracts an immutable snapshot of this step's state.
     *
     * @return the {@link MitigationStepSnapshot}
     */
    public MitigationStepSnapshot snapshot() {
        return new MitigationStepSnapshot(id, instructionKey, completed, completedAt);
    }
}
