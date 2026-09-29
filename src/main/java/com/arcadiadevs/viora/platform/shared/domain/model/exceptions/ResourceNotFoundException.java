package com.arcadiadevs.viora.platform.shared.domain.model.exceptions;

import org.jspecify.annotations.Nullable;

/**
 * Exception thrown when a requested domain resource or aggregate is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final @Nullable Object identifier;

    /**
     * Constructs a ResourceNotFoundException with the specified detail message or i18n key.
     *
     * @param message the detail message or i18n key
     */
    public ResourceNotFoundException(String message) {
        super(message);
        this.resourceName = "Resource";
        this.identifier = null;
    }

    /**
     * Constructs a ResourceNotFoundException for a given resource name and identifier.
     *
     * @param resourceName the name of the resource type
     * @param identifier   the identifier that was not found
     */
    public ResourceNotFoundException(String resourceName, @Nullable Object identifier) {
        super("error.resource.not-found");
        this.resourceName = resourceName != null ? resourceName : "Resource";
        this.identifier = identifier;
    }

    /**
     * Gets the name of the resource type that was not found.
     *
     * @return the resource name
     */
    public String getResourceName() {
        return resourceName;
    }

    /**
     * Gets the identifier of the resource that was not found.
     *
     * @return the resource identifier, or null if not specified
     */
    public @Nullable Object getIdentifier() {
        return identifier;
    }
}
