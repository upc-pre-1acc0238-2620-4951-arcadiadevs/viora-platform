package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.util.UUID;

/**
 * Logical reference to a technical manager, kept by the IAM context.
 *
 * @param managerId the identifier as a lowercase UUID string
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
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
        try {
            managerId = UUID.fromString(managerId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("territory.manager.id.invalid_uuid", exception);
        }
    }
}
