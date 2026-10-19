package com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.TokenService;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Optional;

/**
 * Interface extending {@link TokenService} with the HTTP Bearer operations the authorization filter needs.
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
     * Extracts the credentials version the token was issued with.
     *
     * @param token token value
     * @return the version, or empty when the token carries none or is invalid
     */
    Optional<Long> extractCredentialsVersion(String token);
}
