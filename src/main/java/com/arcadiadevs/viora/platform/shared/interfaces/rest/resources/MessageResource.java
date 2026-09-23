package com.arcadiadevs.viora.platform.shared.interfaces.rest.resources;

/**
 * Resource used for simple success or informational REST responses.
 *
 * @param message the informational or status message
 */
public record MessageResource(String message) {
}
