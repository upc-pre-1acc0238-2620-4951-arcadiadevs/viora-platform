package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object referencing the technical manager in charge of a cooperative license: the IAM user who redeemed its
 * manager activation code.
 *
 * @param managerId the identifier; it must be a valid UUID string
 */
public record ManagerId(String managerId) {

    /**
     * Generates a new random identifier.
     */
    public ManagerId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public ManagerId {
        if (managerId == null || managerId.isBlank()) {
            throw new IllegalArgumentException("subscription.manager.id.null_or_empty");
        }
        try {
            managerId = UUID.fromString(managerId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("subscription.manager.id.invalid_uuid", exception);
        }
    }
}
