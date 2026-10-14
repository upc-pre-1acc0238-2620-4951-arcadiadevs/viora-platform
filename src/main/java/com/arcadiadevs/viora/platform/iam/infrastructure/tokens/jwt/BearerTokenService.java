package com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.TokenService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import java.util.Optional;

/**
 * Interface extending {@link TokenService} for HTTP Bearer token operations.
 */
public interface BearerTokenService extends TokenService {

    /**
     * Extracts the raw bearer token from the Authorization header of an HTTP request.
     *
     * @param request HTTP servlet request
     * @return bearer token string or null if not found
     */
    String getBearerTokenFrom(HttpServletRequest request);

    /**
     * Generates a signed token from an authenticated Spring Security principal.
     *
     * @param authentication security authentication instance
     * @return signed token string
     */
    String generateToken(Authentication authentication);

    /**
     * Generates a signed access token containing user identity and role claims.
     *
     * @param userId unique user identifier
     * @param email user email address
     * @param role assigned platform role
     * @return signed token string
     */
    default String generateAccessToken(String userId, String email, String role) {
        return generateToken(userId, email, role);
    }

    /**
     * Returns the validity duration of access tokens in seconds.
     *
     * @return validity in seconds
     */
    long getAccessTokenValiditySeconds();

    /**
     * Parses token claims.
     *
     * @param token bearer token string
     * @return Optional containing Claims if valid
     */
    Optional<Claims> parseClaims(String token);

    /**
     * Extracts subject userId from token claims.
     *
     * @param token bearer token string
     * @return Optional containing userId
     */
    Optional<String> extractUserId(String token);

    /**
     * Extracts role from token claims.
     *
     * @param token bearer token string
     * @return Optional containing role
     */
    Optional<String> extractRole(String token);

    /**
     * Extracts email from token claims.
     *
     * @param token bearer token string
     * @return Optional containing email
     */
    Optional<String> extractEmail(String token);

    /**
     * Extracts list of roles from token claims.
     *
     * @param token bearer token string
     * @return list of role strings
     */
    java.util.List<String> extractRoles(String token);
}
