package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object identifying a quota extension of a subscription.
 *
 * @param quotaExtensionId the identifier; it must be a valid UUID string
 */
public record QuotaExtensionId(String quotaExtensionId) {

    /**
     * Generates a new random identifier.
     */
    public QuotaExtensionId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public QuotaExtensionId {
        if (quotaExtensionId == null || quotaExtensionId.isBlank()) {
            throw new IllegalArgumentException("subscription.extension.id.null_or_empty");
        }
        try {
            quotaExtensionId = UUID.fromString(quotaExtensionId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("subscription.extension.id.invalid_uuid", exception);
        }
    }
}
