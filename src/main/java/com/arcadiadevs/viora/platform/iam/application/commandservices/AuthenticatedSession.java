package com.arcadiadevs.viora.platform.iam.application.commandservices;

/**
 * Result of a use case that opens a session: the tokens handed to the client and the account they belong to.
 *
 * @param accessToken           signed bearer token
 * @param refreshToken          opaque refresh token, shown to the client only once
 * @param accessTokenValidity   access token validity in seconds
 * @param userId                the account identifier
 * @param email                 the account email
 * @param role                  the account role
 */
public record AuthenticatedSession(
        String accessToken,
        String refreshToken,
        long accessTokenValidity,
        String userId,
        String email,
        String role
) {
}
