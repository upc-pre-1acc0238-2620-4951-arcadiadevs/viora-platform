package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object identifying a manager activation code of a cooperative license.
 *
 * @param managerCodeId the identifier; it must be a valid UUID string
 */
public record ManagerCodeId(String managerCodeId) {

    /**
     * Generates a new random identifier.
     */
    public ManagerCodeId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public ManagerCodeId {
        if (managerCodeId == null || managerCodeId.isBlank()) {
            throw new IllegalArgumentException("subscription.manager_code.id.null_or_empty");
        }
        try {
            managerCodeId = UUID.fromString(managerCodeId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("subscription.manager_code.id.invalid_uuid", exception);
        }
    }
}
