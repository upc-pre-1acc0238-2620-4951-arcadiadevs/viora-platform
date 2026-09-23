package com.arcadiadevs.viora.platform.shared.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.jspecify.annotations.Nullable;

/**
 * Standard error response resource returned from REST endpoints.
 *
 * @param code    the machine-readable error code
 * @param message the human-readable error message
 * @param details additional diagnostic or context details
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResource(
        String code,
        String message,
        @Nullable String details) {

    /**
     * Creates an ErrorResource from code and message.
     *
     * @param code    the machine-readable error code
     * @param message the human-readable error message
     */
    public ErrorResource(String code, String message) {
        this(code, message, null);
    }
}

