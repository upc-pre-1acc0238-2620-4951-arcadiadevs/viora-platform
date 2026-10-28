package com.arcadiadevs.viora.platform.iam.domain.model;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.domain.exceptions.EmailVerificationCooldownException;
import com.arcadiadevs.viora.platform.iam.domain.exceptions.InvalidEmailVerificationCodeException;
import com.arcadiadevs.viora.platform.iam.domain.exceptions.WeakPasswordException;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.events.EmailVerificationRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordChangedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetCompletedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UnverifiedUserAccountDiscardedEvent;
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
    @DisplayName("Should restart a pending sign-up keeping the identity and the password, raising the events again")
    void shouldRestartPendingSignUp() {
        var issuedAt = Instant.now();
        var account = pendingAccount(issuedAt);
        var id = account.snapshot().id();
        var originalHash = account.snapshot().passwordHash();
        var details = new OnboardingProfileDetails("Juan Perez", "PE", "+51987654321");

        account.restartSignUp(TERMS, details, "hashed:222222", issuedAt.plusSeconds(60));

        assertThat(account.snapshot().id()).isEqualTo(id);
        assertThat(account.snapshot().passwordHash()).isEqualTo(originalHash);
        assertThat(account.snapshot().emailVerification().codeHash()).isEqualTo("hashed:222222");
        assertThat(account.domainEvents()).hasSize(2);
        var profileEvent = (UserSignedUpWithProfileEvent) account.domainEvents().iterator().next();
        assertThat(profileEvent.userId()).isEqualTo(id.userId());
    }

    @Test
    @DisplayName("Should not restart the sign-up of an active account")
    void shouldNotRestartSignUpOfActiveAccount() {
        var account = UserAccountFixtures.activeAccount(email.value(), hash.value(), role);

        assertThatThrownBy(() -> account.restartSignUp(TERMS, null, "hashed:222222", Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.email.already_exists");
    }

    @Test
    @DisplayName("Should not issue a password recovery code to a pending account")
    void shouldNotResetPasswordOfPendingAccount() {
        var account = pendingAccount(Instant.now());

        assertThatThrownBy(() -> account.requestPasswordReset(LINK_HASH, Instant.now()))
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
    @DisplayName("Should change the password after checking the current one, remember the old one and raise the version")
    void shouldSuccessfullyChangePassword() {
        var account = UserAccountFixtures.activeAccount(email.value(), FakePasswordHasher.hashOf("Password123").value(), role);

        account.changePassword("Password123", NEW_PASSWORD, PW, Instant.now());

        var snapshot = account.snapshot();
        assertThat(snapshot.passwordHash()).isEqualTo(FakePasswordHasher.hashOf(NEW_PASSWORD.value()));
        assertThat(snapshot.passwordHistory().previousHashes()).containsExactly(FakePasswordHasher.hashOf("Password123"));
        assertThat(snapshot.credentialsVersion()).isEqualTo(1);
        assertThat(account.domainEvents()).hasSize(1);
        var event = (PasswordChangedEvent) account.domainEvents().iterator().next();
        assertThat(event.userId()).isEqualTo(snapshot.id().userId());
    }

    @Test
    @DisplayName("Should reject a password change with a wrong current password")
    void shouldRejectWrongCurrentPassword() {
        var account = UserAccountFixtures.activeAccount(email.value(), FakePasswordHasher.hashOf("Password123").value(), role);

        assertThatThrownBy(() -> account.changePassword("Otra12345", NEW_PASSWORD, PW, Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.password.current_invalid");
        assertThat(account.snapshot().credentialsVersion()).isZero();
    }

    @Test
    @DisplayName("Should reject any of the last 5 passwords, the current one included")
    void shouldRejectRecentlyUsedPasswords() {
        var account = UserAccountFixtures.activeAccount(email.value(), FakePasswordHasher.hashOf("Clave0001").value(), role);
        for (int i = 2; i <= 6; i++) {
            account.changePassword("Clave000" + (i - 1), new NewPassword("Clave000" + i), PW, Instant.now());
        }
        // Current: Clave0006; remembered: Clave0005..Clave0002; Clave0001 is the sixth and is forgotten
        assertThat(account.snapshot().passwordHistory().previousHashes()).hasSize(PasswordHistory.REMEMBERED_PASSWORDS - 1);

        for (var reused : new String[] {"Clave0006", "Clave0005", "Clave0002"}) {
            assertThatThrownBy(() -> account.changePassword("Clave0006", new NewPassword(reused), PW, Instant.now()))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessage("user.password.reused");
        }
        account.changePassword("Clave0006", new NewPassword("Clave0001"), PW, Instant.now());
        assertThat(account.snapshot().credentialsVersion()).isEqualTo(6);
    }

    @Test
    @DisplayName("Should report every broken password rule at once")
    void shouldReportEveryBrokenPasswordRule() {
        assertThatThrownBy(() -> new NewPassword("abc"))
                .isInstanceOfSatisfying(WeakPasswordException.class, ex -> assertThat(ex.violations())
                        .containsExactly("user.password.min_length", "user.password.letters_and_digits"));
        assertThatThrownBy(() -> new NewPassword("12345678"))
                .isInstanceOfSatisfying(WeakPasswordException.class, ex -> assertThat(ex.violations())
                        .containsExactly("user.password.letters_and_digits"));
        assertThatThrownBy(() -> new NewPassword("abc12"))
                .isInstanceOfSatisfying(WeakPasswordException.class, ex -> assertThat(ex.violations())
                        .containsExactly("user.password.min_length"));
        assertThat(new NewPassword("NuevaClave456").toString()).doesNotContain("NuevaClave456");
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
    @DisplayName("Should store only the link token hash and emit PasswordResetRequestedEvent without the token")
    void shouldRequestPasswordReset() {
        var account = UserAccountFixtures.activeAccount(email.value(), hash.value(), role);
        var now = Instant.now();

        account.requestPasswordReset(LINK_HASH, now);

        var token = account.snapshot().resetToken();
        assertThat(token.tokenHash()).isEqualTo(LINK_HASH);
        assertThat(token.expiresAt()).isEqualTo(now.plus(30, ChronoUnit.MINUTES));
        assertThat(account.domainEvents()).hasSize(1);
        var event = (PasswordResetRequestedEvent) account.domainEvents().iterator().next();
        assertThat(event.email()).isEqualTo(email.value());
    }

    @Test
    @DisplayName("Should reset the password with the link once, then reject the used link")
    void shouldResetPasswordWithLinkOnce() {
        var account = accountWithResetLink(Instant.now());

        account.resetPassword(LINK_HASH, NEW_PASSWORD, PW, Instant.now());

        assertThat(account.snapshot().passwordHash()).isEqualTo(FakePasswordHasher.hashOf(NEW_PASSWORD.value()));
        assertThat(account.snapshot().resetToken()).isNull();
        assertThat(account.snapshot().credentialsVersion()).isEqualTo(1);
        assertThat(account.domainEvents()).hasSize(1);
        assertThat(account.domainEvents().iterator().next()).isInstanceOf(PasswordResetCompletedEvent.class);
        assertThatThrownBy(() -> account.resetPassword(LINK_HASH, new NewPassword("OtraClave789"), PW, Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.password_reset.link_expired");
    }

    @Test
    @DisplayName("Should reject a link after 30 minutes, a replaced link, and a reset without link")
    void shouldRejectExpiredReplacedOrMissingLink() {
        var issuedAt = Instant.now();
        var account = accountWithResetLink(issuedAt);

        assertThat(account.canResetPasswordWith(LINK_HASH, issuedAt.plus(29, ChronoUnit.MINUTES))).isTrue();
        assertThat(account.canResetPasswordWith(LINK_HASH, issuedAt.plus(31, ChronoUnit.MINUTES))).isFalse();
        assertThatThrownBy(() -> account.resetPassword(LINK_HASH, NEW_PASSWORD, PW, issuedAt.plus(31, ChronoUnit.MINUTES)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.password_reset.link_expired");

        account.requestPasswordReset(UserAccountFixtures.tokenHashOf("other-link"), issuedAt);
        assertThat(account.canResetPasswordWith(LINK_HASH, issuedAt)).isFalse();

        var withoutLink = UserAccountFixtures.activeAccount(email.value(), hash.value(), role);
        assertThatThrownBy(() -> withoutLink.resetPassword(LINK_HASH, NEW_PASSWORD, PW, Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.password_reset.link_expired");
    }

    @Test
    @DisplayName("Should reject a reset to a recent password without consuming the link")
    void shouldRejectResetToRecentPassword() {
        var account = UserAccountFixtures.activeAccount(email.value(), FakePasswordHasher.hashOf("Password123").value(), role);
        account.requestPasswordReset(LINK_HASH, Instant.now());

        assertThatThrownBy(() -> account.resetPassword(LINK_HASH, new NewPassword("Password123"), PW, Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.password.reused");
        assertThat(account.snapshot().resetToken()).isNotNull();

        account.resetPassword(LINK_HASH, NEW_PASSWORD, PW, Instant.now());
        assertThat(account.snapshot().credentialsVersion()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should validate VerificationCode format and PasswordResetToken invariants")
    void shouldValidateResetValueObjects() {
        assertThat(new VerificationCode(" 012345 ").value()).isEqualTo("012345");
        assertThatThrownBy(() -> new VerificationCode("12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.verification_code.format");
        assertThatThrownBy(() -> new VerificationCode("abcdef"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.verification_code.format");
        assertThatThrownBy(() -> new PasswordResetToken(null, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.reset_token.null_or_empty");
        assertThatThrownBy(() -> PasswordResetToken.issue(LINK_HASH, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.clock.now.null");
        assertThat(PasswordResetToken.issue(LINK_HASH, Instant.now()).matches(LINK_HASH)).isTrue();
        assertThat(PasswordResetToken.issue(LINK_HASH, Instant.now()).matches(UserAccountFixtures.tokenHashOf("other"))).isFalse();
    }

    private static final TokenHash LINK_HASH = UserAccountFixtures.tokenHashOf("link-token");

    private UserAccount accountWithResetLink(Instant issuedAt) {
        var account = UserAccountFixtures.activeAccount(email.value(), hash.value(), role);
        account.requestPasswordReset(LINK_HASH, issuedAt);
        account.clearDomainEvents();
        return account;
    }

    private static final VerificationCode CODE = new VerificationCode("482913");
    private static final NewPassword NEW_PASSWORD = new NewPassword("NuevaClave456");
    private static final FakePasswordHasher PW = new FakePasswordHasher();

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


    @Test
    @DisplayName("Should accept only 64 hexadecimal characters as a token hash")
    void shouldValidateTokenHash() {
        assertThat(new TokenHash(" " + LINK_HASH.value().toUpperCase() + " ")).isEqualTo(LINK_HASH);
        assertThat(LINK_HASH.matches(UserAccountFixtures.tokenHashOf("link-token"))).isTrue();
        assertThat(LINK_HASH.matches(null)).isFalse();
        assertThatThrownBy(() -> new TokenHash("link-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.token_hash.invalid");
        assertThatThrownBy(() -> new TokenHash(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.token_hash.null_or_empty");
    }

    @Test
    @DisplayName("Should discard only a pending account with no activity for 7 days")
    void shouldDiscardOnlyAbandonedPendingAccounts() {
        var signedUpAt = Instant.now().minus(8, ChronoUnit.DAYS);
        var abandoned = pendingAccount(signedUpAt);
        assertThat(abandoned.isAbandonedUnverified(Instant.now())).isTrue();

        abandoned.discardUnverified(Instant.now());
        assertThat(abandoned.domainEvents()).hasSize(1);
        var event = (UnverifiedUserAccountDiscardedEvent) abandoned.domainEvents().iterator().next();
        assertThat(event.userId()).isEqualTo(abandoned.snapshot().id().userId());

        var recent = pendingAccount(Instant.now().minus(6, ChronoUnit.DAYS));
        assertThat(recent.isAbandonedUnverified(Instant.now())).isFalse();
        assertThatThrownBy(() -> recent.discardUnverified(Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.account.not_discardable");

        var active = UserAccountFixtures.activeAccount(email.value(), hash.value(), role);
        assertThat(active.isAbandonedUnverified(Instant.now().plus(30, ChronoUnit.DAYS))).isFalse();
    }
}
