package com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt.services;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt.BearerTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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

    private final SecretKey signingKey;
    private final long accessTokenValidityMinutes;
    private final UserAccountRepository userAccountRepository;

    public TokenServiceImpl(
            @Value("${authorization.jwt.secret:${viora.security.jwt.secret:viora-secret-super-secure-key-for-jwt-token-generation-2026-very-long-string}}") String secret,
            @Value("${viora.security.jwt.access-token-validity-minutes:15}") long accessTokenValidityMinutes,
            UserAccountRepository userAccountRepository
    ) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenValidityMinutes = accessTokenValidityMinutes;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    public String generateToken(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalArgumentException("authentication.null");
        }
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse(null);
        return buildToken(authentication.getName(), null, role);
    }

    @Override
    public String generateToken(String username) {
        String role = null;
        String email = null;
        if (userAccountRepository != null && StringUtils.hasText(username)) {
            var accountOpt = username.contains("@")
                    ? userAccountRepository.findByEmail(new EmailAddress(username))
                    : userAccountRepository.findById(new UserId(username));
            if (accountOpt.isPresent()) {
                var snap = accountOpt.get().snapshot();
                role = snap.role().name();
                email = snap.email().value();
            }
        }
        return buildToken(username, email, role);
    }

    @Override
    public String generateToken(String username, String role) {
        return buildToken(username, null, role);
    }

    @Override
    public String generateToken(String userId, String email, String role) {
        return buildToken(userId, email, role);
    }

    private String buildToken(String subject, String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessTokenValidityMinutes, ChronoUnit.MINUTES);

        var builder = Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry));

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
    public String getUsernameFromToken(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    @Override
    public boolean validateToken(String token) {
        return parseClaims(token).isPresent();
    }

    @Override
    public Optional<Claims> parseClaims(String token) {
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

    @Override
    public Optional<String> extractUserId(String token) {
        return parseClaims(token).map(Claims::getSubject);
    }

    @Override
    public Optional<String> extractRole(String token) {
        return parseClaims(token).map(claims -> {
            String role = claims.get("role", String.class);
            if (role != null) {
                return role;
            }
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List<?> list && !list.isEmpty()) {
                return list.get(0).toString();
            }
            return null;
        });
    }

    @Override
    public List<String> extractRoles(String token) {
        return parseClaims(token).map(claims -> {
            Object rolesObj = claims.get("roles");
            if (rolesObj instanceof List<?> list) {
                return list.stream().map(Object::toString).toList();
            }
            String role = claims.get("role", String.class);
            return role != null ? List.of(role) : List.<String>of();
        }).orElse(List.of());
    }

    @Override
    public Optional<String> extractEmail(String token) {
        return parseClaims(token).map(claims -> claims.get("email", String.class));
    }

    @Override
    public long getAccessTokenValiditySeconds() {
        return accessTokenValidityMinutes * 60;
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

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return parseClaims(token).map(claimsResolver).orElse(null);
    }
}
