package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object identifying the consent a producer gave to share their data with the license that sponsors their plan.
 *
 * @param dataSharingConsentId the identifier; it must be a valid UUID string
 */
public record DataSharingConsentId(String dataSharingConsentId) {

    /**
     * Generates a new random identifier.
     */
    public DataSharingConsentId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public DataSharingConsentId {
        if (dataSharingConsentId == null || dataSharingConsentId.isBlank()) {
            throw new IllegalArgumentException("subscription.consent.id.null_or_empty");
        }
        try {
            dataSharingConsentId = UUID.fromString(dataSharingConsentId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("subscription.consent.id.invalid_uuid", exception);
        }
    }
}
