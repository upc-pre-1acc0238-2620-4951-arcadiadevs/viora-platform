package com.arcadiadevs.viora.platform.iam.application.internal.commandservices;

import com.arcadiadevs.viora.platform.iam.application.commandservices.AuthenticatedSession;
import com.arcadiadevs.viora.platform.iam.application.commandservices.UserSessionCommandService;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.SecureTokenGenerator;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.TokenService;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserSession;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.AuthenticateUserCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.OpenUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.PurgeEndedUserSessionsCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RefreshUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.PasswordHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserAccountStatus;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserSessionRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Application service implementation opening and renewing user sessions.
 *
 * <p>Every session stores only the hash of its refresh token. A refresh rotates the token; presenting a token that
 * was already rotated or revoked revokes every session of the account, because it means the token leaked.</p>
 */
@Service
@Transactional
public class UserSessionCommandServiceImpl implements UserSessionCommandService {

    private final UserAccountRepository userAccountRepository;
    private final UserSessionRepository userSessionRepository;
    private final PasswordHasher passwordHasher;
    private final TokenService tokenService;
    private final SecureTokenGenerator tokenGenerator;

    /**
     * Creates the session command service.
     *
     * @param userAccountRepository the user account repository port
     * @param userSessionRepository the user session repository port
     * @param passwordHasher        checks the password typed at sign-in
     * @param tokenService          issues the access tokens
     * @param tokenGenerator generates and hashes the refresh tokens
     * @throws IllegalArgumentException if any dependency is null
     */
    public UserSessionCommandServiceImpl(
            UserAccountRepository userAccountRepository,
            UserSessionRepository userSessionRepository,
            PasswordHasher passwordHasher,
            TokenService tokenService,
            SecureTokenGenerator tokenGenerator
    ) {
        if (userAccountRepository == null) {
            throw new IllegalArgumentException("user.repository.null");
        }
        if (userSessionRepository == null) {
            throw new IllegalArgumentException("user.session.repository.null");
        }
        if (passwordHasher == null) {
            throw new IllegalArgumentException("user.password_hasher.null");
        }
        if (tokenService == null) {
            throw new IllegalArgumentException("jwt.provider.null");
        }
        if (tokenGenerator == null) {
            throw new IllegalArgumentException("user.refresh_token_generator.null");
        }
        this.userAccountRepository = userAccountRepository;
        this.userSessionRepository = userSessionRepository;
        this.passwordHasher = passwordHasher;
        this.tokenService = tokenService;
        this.tokenGenerator = tokenGenerator;
    }

    /**
     * Signs in with email and password. A wrong email and a wrong password get the same answer; an unverified
     * email is only reported once the password is right.
     *
     * @param command sign-in command
     * @return the new session, or why the credentials were rejected
     */
    @Override
    public Result<AuthenticatedSession, ApplicationError> handle(AuthenticateUserCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "user.command.null"));
        }
        try {
            var optionalAccount = userAccountRepository.findByEmail(new EmailAddress(command.email()));
            if (optionalAccount.isEmpty()
                    || !passwordHasher.matches(command.rawPassword(), optionalAccount.get().snapshot().passwordHash())) {
                return Result.failure(ApplicationError.forbidden("user", "user.credentials.invalid"));
            }
            return openSessionFor(optionalAccount.get(), Instant.now());
        } catch (IllegalArgumentException ex) {
            // A malformed email cannot belong to an account, so it gets the same answer as wrong credentials
            return Result.failure(ApplicationError.forbidden("user", "user.credentials.invalid"));
        }
    }

    /**
     * Opens a session for an active account, such as one that just verified its email.
     *
     * @param command open session command
     * @return the new session, or why it cannot be opened
     */
    @Override
    public Result<AuthenticatedSession, ApplicationError> handle(OpenUserSessionCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "user.command.null"));
        }
        try {
            return userAccountRepository.findById(new UserId(command.userId()))
                    .map(account -> openSessionFor(account, Instant.now()))
                    .orElseGet(() -> Result.failure(ApplicationError.notFound("user", command.userId())));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    /**
     * Rotates a refresh token: revokes its session and opens a new one with the current credentials version.
     *
     * @param command refresh command
     * @return the new session, or why the refresh token was rejected
     */
    @Override
    public Result<AuthenticatedSession, ApplicationError> handle(RefreshUserSessionCommand command) {
        if (command == null || command.refreshToken().isBlank()) {
            return Result.failure(invalidRefreshToken());
        }
        var now = Instant.now();
        var optionalSession = userSessionRepository.findByRefreshTokenHash(
                tokenGenerator.hash(command.refreshToken().trim()));
        if (optionalSession.isEmpty()) {
            return Result.failure(invalidRefreshToken());
        }

        var session = optionalSession.get();
        var userId = session.snapshot().userId();
        if (session.snapshot().revokedAt() != null) {
            // A rotated or revoked token came back: someone else holds a copy, so every session is closed
            revokeAllSessionsOf(userId, now);
            return Result.failure(invalidRefreshToken());
        }
        if (!session.isActive(now)) {
            return Result.failure(invalidRefreshToken());
        }

        var optionalAccount = userAccountRepository.findById(userId);
        session.revoke(now);
        userSessionRepository.save(session);
        if (optionalAccount.isEmpty()
                || optionalAccount.get().snapshot().credentialsVersion() != session.snapshot().credentialsVersion()) {
            return Result.failure(invalidRefreshToken());
        }
        return openSessionFor(optionalAccount.get(), now);
    }

    /**
     * Removes the sessions that expired or were revoked more than {@link UserSession#RETENTION_AFTER_END} ago.
     *
     * @param command purge command
     * @return how many sessions were removed
     */
    @Override
    public Result<Integer, ApplicationError> handle(PurgeEndedUserSessionsCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "user.command.null"));
        }
        return Result.success(userSessionRepository.deleteEndedBefore(
                Instant.now().minus(UserSession.RETENTION_AFTER_END)));
    }

    private Result<AuthenticatedSession, ApplicationError> openSessionFor(UserAccount account, Instant now) {
        var snapshot = account.snapshot();
        if (snapshot.status() != UserAccountStatus.ACTIVE) {
            return Result.failure(ApplicationError.forbidden("email-verification", "user.email.not_verified"));
        }

        var refreshToken = tokenGenerator.generate();
        userSessionRepository.save(UserSession.start(snapshot.id(), tokenGenerator.hash(refreshToken),
                snapshot.credentialsVersion(), now));

        var accessToken = tokenService.generateAccessToken(snapshot.id().userId(), snapshot.email().value(),
                snapshot.role().name(), snapshot.credentialsVersion());
        return Result.success(new AuthenticatedSession(accessToken, refreshToken,
                tokenService.getAccessTokenValiditySeconds(), snapshot.id().userId(), snapshot.email().value(),
                snapshot.role().name()));
    }

    private void revokeAllSessionsOf(UserId userId, Instant now) {
        for (var session : userSessionRepository.findNotRevokedByUserId(userId)) {
            session.revoke(now);
            userSessionRepository.save(session);
        }
    }

    private static ApplicationError invalidRefreshToken() {
        return ApplicationError.forbidden("user", "user.refresh_token.invalid");
    }
}
