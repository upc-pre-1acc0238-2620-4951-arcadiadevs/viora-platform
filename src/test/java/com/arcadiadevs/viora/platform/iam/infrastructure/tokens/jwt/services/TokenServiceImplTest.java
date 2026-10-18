package com.arcadiadevs.viora.platform.iam.infrastructure.tokens.jwt.services;

import com.arcadiadevs.viora.platform.iam.domain.model.UserAccountFixtures;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenServiceImpl JWT Generation & Claims Unit Tests")
class TokenServiceImplTest {

    private static final String SECRET = "viora-secret-super-secure-key-for-jwt-token-generation-2026-very-long-string";
    private static final long EXPIRATION_MINUTES = 15;

    @Mock
    private UserAccountRepository userAccountRepository;

    private TokenServiceImpl tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenServiceImpl(SECRET, EXPIRATION_MINUTES, userAccountRepository);
    }

    @Test
    @DisplayName("generateToken with userId, email, and role should include role in claims")
    void shouldGenerateTokenWithFullClaimsIncludingRole() {
        String token = tokenService.generateToken("user-123", "productor@viora.ag", "ROLE_PRODUCER");

        assertThat(token).isNotBlank();
        assertThat(tokenService.validateToken(token)).isTrue();
        assertThat(tokenService.extractUserId(token)).contains("user-123");
        assertThat(tokenService.extractEmail(token)).contains("productor@viora.ag");
        assertThat(tokenService.extractRole(token)).contains("ROLE_PRODUCER");
        assertThat(tokenService.extractRoles(token)).containsExactly("ROLE_PRODUCER");
    }

    @Test
    @DisplayName("generateToken with username and role should include role in claims")
    void shouldGenerateTokenWithUsernameAndRole() {
        String token = tokenService.generateToken("user-456", "ROLE_AGRONOMIST");

        assertThat(token).isNotBlank();
        assertThat(tokenService.validateToken(token)).isTrue();
        assertThat(tokenService.extractUserId(token)).contains("user-456");
        assertThat(tokenService.extractRole(token)).contains("ROLE_AGRONOMIST");
    }

    @Test
    @DisplayName("generateToken with Authentication object should extract authority and include role")
    void shouldGenerateTokenFromAuthenticationIncludingRole() {
        var authority = new SimpleGrantedAuthority("ROLE_ADMIN");
        var authentication = new UsernamePasswordAuthenticationToken("admin-user", null, List.of(authority));

        String token = tokenService.generateToken(authentication);

        assertThat(token).isNotBlank();
        assertThat(tokenService.validateToken(token)).isTrue();
        assertThat(tokenService.extractUserId(token)).contains("admin-user");
        assertThat(tokenService.extractRole(token)).contains("ROLE_ADMIN");
    }

    @Test
    @DisplayName("generateToken with username only should lookup repository and include role")
    void shouldGenerateTokenLookingUpUserAccountRole() {
        var account = UserAccountFixtures.activeAccount("productor@viora.ag", "$argon2id$hash", UserRole.ROLE_PRODUCER);
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(account));

        String token = tokenService.generateToken("productor@viora.ag");

        assertThat(token).isNotBlank();
        assertThat(tokenService.validateToken(token)).isTrue();
        assertThat(tokenService.extractRole(token)).contains("ROLE_PRODUCER");
        assertThat(tokenService.extractEmail(token)).contains("productor@viora.ag");
    }
}
