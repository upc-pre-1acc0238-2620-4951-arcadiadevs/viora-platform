package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

/**
 * Enumeration of system roles recognized within the Viora platform.
 */
public enum UserRole {
    ROLE_PRODUCER,
    ROLE_TECHNICAL_MANAGER;

    /**
     * Parses a role name securely.
     *
     * @param value raw role name
     * @return matching UserRole enum constant
     * @throws IllegalArgumentException if the role name is null, blank, or unsupported
     */
    public static UserRole from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("user.role.null_or_empty");
        }
        try {
            return UserRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("user.role.unsupported", ex);
        }
    }
}
