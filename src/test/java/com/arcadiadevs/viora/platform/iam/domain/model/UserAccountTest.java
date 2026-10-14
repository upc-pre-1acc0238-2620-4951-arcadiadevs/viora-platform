package com.arcadiadevs.viora.platform.iam.domain.model;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordChangedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetCompletedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountRegisteredEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserSignedUpWithProfileEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.PasswordResetCodeHasher;
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
    @DisplayName("Should successfully register UserAccount and publish UserAccountRegisteredEvent")
    void shouldSuccessfullyRegisterUserAccount() {
        var account = UserAccount.register(email, hash, role);

        assertThat(account).isNotNull();
        var snapshot = account.snapshot();
        assertThat(snapshot.id()).isNotNull();
        assertThat(snapshot.id().userId()).isNotBlank();
        assertThat(snapshot.email().value()).isEqualTo("productor@viora.ag");
        assertThat(snapshot.passwordHash().value()).isEqualTo(hash.value());
        assertThat(snapshot.role()).isEqualTo(UserRole.ROLE_PRODUCER);
        assertThat(snapshot.resetToken()).isNull();

        assertThat(account.domainEvents()).hasSize(1);
        var event = (UserAccountRegisteredEvent) account.domainEvents().iterator().next();
        assertThat(event.userId()).isEqualTo(snapshot.id().userId());
        assertThat(event.email()).isEqualTo("productor@viora.ag");
        assertThat(event.role()).isEqualTo("ROLE_PRODUCER");
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
        var account = UserAccount.register(email, hash, role);
        account.clearDomainEvents();

        var newHash = new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$anotherhash");
        account.changePassword(newHash);

        assertThat(account.snapshot().passwordHash().value()).isEqualTo(newHash.value());
        assertThat(account.domainEvents()).hasSize(1);
        var event = (PasswordChangedEvent) account.domainEvents().iterator().next();
        assertThat(event.userId()).isEqualTo(account.snapshot().id().userId());
    }

    @Test
    @DisplayName("Should register with onboarding details and raise UserSignedUpWithProfileEvent")
    void shouldRaiseSignedUpWithProfileEvent() {
        var details = new OnboardingProfileDetails("Juan Perez", "PE", "+51987654321");

        var account = UserAccount.register(email, hash, role, details);

        assertThat(account.domainEvents()).hasSize(2);
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
        var account = UserAccount.register(email, hash, role);
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
        var account = UserAccount.register(email, hash, role);
        var newHash = new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$resetpasswordhash");

        assertThatThrownBy(() -> account.resetPassword(CODE, HASHER, newHash, Instant.now()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.password_reset.no_active_token");
    }

    @Test
    @DisplayName("Should count wrong codes and lock the token after the maximum attempts")
    void shouldLockResetTokenAfterMaxFailedAttempts() {
        var account = accountWithActiveCode();
        var wrongCode = new PasswordResetCode("000000");
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
    @DisplayName("Should validate PasswordResetCode format and PasswordResetToken invariants")
    void shouldValidateResetValueObjects() {
        var now = Instant.now();

        assertThat(new PasswordResetCode(" 012345 ").value()).isEqualTo("012345");
        assertThatThrownBy(() -> new PasswordResetCode("12345"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.reset_token.format");
        assertThatThrownBy(() -> new PasswordResetCode("abcdef"))
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

    private static final PasswordResetCode CODE = new PasswordResetCode("482913");

    /** Deterministic stand-in for the Argon2 adapter: the domain only needs hash/matches semantics. */
    private static final PasswordResetCodeHasher HASHER = new PasswordResetCodeHasher() {
        @Override
        public String hash(PasswordResetCode code) {
            return "hashed:" + code.value();
        }

        @Override
        public boolean matches(PasswordResetCode code, String codeHash) {
            return hash(code).equals(codeHash);
        }
    };

    private UserAccount accountWithActiveCode() {
        var account = UserAccount.register(email, hash, role);
        account.requestPasswordReset(HASHER.hash(CODE), Instant.now());
        account.clearDomainEvents();
        return account;
    }
}
