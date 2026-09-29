package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value Object representing the unique identity of an execution confirmation entity.
 *
 * @param confirmationId the string UUID value
 */
public record ConfirmationId(String confirmationId) {

    /**
     * Compact constructor validating that confirmationId is a non-null valid UUID.
     */
    public ConfirmationId {
        if (confirmationId == null || confirmationId.trim().isEmpty()) {
            throw new IllegalArgumentException("thinning.confirmation.id.null_or_empty");
        }
        try {
            confirmationId = UUID.fromString(confirmationId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("thinning.confirmation.id.invalid_uuid", e);
        }
    }

    /**
     * Constructs a new randomly generated ConfirmationId.
     */
    public ConfirmationId() {
        this(UUID.randomUUID().toString());
    }
}
