package com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens;

/**
 * Outbound port for bearer token issuance and validation used by IAM commands and queries.
 */
public interface TokenService {

    /**
     * Generates a token for a username.
     *
     * @param username principal identifier
     * @return signed token value
     */
    String generateToken(String username);

    /**
     * Generates a signed access token for a username and role.
     *
     * @param username principal identifier
     * @param role assigned platform role
     * @return signed token value
     */
    String generateToken(String username, String role);

    /**
     * Generates a signed access token containing user identity and role claims.
     *
     * @param userId unique user identifier
     * @param email user email address
     * @param role assigned platform role
     * @return signed token value
     */
    String generateToken(String userId, String email, String role);

    /**
     * Extracts the username from a token.
     *
     * @param token token value
     * @return username embedded in the token
     */
    String getUsernameFromToken(String token);

    /**
     * Validates a token.
     *
     * @param token token value
     * @return true when token is valid; otherwise false
     */
    boolean validateToken(String token);
}
