package com.arcadiadevs.viora.platform.iam.domain.model;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordChangedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetCompletedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountRegisteredEvent;
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
    @DisplayName("Should successfully request reset token and emit PasswordResetRequestedEvent")
    void shouldRequestPasswordReset() {
        var account = UserAccount.register(email, hash, role);
        account.clearDomainEvents();

        var token = account.requestPasswordReset();

        assertThat(token).isNotNull();
        assertThat(token.token()).isNotBlank();
        assertThat(account.snapshot().resetToken()).isNotNull();

        assertThat(account.domainEvents()).hasSize(1);
        var event = (PasswordResetRequestedEvent) account.domainEvents().iterator().next();
        assertThat(event.resetToken()).isEqualTo(token.token());
        assertThat(event.email()).isEqualTo(email.value());
    }

    @Test
    @DisplayName("Should successfully reset password with valid token and reject expired token")
    void shouldResetPasswordWithValidToken() {
        var account = UserAccount.register(email, hash, role);
        var token = account.requestPasswordReset();
        account.clearDomainEvents();

        var newHash = new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$resetpasswordhash");
        account.resetPassword(token.token(), newHash, Instant.now());

        assertThat(account.snapshot().passwordHash().value()).isEqualTo(newHash.value());
        assertThat(account.snapshot().resetToken()).isNull();
        assertThat(account.domainEvents()).hasSize(1);
        assertThat(account.domainEvents().iterator().next()).isInstanceOf(PasswordResetCompletedEvent.class);
    }

    @Test
    @DisplayName("Should reject expired password reset token")
    void shouldRejectExpiredResetToken() {
        var account = UserAccount.register(email, hash, role);
        var token = account.requestPasswordReset();

        var newHash = new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$resetpasswordhash");
        Instant future = Instant.now().plus(20, ChronoUnit.MINUTES);

        assertThatThrownBy(() -> account.resetPassword(token.token(), newHash, future))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("user.password_reset.token_expired");
    }

    @Test
    @DisplayName("Should create PasswordResetToken via lookup constructor and support query instantiation")
    void shouldCreatePasswordResetTokenAndQuery() {
        var tokenVo = new PasswordResetToken("sample-lookup-token");
        assertThat(tokenVo.token()).isEqualTo("sample-lookup-token");
        assertThat(tokenVo.expiresAt()).isEqualTo(Instant.MAX);
        assertThat(tokenVo.isExpired(Instant.now())).isFalse();

        var queryFromVo = new com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByResetTokenQuery(tokenVo);
        assertThat(queryFromVo.token()).isEqualTo(tokenVo);

        var queryFromRaw = new com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByResetTokenQuery("raw-lookup-token");
        assertThat(queryFromRaw.token().token()).isEqualTo("raw-lookup-token");

        assertThatThrownBy(() -> new com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByResetTokenQuery((PasswordResetToken) null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.reset_token.null");
    }
}
