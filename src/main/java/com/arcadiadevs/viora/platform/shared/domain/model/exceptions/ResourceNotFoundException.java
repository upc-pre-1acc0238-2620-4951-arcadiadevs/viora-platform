package com.arcadiadevs.viora.platform.shared.domain.model.exceptions;

/**
 * Exception thrown when a requested domain resource or aggregate is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a ResourceNotFoundException with the specified detail message.
     *
     * @param message the detail message
     */
    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a ResourceNotFoundException for a given resource name and identifier.
     *
     * @param resourceName the name of the resource type
     * @param identifier   the identifier that was not found
     */
    public ResourceNotFoundException(String resourceName, Object identifier) {
        super("Resource %s with id %s was not found.".formatted(resourceName, identifier));
    }
}
