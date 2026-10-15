package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

import java.time.Instant;

/**
 * Public response DTO exposing user account attributes.
 *
 * @param id user account identifier string
 * @param email registered email address
 * @param role assigned platform role
 * @param createdAt registration instant
 */
public record UserAccountResource(
        String id,
        String email,
        String role,
        Instant createdAt
) {
}
