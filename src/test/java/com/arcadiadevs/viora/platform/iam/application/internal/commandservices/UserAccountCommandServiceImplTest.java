package com.arcadiadevs.viora.platform.iam.application.internal.commandservices;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.codes.VerificationCodeGenerator;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
import com.arcadiadevs.viora.platform.iam.domain.model.UserAccountFixtures;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAccountCommandService sign-up, email verification and password reset Application Unit Tests")
class UserAccountCommandServiceImplTest {

    private static final String EMAIL = "productor@viora.ag";
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
                emailService, "2026-10");
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
    @DisplayName("Stores only the reset code hash, publishes the domain event and emails the plain code")
    void requestStoresHashAndEmailsCode() {
        var account = UserAccountFixtures.activeAccount(EMAIL, NEW_HASH, UserRole.ROLE_PRODUCER);
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(account));

        var result = service.handle(new RequestPasswordResetCommand(EMAIL));

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(account.snapshot().resetToken().codeHash()).isEqualTo("hashed:482913");
        verify(eventPublisher).publishEvent(any(PasswordResetRequestedEvent.class));
        verify(emailService).sendPasswordResetEmail(EMAIL, "482913");
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
    @DisplayName("Answers an unknown email exactly like a wrong reset code")
    void unknownEmailLooksLikeWrongCode() {
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.empty());

        var result = service.handle(new ResetUserPasswordCommand(EMAIL, "123456", NEW_HASH));

        assertThat(failure(result).details()).isEqualTo("user.password_reset.invalid_token");
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Persists the failed reset attempt when the code is wrong")
    void wrongCodeIsPersisted() {
        var account = accountWithActiveResetCode();
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(account));

        var result = service.handle(new ResetUserPasswordCommand(EMAIL, "000000", NEW_HASH));

        assertThat(result).isInstanceOf(Result.Failure.class);
        verify(userAccountRepository).save(account);
        assertThat(account.snapshot().resetToken().failedAttempts()).isEqualTo(1);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Resets the password with the right code for the right email")
    void rightCodeResetsPassword() {
        var account = accountWithActiveResetCode();
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(account));

        var result = service.handle(new ResetUserPasswordCommand(EMAIL, CODE.value(), NEW_HASH));

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(account.snapshot().passwordHash().value()).isEqualTo(NEW_HASH);
        assertThat(account.snapshot().resetToken()).isNull();
        verify(eventPublisher).publishEvent(any(Object.class));
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

    private static UserAccount accountWithActiveResetCode() {
        var account = UserAccountFixtures.activeAccount(EMAIL, "$argon2id$v=19$m=65536,t=3,p=4$oldhash",
                UserRole.ROLE_PRODUCER);
        account.requestPasswordReset(HASHER.hash(CODE), Instant.now());
        account.clearDomainEvents();
        return account;
    }
}
