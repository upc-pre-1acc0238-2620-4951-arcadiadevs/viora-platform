package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.UUID;

/**
 * Identifier of a saved answer.
 *
 * @param value the UUID, lower case
 */
public record AnswerId(String value) {

    /**
     * Compact constructor validating that the value is a UUID.
     */
    public AnswerId {
        try {
            value = UUID.fromString(value == null ? "" : value.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("assistant.answer.id.invalid", e);
        }
    }

    /**
     * Creates a new random identifier.
     *
     * @return the identifier
     */
    public static AnswerId generate() {
        return new AnswerId(UUID.randomUUID().toString());
    }
}
