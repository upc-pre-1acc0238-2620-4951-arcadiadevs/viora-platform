package com.arcadiadevs.viora.platform.iam.domain.model.aggregates;

import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordChangedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetCompletedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountRegisteredEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordResetToken;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

import java.time.Instant;

/**
 * Pure Domain Aggregate Root representing a security subject and access account.
 *
 * <p>Enforces identity assignment, password update invariants, and cryptographic reset token lifecycles.
 * Contains zero JPA, framework, or presentation annotations.</p>
 */
public class UserAccount extends AbstractDomainAggregateRoot<UserAccount> {

    private final UserId id;
    private final EmailAddress email;
    private PasswordHash passwordHash;
    private final UserRole role;
    private PasswordResetToken resetToken;
    private final Instant createdAt;
    private Instant updatedAt;

    private UserAccount(
            UserId id,
            EmailAddress email,
            PasswordHash passwordHash,
            UserRole role,
            PasswordResetToken resetToken,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.resetToken = resetToken;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method registering a new UserAccount aggregate.
     *
     * @param email normalized email address
     * @param passwordHash derived password hash
     * @param role assigned platform role
     * @return newly instantiated UserAccount
     */
    public static UserAccount register(EmailAddress email, PasswordHash passwordHash, UserRole role) {
        if (email == null) {
            throw new IllegalArgumentException("user.email.null");
        }
        if (passwordHash == null) {
            throw new IllegalArgumentException("user.password_hash.null");
        }
        if (role == null) {
            throw new IllegalArgumentException("user.role.null");
        }

        var id = new UserId();
        var now = Instant.now();
        var account = new UserAccount(id, email, passwordHash, role, null, now, now);
        account.registerDomainEvent(new UserAccountRegisteredEvent(id.userId(), email.value(), role.name(), now));
        return account;
    }

    /**
     * Reconstitutes an existing aggregate root from its persistence snapshot.
     *
     * @param snapshot immutable snapshot
     * @return reconstituted UserAccount aggregate
     */
    public static UserAccount reconstitute(UserAccountSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("user.snapshot.null");
        }
        return new UserAccount(
                snapshot.id(),
                snapshot.email(),
                snapshot.passwordHash(),
                snapshot.role(),
                snapshot.resetToken(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }

    /**
     * Modifies the account password.
     *
     * @param newPasswordHash new hashed password
     */
    public void changePassword(PasswordHash newPasswordHash) {
        if (newPasswordHash == null) {
            throw new IllegalArgumentException("user.password_hash.null");
        }
        this.passwordHash = newPasswordHash;
        this.resetToken = null;
        this.updatedAt = Instant.now();
        registerDomainEvent(new PasswordChangedEvent(this.id.userId(), this.updatedAt));
    }

    /**
     * Issues an ephemeral password reset token and registers {@link PasswordResetRequestedEvent}.
     *
     * @return newly generated {@link PasswordResetToken}
     */
    public PasswordResetToken requestPasswordReset() {
        var token = PasswordResetToken.issue();
        this.resetToken = token;
        this.updatedAt = Instant.now();
        registerDomainEvent(new PasswordResetRequestedEvent(
                this.id.userId(),
                this.email.value(),
                token.token(),
                token.expiresAt(),
                this.updatedAt
        ));
        return token;
    }

    /**
     * Consumes a valid recovery token to reset the password.
     *
     * @param tokenString incoming plain token string
     * @param newPasswordHash new hashed password
     * @param now current instant for expiry verification
     * @throws BusinessRuleException if the token is missing, mismatched, or expired
     */
    public void resetPassword(String tokenString, PasswordHash newPasswordHash, Instant now) {
        if (tokenString == null || tokenString.isBlank()) {
            throw new IllegalArgumentException("user.reset_token.null_or_empty");
        }
        if (newPasswordHash == null) {
            throw new IllegalArgumentException("user.password_hash.null");
        }
        if (now == null) {
            throw new IllegalArgumentException("user.clock.now.null");
        }

        if (this.resetToken == null) {
            throw new BusinessRuleException("user.password_reset.no_active_token");
        }
        if (!this.resetToken.token().equals(tokenString.trim())) {
            throw new BusinessRuleException("user.password_reset.invalid_token");
        }
        if (this.resetToken.isExpired(now)) {
            throw new BusinessRuleException("user.password_reset.token_expired");
        }

        this.passwordHash = newPasswordHash;
        this.resetToken = null;
        this.updatedAt = now;
        registerDomainEvent(new PasswordResetCompletedEvent(this.id.userId(), now));
    }

    /**
     * Exposes the current state as an immutable snapshot.
     *
     * @return UserAccountSnapshot instance
     */
    public UserAccountSnapshot snapshot() {
        return new UserAccountSnapshot(
                this.id,
                this.email,
                this.passwordHash,
                this.role,
                this.resetToken,
                this.createdAt,
                this.updatedAt
        );
    }
}
