package com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt.services;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TokenServiceImpl JWT Generation & Claims Unit Tests")
class TokenServiceImplTest {

    private static final String SECRET = "viora-secret-super-secure-key-for-jwt-token-generation-2026-very-long-string";
    private static final long EXPIRATION_MINUTES = 15;

    private final TokenServiceImpl tokenService = new TokenServiceImpl(SECRET, EXPIRATION_MINUTES);

    @Test
    @DisplayName("generateAccessToken should sign the identity, role and credentials version claims")
    void shouldGenerateAccessTokenWithClaims() {
        String token = tokenService.generateAccessToken("user-123", "productor@viora.ag", "ROLE_PRODUCER", 3L);

        assertThat(tokenService.validateToken(token)).isTrue();
        assertThat(tokenService.getUsernameFromToken(token)).isEqualTo("user-123");
        assertThat(tokenService.extractCredentialsVersion(token)).contains(3L);

        var claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();
        assertThat(claims.get("email", String.class)).isEqualTo("productor@viora.ag");
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_PRODUCER");
        assertThat(claims.get("roles", List.class)).containsExactly("ROLE_PRODUCER");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(EXPIRATION_MINUTES * 60 * 1000);
    }

    @Test
    @DisplayName("Should report the access token validity in seconds")
    void shouldReportValidity() {
        assertThat(tokenService.getAccessTokenValiditySeconds()).isEqualTo(EXPIRATION_MINUTES * 60);
    }

    @Test
    @DisplayName("Should reject tokens signed with another key or malformed")
    void shouldRejectForeignOrMalformedTokens() {
        var otherService = new TokenServiceImpl("another-secret-that-is-long-enough-for-hmac-sha-256-keys", 15);
        String foreign = otherService.generateAccessToken("user-123", "productor@viora.ag", "ROLE_PRODUCER", 0L);

        assertThat(tokenService.validateToken(foreign)).isFalse();
        assertThat(tokenService.validateToken("not-a-jwt")).isFalse();
        assertThat(tokenService.getUsernameFromToken("not-a-jwt")).isNull();
        assertThat(tokenService.extractCredentialsVersion("not-a-jwt")).isEmpty();
    }

    @Test
    @DisplayName("Should read the bearer token from the Authorization header")
    void shouldReadBearerTokenFromHeader() {
        var request = new MockHttpServletRequest();
        assertThat(tokenService.getBearerTokenFrom(request)).isNull();

        request.addHeader("Authorization", "Bearer abc.def.ghi");
        assertThat(tokenService.getBearerTokenFrom(request)).isEqualTo("abc.def.ghi");
    }
}
