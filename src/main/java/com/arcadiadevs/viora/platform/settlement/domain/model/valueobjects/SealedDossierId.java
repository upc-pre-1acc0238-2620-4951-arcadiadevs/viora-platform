package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identity of an agronomic dossier sealed by the producer.
 *
 * @param value lowercase UUID
 */
public record SealedDossierId(String value) {

    public SealedDossierId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("settlement.dossier.id.invalid");
        }
        try {
            value = UUID.fromString(value.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("settlement.dossier.id.invalid", exception);
        }
    }

    /**
     * Creates a new random identity.
     *
     * @return a new identity
     */
    public static SealedDossierId generate() {
        return new SealedDossierId(UUID.randomUUID().toString());
    }
}
