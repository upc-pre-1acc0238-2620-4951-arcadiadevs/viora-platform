package com.arcadiadevs.viora.platform.shared.domain.model.exceptions;

/**
 * Domain exception thrown when an operation conflicts with existing state,
 * such as uniqueness invariants or state collisions.
 */
public class ResourceConflictException extends RuntimeException {

    /**
     * Constructs a ResourceConflictException with the specified detail message.
     *
     * @param message the detail message
     */
    public ResourceConflictException(String message) {
        super(message);
    }
}
