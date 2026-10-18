package com.arcadiadevs.viora.platform.iam.domain.model;

import com.arcadiadevs.viora.platform.iam.domain.exceptions.EmailVerificationCooldownException;
import com.arcadiadevs.viora.platform.iam.domain.exceptions.InvalidEmailVerificationCodeException;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.events.EmailVerificationRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordChangedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetCompletedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountActivatedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountRegisteredEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserSignedUpWithProfileEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.VerificationCodeHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserAccount Aggregate Root and Value Objects Domain Unit Tests")
class UserAccountTest {

    private final EmailAddress email = new EmailAddress("productor@viora.ag");
    private final PasswordHash hash = new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$somehash");
    private final UserRole role = UserRole.ROLE_PRODUCER;

    @Test
    @DisplayName("Should sign up a pending account and emit the registration and verification events")
    void shouldSignUpPendingAccount() {
        var now = Instant.now();
        var account = UserAccount.signUp(email, hash, role, TERMS, null, HASHER.hash(CODE), now);

        var snapshot = account.snapshot();
        assertThat(snapshot.id().userId()).isNotBlank();
        assertThat(snapshot.email().value()).isEqualTo("productor@viora.ag");
        assertThat(snapshot.status()).isEqualTo(UserAccountStatus.PENDING_VERIFICATION);
        assertThat(snapshot.termsAcceptance()).isEqualTo(TERMS);
        assertThat(snapshot.emailVerification().codeHash()).isEqualTo(HASHER.hash(CODE));
        assertThat(snapshot.emailVerification().expiresAt()).isEqualTo(now.plus(10, ChronoUnit.MINUTES));
        assertThat(snapshot.resetToken()).isNull();

        assertThat(account.domainEvents()).hasSize(2);
        var events = account.domainEvents().iterator();
        var registered = (UserAccountRegisteredEvent) events.next();
        assertThat(registered.userId()).isEqualTo(snapshot.id().userId());
        assertThat(registered.role()).isEqualTo("ROLE_PRODUCER");
        assertThat(events.next()).isInstanceOf(EmailVerificationRequestedEvent.class);
    }

    @Test
    @DisplayName("Should only let producers sign up, and only after accepting the terms")
    void shouldRejectSignUpWithoutTermsOrWithAnotherRole() {
        var now = Instant.now();

        assertThatThrownBy(() -> UserAccount.signUp(email, hash, UserRole.ROLE_TECHNICAL_MANAGER, TERMS, null,
                HASHER.hash(CODE), now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.role.sign_up_not_allowed");
        assertThatThrownBy(() -> UserAccount.signUp(email, hash, role, null, null, HASHER.hash(CODE), now))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.terms.not_accepted");
    }

    @Test
    @DisplayName("Should activate the account with the right code and emit UserAccountActivatedEvent")
    void shouldVerifyEmailAndActivate() {
        var account = pendingAccount(Instant.now());

        account.verifyEmail(CODE, HASHER, Instant.now());

        assertThat(account.snapshot().status()).isEqualTo(UserAccountStatus.ACTIVE);
        assertThat(account.snapshot().emailVerification()).isNull();
        assertThat(account.domainEvents()).hasSize(1);
        assertThat(account.domainEvents().iterator().next()).isInstanceOf(UserAccountActivatedEvent.class);
        assertThatThrownBy(() -> account.verifyEmail(CODE, HASHER, Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.email.already_verified");
    }

    @Test
    @DisplayName("Should count wrong verification codes and lock the code after 3 attempts")
    void shouldLockVerificationAfterThreeWrongCodes() {
        var account = pendingAccount(Instant.now());
        var wrongCode = new VerificationCode("000000");

        for (int remaining = 2; remaining >= 0; remaining--) {
            final int expected = remaining;
            assertThatThrownBy(() -> account.verifyEmail(wrongCode, HASHER, Instant.now()))
                    .isInstanceOfSatisfying(InvalidEmailVerificationCodeException.class,
                            ex -> assertThat(ex.remainingAttempts()).isEqualTo(expected));
        }

        assertThatThrownBy(() -> account.verifyEmail(CODE, HASHER, Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.email_verification.attempts_exceeded");
        assertThat(account.snapshot().status()).isEqualTo(UserAccountStatus.PENDING_VERIFICATION);
    }

    @Test
    @DisplayName("Should reject a verification code after 10 minutes")
    void shouldRejectExpiredVerificationCode() {
        var issuedAt = Instant.now();
        var account = pendingAccount(issuedAt);

        assertThatThrownBy(() -> account.verifyEmail(CODE, HASHER, issuedAt.plus(11, ChronoUnit.MINUTES)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.email_verification.code_expired");
    }

    @Test
    @DisplayName("Should resend the verification code only after the 45 second cooldown")
    void shouldEnforceResendCooldown() {
        var issuedAt = Instant.now();
        var account = pendingAccount(issuedAt);

        assertThatThrownBy(() -> account.resendEmailVerification(HASHER.hash(CODE), issuedAt.plusSeconds(10)))
                .isInstanceOfSatisfying(EmailVerificationCooldownException.class,
                        ex -> assertThat(ex.retryAfterSeconds()).isEqualTo(35));

        account.resendEmailVerification("hashed:111111", issuedAt.plusSeconds(45));

        assertThat(account.snapshot().emailVerification().codeHash()).isEqualTo("hashed:111111");
        assertThat(account.snapshot().emailVerification().failedAttempts()).isZero();
        assertThat(account.domainEvents()).hasSize(1);
        assertThat(account.domainEvents().iterator().next()).isInstanceOf(EmailVerificationRequestedEvent.class);
    }

    @Test
    @DisplayName("Should restart a pending sign-up keeping the same identity and raising the sign-up events again")
    void shouldRestartPendingSignUp() {
        var issuedAt = Instant.now();
        var account = pendingAccount(issuedAt);
        var id = account.snapshot().id();
        var newHash = new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$retryhash");
        var details = new OnboardingProfileDetails("Juan Perez", "PE", "+51987654321");

        account.restartSignUp(newHash, TERMS, details, "hashed:222222", issuedAt.plusSeconds(60));

        assertThat(account.snapshot().id()).isEqualTo(id);
        assertThat(account.snapshot().passwordHash()).isEqualTo(newHash);
        assertThat(account.snapshot().emailVerification().codeHash()).isEqualTo("hashed:222222");
        assertThat(account.domainEvents()).hasSize(2);
        var profileEvent = (UserSignedUpWithProfileEvent) account.domainEvents().iterator().next();
        assertThat(profileEvent.userId()).isEqualTo(id.userId());
    }

    @Test
    @DisplayName("Should not restart the sign-up of an active account")
    void shouldNotRestartSignUpOfActiveAccount() {
        var account = UserAccountFixtures.activeAccount(email.value(), hash.value(), role);

        assertThatThrownBy(() -> account.restartSignUp(hash, TERMS, null, "hashed:222222", Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.email.already_exists");
    }

    @Test
    @DisplayName("Should not issue a password recovery code to a pending account")
    void shouldNotResetPasswordOfPendingAccount() {
        var account = pendingAccount(Instant.now());

        assertThatThrownBy(() -> account.requestPasswordReset(HASHER.hash(CODE), Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.email.not_verified");
    }

    @Test
    @DisplayName("Should validate EmailAddress syntax and normalization")
    void shouldValidateEmailAddress() {
        var normalized = new EmailAddress("  TEST@viora.AG ");
        assertThat(normalized.value()).isEqualTo("test@viora.ag");

        assertThatThrownBy(() -> new EmailAddress(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.email.null_or_empty");

        assertThatThrownBy(() -> new EmailAddress("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.email.invalid_format");
    }

    @Test
    @DisplayName("Should validate UserId generation and malformed UUIDs")
    void shouldValidateUserId() {
        var id = new UserId();
        assertThat(id.userId()).isNotBlank();
        assertThat(UUID.fromString(id.userId())).isNotNull();

        assertThatThrownBy(() -> new UserId("invalid-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.id.invalid_uuid");
    }

    @Test
    @DisplayName("Should successfully change password and emit PasswordChangedEvent")
    void shouldSuccessfullyChangePassword() {
        var account = UserAccountFixtures.activeAccount(email.value(), hash.value(), role);
        account.clearDomainEvents();

        var newHash = new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$anotherhash");
        account.changePassword(newHash);

        assertThat(account.snapshot().passwordHash().value()).isEqualTo(newHash.value());
        assertThat(account.domainEvents()).hasSize(1);
        var event = (PasswordChangedEvent) account.domainEvents().iterator().next();
        assertThat(event.userId()).isEqualTo(account.snapshot().id().userId());
    }

    @Test
    @DisplayName("Should sign up with onboarding details and raise UserSignedUpWithProfileEvent")
    void shouldRaiseSignedUpWithProfileEvent() {
        var details = new OnboardingProfileDetails("Juan Perez", "PE", "+51987654321");

        var account = UserAccount.signUp(email, hash, role, TERMS, details, HASHER.hash(CODE), Instant.now());

        assertThat(account.domainEvents()).hasSize(3);
        var event = account.domainEvents().stream()
                .filter(UserSignedUpWithProfileEvent.class::isInstance)
                .map(UserSignedUpWithProfileEvent.class::cast)
                .findFirst().orElseThrow();
        assertThat(event.userId()).isEqualTo(account.snapshot().id().userId());
        assertThat(event.fullName()).isEqualTo("Juan Perez");
        assertThat(event.phoneNumber()).isEqualTo("+51987654321");
    }

    @Test
    @DisplayName("Should store only the code hash and emit PasswordResetRequestedEvent without the code")
    void shouldRequestPasswordReset() {
        var account = UserAccountFixtures.activeAccount(email.value(), hash.value(), role);
        account.clearDomainEvents();
        var now = Instant.now();

        account.requestPasswordReset(HASHER.hash(CODE), now);

        var token = account.snapshot().resetToken();
        assertThat(token.codeHash()).isEqualTo(HASHER.hash(CODE));
        assertThat(token.expiresAt()).isEqualTo(now.plus(15, ChronoUnit.MINUTES));
        assertThat(token.failedAttempts()).isZero();

        assertThat(account.domainEvents()).hasSize(1);
        var event = (PasswordResetRequestedEvent) account.domainEvents().iterator().next();
        assertThat(event.email()).isEqualTo(email.value());
        assertThat(event.expiresAt()).isEqualTo(token.expiresAt());
    }

    @Test
    @DisplayName("Should successfully reset password with the valid code")
    void shouldResetPasswordWithValidToken() {
        var account = accountWithActiveCode();

        var newHash = new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$resetpasswordhash");
        account.resetPassword(CODE, HASHER, newHash, Instant.now());

        assertThat(account.snapshot().passwordHash().value()).isEqualTo(newHash.value());
        assertThat(account.snapshot().resetToken()).isNull();
        assertThat(account.domainEvents()).hasSize(1);
        assertThat(account.domainEvents().iterator().next()).isInstanceOf(PasswordResetCompletedEvent.class);
    }

    @Test
    @DisplayName("Should reject expired password reset code")
    void shouldRejectExpiredResetToken() {
        var account = accountWithActiveCode();

        var newHash = new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$resetpasswordhash");
        Instant future = Instant.now().plus(20, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> account.resetPassword(CODE, HASHER, newHash, future))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.password_reset.token_expired");
    }

    @Test
    @DisplayName("Should reject a reset when no code was requested")
    void shouldRejectResetWithoutActiveCode() {
        var account = UserAccountFixtures.activeAccount(email.value(), hash.value(), role);
        var newHash = new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$resetpasswordhash");

        assertThatThrownBy(() -> account.resetPassword(CODE, HASHER, newHash, Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.password_reset.no_active_token");
    }

    @Test
    @DisplayName("Should count wrong codes and lock the token after the maximum attempts")
    void shouldLockResetTokenAfterMaxFailedAttempts() {
        var account = accountWithActiveCode();
        var wrongCode = new VerificationCode("000000");
        var newHash = new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$resetpasswordhash");

        for (int attempt = 1; attempt <= PasswordResetToken.MAX_FAILED_ATTEMPTS; attempt++) {
            assertThatThrownBy(() -> account.resetPassword(wrongCode, HASHER, newHash, Instant.now()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("user.password_reset.invalid_token");
            assertThat(account.snapshot().resetToken().failedAttempts()).isEqualTo(attempt);
        }

        assertThatThrownBy(() -> account.resetPassword(CODE, HASHER, newHash, Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.password_reset.attempts_exceeded");
        assertThat(account.snapshot().passwordHash().value()).isEqualTo(hash.value());
    }

    @Test
    @DisplayName("Should validate VerificationCode format and PasswordResetToken invariants")
    void shouldValidateResetValueObjects() {
        var now = Instant.now();

        assertThat(new VerificationCode(" 012345 ").value()).isEqualTo("012345");
        assertThatThrownBy(() -> new VerificationCode("12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.reset_token.format");
        assertThatThrownBy(() -> new VerificationCode("abcdef"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.reset_token.format");
        assertThatThrownBy(() -> new PasswordResetToken(" ", now, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.reset_token.null_or_empty");
        assertThatThrownBy(() -> new PasswordResetToken("hash", now, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.reset_token.failed_attempts.negative");
        assertThatThrownBy(() -> PasswordResetToken.issue("hash", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.clock.now.null");
    }

    private static final VerificationCode CODE = new VerificationCode("482913");

    /** Deterministic stand-in for the Argon2 adapter: the domain only needs hash/matches semantics. */
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

    private static final TermsAcceptance TERMS = new TermsAcceptance("2026-10", Instant.parse("2026-10-17T12:00:00Z"));

    private UserAccount pendingAccount(Instant issuedAt) {
        var account = UserAccount.signUp(email, hash, role, TERMS, null, HASHER.hash(CODE), issuedAt);
        account.clearDomainEvents();
        return account;
    }

    private UserAccount accountWithActiveCode() {
        var account = UserAccountFixtures.activeAccount(email.value(), hash.value(), role);
        account.requestPasswordReset(HASHER.hash(CODE), Instant.now());
        account.clearDomainEvents();
        return account;
    }
}
