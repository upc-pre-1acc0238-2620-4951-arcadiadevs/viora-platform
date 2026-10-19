package com.arcadiadevs.viora.platform.iam.application.internal.commandservices;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.application.commandservices.AuthenticatedSession;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.SecureTokenGenerator;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.arcadiadevs.viora.platform.iam.domain.model.FakePasswordHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.UserAccountFixtures;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserSession;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.AuthenticateUserCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.OpenUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.PurgeEndedUserSessionsCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RefreshUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.NewPassword;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserSessionRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSessionCommandService sign-in and refresh token rotation Application Unit Tests")
class UserSessionCommandServiceImplTest {

    private static final String EMAIL = "productor@viora.ag";

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private TokenService tokenService;

    private final InMemoryUserSessionRepository sessions = new InMemoryUserSessionRepository();
    private UserSessionCommandServiceImpl service;
    private UserAccount account;

    @BeforeEach
    void setUp() {
        service = new UserSessionCommandServiceImpl(userAccountRepository, sessions, new FakePasswordHasher(),
                tokenService, new SequentialTokenGenerator());
        account = UserAccountFixtures.activeAccount(EMAIL, FakePasswordHasher.hashOf("Password123").value(),
                UserRole.ROLE_PRODUCER);
        lenient().when(tokenService.generateAccessToken(anyString(), anyString(), anyString(), anyLong()))
                .thenAnswer(invocation -> "access-v" + invocation.getArgument(3));
        lenient().when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(account));
        lenient().when(userAccountRepository.findById(any(UserId.class))).thenReturn(Optional.of(account));
    }

    @Test
    @DisplayName("Signs in with the right password and stores only the hash of the refresh token")
    void signInOpensSession() {
        var session = success(service.handle(new AuthenticateUserCommand(EMAIL, "Password123")));

        assertThat(session.accessToken()).isEqualTo("access-v0");
        assertThat(session.refreshToken()).isEqualTo("refresh-1");
        assertThat(sessions.byHash).containsOnlyKeys(UserAccountFixtures.tokenHashOf("refresh-1"));
    }

    @Test
    @DisplayName("Answers a wrong password and an unknown email the same way")
    void wrongCredentialsLookTheSame() {
        var wrongPassword = failure(service.handle(new AuthenticateUserCommand(EMAIL, "Otra12345")));
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.empty());
        var unknownEmail = failure(service.handle(new AuthenticateUserCommand("nadie@viora.ag", "Password123")));

        assertThat(wrongPassword).isEqualTo(unknownEmail);
        assertThat(wrongPassword.details()).isEqualTo("user.credentials.invalid");
        assertThat(sessions.byHash).isEmpty();
    }

    @Test
    @DisplayName("Does not open a session for an account whose email is not verified")
    void pendingAccountCannotSignIn() {
        account = UserAccountFixtures.pendingAccount(EMAIL, FakePasswordHasher.hashOf("Password123").value());
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(account));

        var error = failure(service.handle(new AuthenticateUserCommand(EMAIL, "Password123")));

        assertThat(error.code()).isEqualTo("EMAIL_VERIFICATION_FORBIDDEN");
    }

    @Test
    @DisplayName("Rotates the refresh token: the old one stops working and a new one is issued")
    void refreshRotatesToken() {
        success(service.handle(new OpenUserSessionCommand(account.snapshot().id().userId())));

        var renewed = success(service.handle(new RefreshUserSessionCommand("refresh-1")));

        assertThat(renewed.refreshToken()).isEqualTo("refresh-2");
        assertThat(sessions.byHash.get(UserAccountFixtures.tokenHashOf("refresh-1")).snapshot().revokedAt()).isNotNull();
        assertThat(sessions.byHash.get(UserAccountFixtures.tokenHashOf("refresh-2")).isActive(Instant.now())).isTrue();
    }

    @Test
    @DisplayName("Closes every session when a rotated refresh token is used again")
    void reusedRefreshTokenRevokesEverything() {
        success(service.handle(new OpenUserSessionCommand(account.snapshot().id().userId())));
        success(service.handle(new RefreshUserSessionCommand("refresh-1")));

        var error = failure(service.handle(new RefreshUserSessionCommand("refresh-1")));

        assertThat(error.details()).isEqualTo("user.refresh_token.invalid");
        assertThat(sessions.byHash.values()).noneMatch(session -> session.isActive(Instant.now()));
    }

    @Test
    @DisplayName("Rejects a refresh token issued before the password changed")
    void refreshAfterPasswordChangeIsRejected() {
        success(service.handle(new OpenUserSessionCommand(account.snapshot().id().userId())));
        account.changePassword("Password123", new NewPassword("NuevaClave456"), new FakePasswordHasher(), Instant.now());

        var error = failure(service.handle(new RefreshUserSessionCommand("refresh-1")));

        assertThat(error.details()).isEqualTo("user.refresh_token.invalid");
    }

    @Test
    @DisplayName("Rejects an unknown refresh token")
    void unknownRefreshTokenIsRejected() {
        assertThat(failure(service.handle(new RefreshUserSessionCommand("not-a-token"))).details())
                .isEqualTo("user.refresh_token.invalid");
    }

    @Test
    @DisplayName("Removes only the sessions that ended more than 30 days ago")
    void purgesSessionsEndedLongAgo() {
        var userId = account.snapshot().id();
        var longAgo = Instant.now().minus(UserSession.LIFESPAN).minus(UserSession.RETENTION_AFTER_END).minusSeconds(60);
        var expiredLongAgo = UserSession.start(userId, UserAccountFixtures.tokenHashOf("old"), 0L, longAgo);
        var revokedLongAgo = UserSession.start(userId, UserAccountFixtures.tokenHashOf("revoked"), 0L, Instant.now().minusSeconds(60));
        revokedLongAgo.revoke(Instant.now().minus(UserSession.RETENTION_AFTER_END).minusSeconds(60));
        var revokedRecently = UserSession.start(userId, UserAccountFixtures.tokenHashOf("recent"), 0L, Instant.now());
        revokedRecently.revoke(Instant.now());
        var active = UserSession.start(userId, UserAccountFixtures.tokenHashOf("active"), 0L, Instant.now());
        List.of(expiredLongAgo, revokedLongAgo, revokedRecently, active).forEach(sessions::save);

        var result = service.handle(new PurgeEndedUserSessionsCommand());

        assertThat(((Result.Success<Integer, ApplicationError>) result).value()).isEqualTo(2);
        assertThat(sessions.byHash).containsOnlyKeys(
                UserAccountFixtures.tokenHashOf("recent"), UserAccountFixtures.tokenHashOf("active"));
    }

    private static AuthenticatedSession success(Result<AuthenticatedSession, ApplicationError> result) {
        assertThat(result).isInstanceOf(Result.Success.class);
        return ((Result.Success<AuthenticatedSession, ApplicationError>) result).value();
    }

    private static ApplicationError failure(Result<AuthenticatedSession, ApplicationError> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<AuthenticatedSession, ApplicationError>) result).error();
    }

    /** Hands out refresh-1, refresh-2, ... so the tests can refer to each token. */
    private static final class SequentialTokenGenerator implements SecureTokenGenerator {
        private int next = 1;

        @Override
        public String generate() {
            return "refresh-" + next++;
        }

        @Override
        public TokenHash hash(String refreshToken) {
            return UserAccountFixtures.tokenHashOf(refreshToken);
        }
    }

    private static final class InMemoryUserSessionRepository implements UserSessionRepository {
        private final Map<TokenHash, UserSession> byHash = new HashMap<>();

        @Override
        public UserSession save(UserSession session) {
            byHash.put(session.snapshot().refreshTokenHash(), session);
            return session;
        }

        @Override
        public Optional<UserSession> findByRefreshTokenHash(TokenHash refreshTokenHash) {
            return Optional.ofNullable(byHash.get(refreshTokenHash));
        }

        @Override
        public List<UserSession> findNotRevokedByUserId(UserId userId) {
            var result = new ArrayList<UserSession>();
            for (var session : byHash.values()) {
                if (session.snapshot().userId().equals(userId) && session.snapshot().revokedAt() == null) {
                    result.add(session);
                }
            }
            return result;
        }

        @Override
        public int deleteEndedBefore(Instant cutoff) {
            var ended = byHash.entrySet().stream()
                    .filter(entry -> {
                        var snapshot = entry.getValue().snapshot();
                        return snapshot.expiresAt().isBefore(cutoff)
                                || (snapshot.revokedAt() != null && snapshot.revokedAt().isBefore(cutoff));
                    })
                    .map(Map.Entry::getKey)
                    .toList();
            ended.forEach(byHash::remove);
            return ended.size();
        }
    }
}
