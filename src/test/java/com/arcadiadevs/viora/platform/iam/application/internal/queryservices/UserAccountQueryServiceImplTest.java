package com.arcadiadevs.viora.platform.iam.application.internal.queryservices;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.SecureTokenGenerator;
import com.arcadiadevs.viora.platform.iam.domain.model.UserAccountFixtures;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.CheckPasswordResetTokenQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@DisplayName("UserAccountQueryService password reset link check Unit Tests")
class UserAccountQueryServiceImplTest {

    private final UserAccountRepository repository = Mockito.mock(UserAccountRepository.class);
    private final SecureTokenGenerator tokens = new SecureTokenGenerator() {
        @Override
        public String generate() {
            return "link-token";
        }

        @Override
        public TokenHash hash(String token) {
            return UserAccountFixtures.tokenHashOf(token);
        }
    };
    private final UserAccountQueryServiceImpl queryService = new UserAccountQueryServiceImpl(repository, tokens);

    @Test
    @DisplayName("Accepts the active link without consuming it, and rejects unknown or expired ones")
    void checksResetLink() {
        var account = UserAccountFixtures.activeAccount("productor@viora.ag", "pw:Password123", UserRole.ROLE_PRODUCER);
        account.requestPasswordReset(UserAccountFixtures.tokenHashOf("link-token"), Instant.now());
        when(repository.findByResetTokenHash(UserAccountFixtures.tokenHashOf("link-token"))).thenReturn(Optional.of(account));
        when(repository.findByResetTokenHash(UserAccountFixtures.tokenHashOf("unknown"))).thenReturn(Optional.empty());

        assertThat(queryService.handle(new CheckPasswordResetTokenQuery("link-token"))).isTrue();
        assertThat(queryService.handle(new CheckPasswordResetTokenQuery("link-token"))).isTrue();
        assertThat(queryService.handle(new CheckPasswordResetTokenQuery("unknown"))).isFalse();
        assertThat(queryService.handle(new CheckPasswordResetTokenQuery(" "))).isFalse();

        var expired = UserAccountFixtures.activeAccount("otro@viora.ag", "pw:Password123", UserRole.ROLE_PRODUCER);
        expired.requestPasswordReset(UserAccountFixtures.tokenHashOf("old-token"), Instant.now().minusSeconds(31 * 60));
        when(repository.findByResetTokenHash(UserAccountFixtures.tokenHashOf("old-token"))).thenReturn(Optional.of(expired));
        assertThat(queryService.handle(new CheckPasswordResetTokenQuery("old-token"))).isFalse();
    }
}
