package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

import java.util.UUID;

/**
 * Logical reference to the cooperative license, kept by the Subscription context, whose sector list this is.
 *
 * @param licenseId the identifier as a lowercase UUID string
 */
public record LicenseId(String licenseId) {

    /**
     * Generates a new random identifier.
     */
    public LicenseId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public LicenseId {
        if (licenseId == null || licenseId.isBlank()) {
            throw new IllegalArgumentException("territory.license.id.null_or_empty");
        }
        try {
            licenseId = UUID.fromString(licenseId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("territory.license.id.invalid_uuid", exception);
        }
    }
}
