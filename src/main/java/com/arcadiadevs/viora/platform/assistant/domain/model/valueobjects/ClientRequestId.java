package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.UUID;

/**
 * Idempotency key the app generates for each ask: the same key always gets the same answer.
 *
 * @param value the UUID, lower case
 */
public record ClientRequestId(String value) {

    /**
     * Compact constructor validating that the value is a UUID.
     */
    public ClientRequestId {
        try {
            value = UUID.fromString(value == null ? "" : value.trim()).toString().toLowerCase();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("assistant.client_request_id.invalid", e);
        }
    }
}
