package com.arcadiadevs.viora.platform.iam.application.internal.commandservices;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.infrastructure.ratelimit.InMemoryAuthAttemptLimiter;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.codes.VerificationCodeGenerator;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.SecureTokenGenerator;
import com.arcadiadevs.viora.platform.iam.domain.model.FakePasswordHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.UserAccountFixtures;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ChangeUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.DiscardUnverifiedUserAccountsCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UnverifiedUserAccountDiscardedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RequestPasswordResetCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResendEmailVerificationCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResetUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.VerifyEmailCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.events.EmailVerificationRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserSignedUpWithProfileEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.VerificationCodeHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TermsAcceptance;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserAccountStatus;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.VerificationCode;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAccountCommandService sign-up, email verification and password reset Application Unit Tests")
class UserAccountCommandServiceImplTest {

    private static final String EMAIL = "productor@viora.ag";
    private static final String DEMO_EMAIL = "demo@viora.pe";
    private static final String NEW_HASH = "$argon2id$v=19$m=65536,t=3,p=4$newhash";
    private static final VerificationCode CODE = new VerificationCode("482913");

    /** Deterministic stand-in for the Argon2 adapter. */
    private static final VerificationCodeHasher HASHER = new VerificationCodeHasher() {
        @Override
        public String hash(VerificationCode code) {
            return "hashed:" + code.value();
        }

        @Override
        public boolean matches(VerificationCode code, String codeHash) {
            return hash(code).equals(codeHash);
        }
    };

    /** Hands out "link-token" and hashes it as UserAccountFixtures.tokenHashOf("link-token"). */
    private static final SecureTokenGenerator LINK_TOKENS = new SecureTokenGenerator() {
        @Override
        public String generate() {
            return "link-token";
        }

        @Override
        public TokenHash hash(String token) {
            return UserAccountFixtures.tokenHashOf(token);
        }
    };

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TransactionalEmailService emailService;

    private UserAccountCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        VerificationCodeGenerator fixedGenerator = () -> CODE;
        service = new UserAccountCommandServiceImpl(userAccountRepository, eventPublisher, fixedGenerator, HASHER,
                new FakePasswordHasher(), LINK_TOKENS, emailService, "2026-10", DEMO_EMAIL,
                new InMemoryAuthAttemptLimiter(5, 15, 2, 60));
    }

    @Test
    @DisplayName("Signs up a pending account, publishes its events and emails the verification code")
    void signUpCreatesPendingAccount() {
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.empty());
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.handle(signUpCommand(true));

        assertThat(result).isInstanceOf(Result.Success.class);
        var captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        var snapshot = captor.getValue().snapshot();
        assertThat(snapshot.status()).isEqualTo(UserAccountStatus.PENDING_VERIFICATION);
        assertThat(snapshot.termsAcceptance().version()).isEqualTo("2026-10");
        assertThat(snapshot.emailVerification().codeHash()).isEqualTo("hashed:482913");
        var events = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(3)).publishEvent(events.capture());
        assertThat(events.getAllValues()).anyMatch(UserSignedUpWithProfileEvent.class::isInstance)
                .anyMatch(EmailVerificationRequestedEvent.class::isInstance);
        // No transaction is active in a unit test, so the code is sent right away
        verify(emailService).sendEmailVerificationEmail(EMAIL, "482913");
    }

    @Test
    @DisplayName("Rejects a sign-up whose terms were not accepted")
    void signUpWithoutTermsFails() {
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.empty());

        var result = service.handle(signUpCommand(false));

        assertThat(failure(result).details()).isEqualTo("user.terms.not_accepted");
        verify(userAccountRepository, never()).save(any());
        verify(emailService, never()).sendEmailVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Answers a conflict when the email already belongs to an active account")
    void signUpOverActiveAccountConflicts() {
        var active = UserAccountFixtures.activeAccount(EMAIL, NEW_HASH, UserRole.ROLE_PRODUCER);
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(active));

        var result = service.handle(signUpCommand(true));

        assertThat(failure(result).code()).isEqualTo("USER_CONFLICT");
        assertThat(failure(result).details()).isEqualTo("user.email.already_exists");
    }

    @Test
    @DisplayName("Reuses a pending account when the same email signs up again")
    void signUpOverPendingAccountReusesIt() {
        var pending = pendingAccount(Instant.now().minusSeconds(60));
        var id = pending.snapshot().id().userId();
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(pending));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.handle(signUpCommand(true));

        assertThat(((Result.Success<String, ApplicationError>) result).value()).isEqualTo(id);
        verify(emailService).sendEmailVerificationEmail(EMAIL, "482913");
    }

    @Test
    @DisplayName("Answers 429 with the seconds to wait when a pending sign-up is retried within the cooldown")
    void signUpWithinCooldownIsRejected() {
        var pending = pendingAccount(Instant.now());
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(pending));

        var result = service.handle(signUpCommand(true));

        assertThat(failure(result).code()).isEqualTo("EMAIL_VERIFICATION_TOO_MANY_REQUESTS");
        assertThat(failure(result).properties()).containsKey("retryAfterSeconds");
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Activates the account with the right verification code")
    void verifyEmailActivatesAccount() {
        var pending = pendingAccount(Instant.now());
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(pending));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.handle(new VerifyEmailCommand(EMAIL, CODE.value()));

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(pending.snapshot().status()).isEqualTo(UserAccountStatus.ACTIVE);
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Persists a wrong verification code and reports the remaining attempts")
    void wrongVerificationCodeIsPersisted() {
        var pending = pendingAccount(Instant.now());
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(pending));

        var result = service.handle(new VerifyEmailCommand(EMAIL, "000000"));

        assertThat(failure(result).properties())
                .containsEntry("reason", "user.email_verification.invalid_code")
                .containsEntry("remainingAttempts", 2);
        verify(userAccountRepository).save(pending);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Resends the verification code once the cooldown elapsed")
    void resendSendsNewCode() {
        var pending = pendingAccount(Instant.now().minusSeconds(60));
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(pending));

        var result = service.handle(new ResendEmailVerificationCommand(EMAIL));

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(userAccountRepository).save(pending);
        verify(emailService).sendEmailVerificationEmail(EMAIL, "482913");
    }

    @Test
    @DisplayName("Answers an unknown email with the same empty success and sends nothing")
    void resendForUnknownEmailSendsNothing() {
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.empty());

        var result = service.handle(new ResendEmailVerificationCommand(EMAIL));

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(emailService, never()).sendEmailVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Stores only the reset link token hash, publishes the domain event and emails the plain token")
    void requestStoresHashAndEmailsToken() {
        var account = UserAccountFixtures.activeAccount(EMAIL, NEW_HASH, UserRole.ROLE_PRODUCER);
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(account));

        var result = service.handle(new RequestPasswordResetCommand(EMAIL));

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(account.snapshot().resetToken().tokenHash()).isEqualTo(UserAccountFixtures.tokenHashOf("link-token"));
        verify(eventPublisher).publishEvent(any(PasswordResetRequestedEvent.class));
        verify(emailService).sendPasswordResetEmail(EMAIL, "link-token");
    }

    @Test
    @DisplayName("Answers a pending account's reset request like any other and sends nothing")
    void resetRequestOfPendingAccountSendsNothing() {
        var pending = pendingAccount(Instant.now());
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(pending));

        var result = service.handle(new RequestPasswordResetCommand(EMAIL));

        assertThat(result).isInstanceOf(Result.Success.class);
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Answers an unknown, used or replaced link as an expired link")
    void unknownLinkLooksExpired() {
        when(userAccountRepository.findByResetTokenHash(UserAccountFixtures.tokenHashOf("unknown"))).thenReturn(Optional.empty());

        var result = service.handle(new ResetUserPasswordCommand("unknown", "NuevaClave456"));

        assertThat(failure(result).code()).isEqualTo("PASSWORD_RESET_TOKEN_NOT_FOUND");
        assertThat(failure(result).details()).isEqualTo("user.password_reset.link_expired");
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Resets the password with the link found by its token hash")
    void linkResetsPassword() {
        var account = accountWithResetLink();
        when(userAccountRepository.findByResetTokenHash(UserAccountFixtures.tokenHashOf("link-token"))).thenReturn(Optional.of(account));

        var result = service.handle(new ResetUserPasswordCommand("link-token", "NuevaClave456"));

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(account.snapshot().passwordHash()).isEqualTo(FakePasswordHasher.hashOf("NuevaClave456"));
        assertThat(account.snapshot().resetToken()).isNull();
        verify(userAccountRepository).save(account);
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Rejects a weak password before looking the link up")
    void weakResetPasswordListsViolations() {
        var result = service.handle(new ResetUserPasswordCommand("link-token", "abc"));

        assertThat(failure(result).properties()).containsKey("violations");
        verify(userAccountRepository, never()).findByResetTokenHash(any());
    }

    @Test
    @DisplayName("Rejects a weak sign-up password listing every broken rule, and stores nothing")
    void weakSignUpPasswordListsViolations() {
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.empty());

        var result = service.handle(new RegisterUserAccountCommand(EMAIL, "abc", "ROLE_PRODUCER", true,
                null, null, null));

        var error = failure(result);
        assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(error.properties()).containsEntry("violations",
                java.util.List.of("user.password.min_length", "user.password.letters_and_digits"));
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Stores the plain sign-up password only as its hash")
    void signUpHashesThePassword() {
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.empty());
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(new RegisterUserAccountCommand(EMAIL, "Password123", "ROLE_PRODUCER", true, null, null, null));

        var captor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userAccountRepository).save(captor.capture());
        assertThat(captor.getValue().snapshot().passwordHash()).isEqualTo(FakePasswordHasher.hashOf("Password123"));
    }

    @Test
    @DisplayName("Changes the password only with the right current password")
    void changePasswordChecksCurrentPassword() {
        var account = UserAccountFixtures.activeAccount(EMAIL, FakePasswordHasher.hashOf("Password123").value(),
                UserRole.ROLE_PRODUCER);
        var userId = account.snapshot().id().userId();
        when(userAccountRepository.findById(any())).thenReturn(Optional.of(account));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var wrong = service.handle(new ChangeUserPasswordCommand(userId, "Otra12345", "NuevaClave456"));
        assertThat(failure(wrong).properties()).containsEntry("reason", "user.password.current_invalid");

        var reused = service.handle(new ChangeUserPasswordCommand(userId, "Password123", "Password123"));
        assertThat(failure(reused).properties()).containsEntry("reason", "user.password.reused");

        var changed = service.handle(new ChangeUserPasswordCommand(userId, "Password123", "NuevaClave456"));
        assertThat(changed).isInstanceOf(Result.Success.class);
        assertThat(account.snapshot().credentialsVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Discards the abandoned pending accounts, publishing the discard before deleting each one")
    void discardsAbandonedPendingAccounts() {
        var abandoned = pendingAccount(Instant.now().minus(UserAccount.UNVERIFIED_RETENTION).minusSeconds(60));
        var recent = pendingAccount(Instant.now());
        when(userAccountRepository.findPendingVerificationNotUpdatedSince(any(Instant.class)))
                .thenReturn(java.util.List.of(abandoned, recent));

        var result = service.handle(new DiscardUnverifiedUserAccountsCommand());

        assertThat(((Result.Success<Integer, ApplicationError>) result).value()).isEqualTo(1);
        var order = org.mockito.Mockito.inOrder(eventPublisher, userAccountRepository);
        order.verify(eventPublisher).publishEvent(any(UnverifiedUserAccountDiscardedEvent.class));
        order.verify(userAccountRepository).delete(abandoned);
        verify(userAccountRepository, never()).delete(recent);
    }

    @Test
    @DisplayName("A second sign-up with another password only resends the code: the pending account keeps its password")
    void signUpOverPendingAccountWithAnotherPasswordCannotTakeItOver() {
        var pending = pendingAccount(Instant.now().minusSeconds(60));
        var originalHash = pending.snapshot().passwordHash();
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(pending));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.handle(new RegisterUserAccountCommand(EMAIL, "Atacante2026", "ROLE_PRODUCER", true,
                "Attacker", "PE", "+51911111111"));

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(pending.snapshot().passwordHash()).isEqualTo(originalHash);
        verify(eventPublisher, never()).publishEvent(any(UserSignedUpWithProfileEvent.class));
        verify(emailService).sendEmailVerificationEmail(EMAIL, "482913");
    }

    @Test
    @DisplayName("A second sign-up with the same password refreshes the profile details of the pending account")
    void signUpOverPendingAccountWithTheSamePasswordRefreshesIt() {
        var pending = UserAccount.signUp(new EmailAddress(EMAIL), FakePasswordHasher.hashOf(NEW_HASH),
                UserRole.ROLE_PRODUCER, new TermsAcceptance("2026-10", Instant.now().minusSeconds(60)), null,
                HASHER.hash(CODE), Instant.now().minusSeconds(60));
        pending.clearDomainEvents();
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(pending));
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.handle(signUpCommand(true));

        verify(eventPublisher).publishEvent(any(UserSignedUpWithProfileEvent.class));
    }

    @Test
    @DisplayName("Sends at most the allowed reset links to one email, answering every request the same")
    void resetLinksToOneEmailAreLimited() {
        when(userAccountRepository.findByEmail(any(EmailAddress.class)))
                .thenAnswer(invocation -> Optional.of(UserAccountFixtures.activeAccount(EMAIL, NEW_HASH,
                        UserRole.ROLE_PRODUCER)));

        for (int i = 0; i < 4; i++) {
            assertThat(service.handle(new RequestPasswordResetCommand(EMAIL))).isInstanceOf(Result.Success.class);
        }

        verify(emailService, times(2)).sendPasswordResetEmail(eq(EMAIL), anyString());
    }

    @Test
    @DisplayName("Nobody can change the password of the shared demo account or ask for its reset link")
    void demoAccountKeepsItsPassword() {
        var demo = UserAccountFixtures.activeAccount(DEMO_EMAIL, FakePasswordHasher.hashOf("Password123").value(),
                UserRole.ROLE_PRODUCER);
        when(userAccountRepository.findById(any(UserId.class))).thenReturn(Optional.of(demo));
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(demo));

        var change = service.handle(new ChangeUserPasswordCommand(demo.snapshot().id().userId(), "Password123",
                "OtraClave2026"));
        service.handle(new RequestPasswordResetCommand(DEMO_EMAIL));

        assertThat(failure(change).details()).isEqualTo("user.account.demo_protected");
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    private static RegisterUserAccountCommand signUpCommand(boolean termsAccepted) {
        return new RegisterUserAccountCommand(EMAIL, NEW_HASH, "ROLE_PRODUCER", termsAccepted,
                "Juan Perez", "PE", "+51987654321");
    }

    private static ApplicationError failure(Result<?, ApplicationError> result) {
        assertThat(result).isInstanceOf(Result.Failure.class);
        return ((Result.Failure<?, ApplicationError>) result).error();
    }

    private static UserAccount pendingAccount(Instant issuedAt) {
        var account = UserAccount.signUp(new EmailAddress(EMAIL),
                new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$oldhash"), UserRole.ROLE_PRODUCER,
                new TermsAcceptance("2026-10", issuedAt), null, HASHER.hash(CODE), issuedAt);
        account.clearDomainEvents();
        return account;
    }

    private static UserAccount accountWithResetLink() {
        var account = UserAccountFixtures.activeAccount(EMAIL, "$argon2id$v=19$m=65536,t=3,p=4$oldhash",
                UserRole.ROLE_PRODUCER);
        account.requestPasswordReset(LINK_TOKENS.hash("link-token"), Instant.now());
        account.clearDomainEvents();
        return account;
    }
}
