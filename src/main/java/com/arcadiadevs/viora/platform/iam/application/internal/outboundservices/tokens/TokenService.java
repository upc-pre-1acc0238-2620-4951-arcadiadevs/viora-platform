package com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens;

/**
 * Outbound port for issuing and validating the bearer access tokens of IAM sessions.
 */
public interface TokenService {

    /**
     * Generates a signed access token carrying the credentials version of the account, so the token stops working
     * once the password changes.
     *
     * @param userId             unique user identifier
     * @param email              user email address
     * @param role               assigned platform role
     * @param credentialsVersion credentials version of the account
     * @return signed token value
     */
    String generateAccessToken(String userId, String email, String role, long credentialsVersion);

    /**
     * Returns how long an access token is valid.
     *
     * @return validity in seconds
     */
    long getAccessTokenValiditySeconds();

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
