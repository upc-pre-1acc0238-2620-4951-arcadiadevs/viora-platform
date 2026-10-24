package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object identifying an extension code of a cooperative license.
 *
 * @param extensionCodeId the identifier; it must be a valid UUID string
 */
public record ExtensionCodeId(String extensionCodeId) {

    /**
     * Generates a new random identifier.
     */
    public ExtensionCodeId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public ExtensionCodeId {
        if (extensionCodeId == null || extensionCodeId.isBlank()) {
            throw new IllegalArgumentException("subscription.extension_code.id.null_or_empty");
        }
        try {
            extensionCodeId = UUID.fromString(extensionCodeId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("subscription.extension_code.id.invalid_uuid", exception);
        }
    }
}
