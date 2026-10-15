package com.arcadiadevs.viora.platform.iam.interfaces.rest.resources;

/**
 * Public response DTO returned upon successful sign-in or auto-login.
 *
 * @param accessToken signed JWT bearer token
 * @param refreshToken opaque refresh token
 * @param tokenType type token format ("Bearer")
 * @param expiresIn validity period in seconds
 * @param userId authenticated user identifier
 * @param email authenticated user email
 * @param role authenticated user role
 */
public record AuthenticatedSessionResource(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        String userId,
        String email,
        String role
) {
}
