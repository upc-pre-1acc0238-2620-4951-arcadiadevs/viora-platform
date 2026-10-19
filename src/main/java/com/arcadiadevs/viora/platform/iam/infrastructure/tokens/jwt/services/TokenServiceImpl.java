package com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt.services;

import com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Token service implementation for JWT tokens.
 *
 * <p>Handles signing, issuance, claim extraction, and token verification
 * without coupling callers to JJWT infrastructure.</p>
 */
@Service
public class TokenServiceImpl implements BearerTokenService {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_TOKEN_PREFIX = "Bearer ";
    private static final int BEARER_PREFIX_LENGTH = 7;
    private static final String CREDENTIALS_VERSION_CLAIM = "cv";
    /** HMAC-SHA256 needs a key of at least 256 bits. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey signingKey;
    private final long accessTokenValidityMinutes;

    /**
     * Creates the token service.
     *
     * <p>The secret has no default on purpose: without {@code JWT_SECRET} the platform does not start, instead of
     * signing tokens with a key anyone can read in the repository.</p>
     *
     * @param secret                     the HMAC signing secret, at least 32 bytes long
     * @param accessTokenValidityMinutes how long an access token is valid
     * @throws IllegalStateException if the secret is missing or shorter than 32 bytes
     */
    public TokenServiceImpl(
            @Value("${authorization.jwt.secret}") String secret,
            @Value("${viora.security.jwt.access-token-validity-minutes:15}") long accessTokenValidityMinutes
    ) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET must be set to a random value of at least 32 bytes (e.g. openssl rand -base64 48)");
        }
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenValidityMinutes = accessTokenValidityMinutes;
    }

    @Override
    public String generateAccessToken(String userId, String email, String role, long credentialsVersion) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenValidityMinutes, ChronoUnit.MINUTES);

        var builder = Jwts.builder()
                .subject(userId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .claim(CREDENTIALS_VERSION_CLAIM, credentialsVersion);

        if (email != null) {
            builder.claim("email", email);
        }
        if (role != null) {
            builder.claim("role", role);
            builder.claim("roles", List.of(role));
            builder.claim("authorities", List.of(role));
        }

        return builder.signWith(signingKey).compact();
    }

    @Override
    public long getAccessTokenValiditySeconds() {
        return accessTokenValidityMinutes * 60;
    }

    @Override
    public String getUsernameFromToken(String token) {
        return parseClaims(token).map(Claims::getSubject).orElse(null);
    }

    @Override
    public boolean validateToken(String token) {
        return parseClaims(token).isPresent();
    }

    @Override
    public Optional<Long> extractCredentialsVersion(String token) {
        return parseClaims(token).map(claims -> {
            Object version = claims.get(CREDENTIALS_VERSION_CLAIM);
            return version instanceof Number number ? number.longValue() : null;
        });
    }

    @Override
    public String getBearerTokenFrom(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_TOKEN_PREFIX)) {
            return header.substring(BEARER_PREFIX_LENGTH).trim();
        }
        return null;
    }

    private Optional<Claims> parseClaims(String token) {
        if (!StringUtils.hasText(token)) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token.trim())
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
