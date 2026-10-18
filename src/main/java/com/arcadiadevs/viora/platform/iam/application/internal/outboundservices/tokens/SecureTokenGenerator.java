package com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens;

/**
 * Outbound application port producing unguessable opaque tokens and the hash stored for them.
 *
 * <p>Used for refresh tokens and password reset links. Only the hash is stored: the plain token is handed to the
 * client once and never kept.</p>
 */
public interface SecureTokenGenerator {

    /**
     * Generates a new random, unguessable token.
     *
     * @return the plain token for the client
     */
    String generate();

    /**
     * Hashes a token, to store it or to look up what it belongs to.
     *
     * @param token the plain token
     * @return the hash of the token
     */
    String hash(String token);
}
