package com.arcadiadevs.viora.platform.iam.domain.model.aggregates;

import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordChangedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetCompletedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountRegisteredEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserSignedUpWithProfileEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.PasswordResetCodeHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.OnboardingProfileDetails;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordResetCode;
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
        return register(email, passwordHash, role, null);
    }

    /**
     * Factory method registering a new UserAccount aggregate together with optional onboarding profile details.
     *
     * <p>When details are given it also raises {@link UserSignedUpWithProfileEvent} so the Profiles context can
     * create the profile. The details are not kept in the aggregate.</p>
     *
     * @param email                    normalized email address
     * @param passwordHash             derived password hash
     * @param role                     assigned platform role
     * @param onboardingProfileDetails profile details submitted at sign-up, or null when there are none
     * @return newly instantiated UserAccount
     * @throws IllegalArgumentException if email, password hash or role is null
     */
    public static UserAccount register(
            EmailAddress email,
            PasswordHash passwordHash,
            UserRole role,
            OnboardingProfileDetails onboardingProfileDetails
    ) {
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
        if (onboardingProfileDetails != null) {
            account.registerDomainEvent(new UserSignedUpWithProfileEvent(
                    id.userId(),
                    onboardingProfileDetails.fullName(),
                    onboardingProfileDetails.country(),
                    onboardingProfileDetails.phoneNumber(),
                    now
            ));
        }
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
     * Stores the hash of a freshly issued recovery code, replacing any previous one, and registers
     * {@link PasswordResetRequestedEvent}.
     *
     * @param codeHash the hash of the issued recovery code
     * @param now      the instant the code is issued
     * @throws IllegalArgumentException if the hash is missing or {@code now} is null
     */
    public void requestPasswordReset(String codeHash, Instant now) {
        var token = PasswordResetToken.issue(codeHash, now);
        this.resetToken = token;
        this.updatedAt = now;
        registerDomainEvent(new PasswordResetRequestedEvent(
                this.id.userId(),
                this.email.value(),
                token.expiresAt(),
                now
        ));
    }

    /**
     * Consumes a valid recovery code to reset the password.
     *
     * <p>A wrong code is recorded on the token before the exception is thrown, so the caller must persist the
     * aggregate even when this method fails. After {@link PasswordResetToken#MAX_FAILED_ATTEMPTS} wrong codes the
     * token is locked until a new code is requested.</p>
     *
     * @param code            the code submitted by the user
     * @param hasher          the port verifying the code against the stored hash
     * @param newPasswordHash new hashed password
     * @param now             current instant for expiry verification
     * @throws IllegalArgumentException if an argument is missing
     * @throws BusinessRuleException    if there is no active code, it expired, it is locked, or it does not match
     */
    public void resetPassword(PasswordResetCode code, PasswordResetCodeHasher hasher, PasswordHash newPasswordHash, Instant now) {
        if (code == null) {
            throw new IllegalArgumentException("user.reset_token.null_or_empty");
        }
        if (hasher == null) {
            throw new IllegalArgumentException("user.reset_code_hasher.null");
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
        if (this.resetToken.isExpired(now)) {
            throw new BusinessRuleException("user.password_reset.token_expired");
        }
        if (this.resetToken.hasExhaustedAttempts()) {
            throw new BusinessRuleException("user.password_reset.attempts_exceeded");
        }
        if (!hasher.matches(code, this.resetToken.codeHash())) {
            this.resetToken = this.resetToken.withFailedAttempt();
            this.updatedAt = now;
            throw new BusinessRuleException("user.password_reset.invalid_token");
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
