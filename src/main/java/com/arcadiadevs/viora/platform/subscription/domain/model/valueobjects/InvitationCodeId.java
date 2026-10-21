package com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value object identifying a cooperative invitation code.
 * <p>The identifier is stored as a UUID string. It throws an IllegalArgumentException if it is null, blank, or not a
 * valid UUID.</p>
 *
 * @param invitationCodeId the identifier; it must be a valid UUID string
 */
public record InvitationCodeId(String invitationCodeId) {

    /**
     * Generates a new random identifier.
     */
    public InvitationCodeId() {
        this(UUID.randomUUID().toString());
    }

    /**
     * Validates and normalizes the identifier to a lowercase UUID string.
     *
     * @throws IllegalArgumentException if the identifier is null, blank, or not a valid UUID
     */
    public InvitationCodeId {
        if (invitationCodeId == null || invitationCodeId.isBlank()) {
            throw new IllegalArgumentException("subscription.invitation_code.id.null_or_empty");
        }
        try {
            invitationCodeId = UUID.fromString(invitationCodeId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("subscription.invitation_code.id.invalid_uuid", exception);
        }
    }
}
