package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

import java.util.UUID;

/**
 * Value Object representing the domain identifier of a field sampling round entity.
 *
 * @param roundId the string UUID value
 */
public record RoundId(String roundId) {

    /**
     * Compact constructor validating that roundId is a non-null valid UUID.
     */
    public RoundId {
        if (roundId == null || roundId.trim().isEmpty()) {
            throw new IllegalArgumentException("thinning.round.id.null_or_empty");
        }
        try {
            roundId = UUID.fromString(roundId.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("thinning.round.id.invalid_uuid", e);
        }
    }

    /**
     * Constructs a new randomly generated RoundId.
     */
    public RoundId() {
        this(UUID.randomUUID().toString());
    }
}
