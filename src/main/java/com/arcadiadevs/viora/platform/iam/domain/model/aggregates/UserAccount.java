package com.arcadiadevs.viora.platform.iam.domain.model.aggregates;

import com.arcadiadevs.viora.platform.iam.domain.exceptions.EmailVerificationCooldownException;
import com.arcadiadevs.viora.platform.iam.domain.exceptions.InvalidEmailVerificationCodeException;
import com.arcadiadevs.viora.platform.iam.domain.model.events.EmailVerificationRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordChangedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetCompletedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UnverifiedUserAccountDiscardedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountActivatedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountRegisteredEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserSignedUpWithProfileEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.PasswordHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.VerificationCodeHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailVerificationToken;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.NewPassword;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.OnboardingProfileDetails;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHistory;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.VerificationCode;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordResetToken;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TermsAcceptance;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserAccountStatus;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

import java.time.Duration;
import java.time.Instant;

/**
 * Pure Domain Aggregate Root representing a security subject and access account.
 *
 * <p>Enforces identity assignment, the email verification lifecycle ({@link UserAccountStatus}), password update
 * invariants, and recovery code lifecycles. An account is born pending and can only sign in once its email address
 * is verified. A new password cannot repeat the last {@link PasswordHistory#REMEMBERED_PASSWORDS} ones, and every
 * password change raises the credentials version, which invalidates the sessions issued before it. Contains zero
 * JPA, framework, or presentation annotations.</p>
 */
public class UserAccount extends AbstractDomainAggregateRoot<UserAccount> {

    /** How long a pending account is kept without any sign-up activity before it is discarded. */
    public static final Duration UNVERIFIED_RETENTION = Duration.ofDays(7);

    private final UserId id;
    private final EmailAddress email;
    private PasswordHash passwordHash;
    private PasswordHistory passwordHistory;
    private long credentialsVersion;
    private final UserRole role;
    private UserAccountStatus status;
    private EmailVerificationToken emailVerification;
    private TermsAcceptance termsAcceptance;
    private PasswordResetToken resetToken;
    private final Instant createdAt;
    private Instant updatedAt;

    private UserAccount(
            UserId id,
            EmailAddress email,
            PasswordHash passwordHash,
            PasswordHistory passwordHistory,
            long credentialsVersion,
            UserRole role,
            UserAccountStatus status,
            EmailVerificationToken emailVerification,
            TermsAcceptance termsAcceptance,
            PasswordResetToken resetToken,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.passwordHistory = passwordHistory;
        this.credentialsVersion = credentialsVersion;
        this.role = role;
        this.status = status;
        this.emailVerification = emailVerification;
        this.termsAcceptance = termsAcceptance;
        this.resetToken = resetToken;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * Factory method signing up a new account, pending until its email address is verified.
     *
     * <p>Raises {@link UserAccountRegisteredEvent}, {@link UserSignedUpWithProfileEvent} when profile details are
     * given (the details are not kept in the aggregate), and {@link EmailVerificationRequestedEvent}.</p>
     *
     * @param email                    normalized email address
     * @param passwordHash             derived password hash
     * @param role                     requested role; only {@link UserRole#ROLE_PRODUCER} can sign up
     * @param termsAcceptance          the accepted terms version and instant
     * @param onboardingProfileDetails profile details submitted at sign-up, or null when there are none
     * @param verificationCodeHash     hash of the email verification code being sent
     * @param now                      the sign-up instant
     * @return the new pending account
     * @throws IllegalArgumentException if a required argument is missing, the terms were not accepted, or the role
     *                                  cannot sign up
     */
    public static UserAccount signUp(
            EmailAddress email,
            PasswordHash passwordHash,
            UserRole role,
            TermsAcceptance termsAcceptance,
            OnboardingProfileDetails onboardingProfileDetails,
            String verificationCodeHash,
            Instant now
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
        if (role != UserRole.ROLE_PRODUCER) {
            throw new IllegalArgumentException("user.role.sign_up_not_allowed");
        }
        if (termsAcceptance == null) {
            throw new IllegalArgumentException("user.terms.not_accepted");
        }
        if (now == null) {
            throw new IllegalArgumentException("user.clock.now.null");
        }

        var id = new UserId();
        var verification = EmailVerificationToken.issue(verificationCodeHash, now);
        var account = new UserAccount(id, email, passwordHash, PasswordHistory.empty(), 0L, role,
                UserAccountStatus.PENDING_VERIFICATION,
                verification, termsAcceptance, null, now, now);
        account.registerDomainEvent(new UserAccountRegisteredEvent(id.userId(), email.value(), role.name(), now));
        account.registerSignUpDetails(onboardingProfileDetails, now);
        account.registerDomainEvent(new EmailVerificationRequestedEvent(id.userId(), email.value(),
                verification.expiresAt(), now));
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
                snapshot.passwordHistory(),
                snapshot.credentialsVersion(),
                snapshot.role(),
                snapshot.status(),
                snapshot.emailVerification(),
                snapshot.termsAcceptance(),
                snapshot.resetToken(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }

    /**
     * Signs up again over a pending account that was never verified, keeping its identity.
     *
     * <p>Replaces the password, the accepted terms and the verification code, and raises the sign-up events again so
     * the profile is updated. This is how a pending registration is retried, so nobody can hold an email address by
     * registering it without verifying it.</p>
     *
     * @param newPasswordHash          derived password hash
     * @param newTermsAcceptance       the accepted terms version and instant
     * @param onboardingProfileDetails profile details submitted at sign-up, or null when there are none
     * @param verificationCodeHash     hash of the new email verification code
     * @param now                      the sign-up instant
     * @throws IllegalArgumentException            if a required argument is missing or the terms were not accepted
     * @throws BusinessRuleException               if the account is already active
     * @throws EmailVerificationCooldownException if the previous code was sent less than 45 seconds ago
     */
    public void restartSignUp(
            PasswordHash newPasswordHash,
            TermsAcceptance newTermsAcceptance,
            OnboardingProfileDetails onboardingProfileDetails,
            String verificationCodeHash,
            Instant now
    ) {
        if (newPasswordHash == null) {
            throw new IllegalArgumentException("user.password_hash.null");
        }
        if (newTermsAcceptance == null) {
            throw new IllegalArgumentException("user.terms.not_accepted");
        }
        ensurePendingVerification("user.email.already_exists");
        ensureResendCooldownElapsed(now);

        this.passwordHash = newPasswordHash;
        this.termsAcceptance = newTermsAcceptance;
        this.updatedAt = now;
        registerSignUpDetails(onboardingProfileDetails, now);
        issueEmailVerification(verificationCodeHash, now);
    }

    /**
     * Issues a new email verification code for a pending account, replacing the previous one.
     *
     * @param verificationCodeHash hash of the new verification code
     * @param now                  the instant the code is issued
     * @throws IllegalArgumentException            if an argument is missing
     * @throws BusinessRuleException               if the account is already active
     * @throws EmailVerificationCooldownException if the previous code was sent less than 45 seconds ago
     */
    public void resendEmailVerification(String verificationCodeHash, Instant now) {
        ensurePendingVerification("user.email.already_verified");
        ensureResendCooldownElapsed(now);
        this.updatedAt = now;
        issueEmailVerification(verificationCodeHash, now);
    }

    /**
     * Verifies the email address with the submitted code and activates the account.
     *
     * <p>A wrong code is recorded on the token before the exception is thrown, so the caller must persist the
     * aggregate even when this method fails. After {@link EmailVerificationToken#MAX_FAILED_ATTEMPTS} wrong codes the
     * token is locked until a new code is sent.</p>
     *
     * @param code   the code submitted by the user
     * @param hasher the port verifying the code against the stored hash
     * @param now    current instant for expiry verification
     * @throws IllegalArgumentException              if an argument is missing
     * @throws BusinessRuleException                 if the account is already active, or there is no active code,
     *                                               it expired, or it is locked
     * @throws InvalidEmailVerificationCodeException if the code does not match
     */
    public void verifyEmail(VerificationCode code, VerificationCodeHasher hasher, Instant now) {
        if (code == null) {
            throw new IllegalArgumentException("user.email_verification.code.null");
        }
        if (hasher == null) {
            throw new IllegalArgumentException("user.verification_code_hasher.null");
        }
        if (now == null) {
            throw new IllegalArgumentException("user.clock.now.null");
        }
        ensurePendingVerification("user.email.already_verified");

        if (this.emailVerification == null) {
            throw new BusinessRuleException("user.email_verification.no_active_code");
        }
        if (this.emailVerification.isExpired(now)) {
            throw new BusinessRuleException("user.email_verification.code_expired");
        }
        if (this.emailVerification.hasExhaustedAttempts()) {
            throw new BusinessRuleException("user.email_verification.attempts_exceeded");
        }
        if (!hasher.matches(code, this.emailVerification.codeHash())) {
            this.emailVerification = this.emailVerification.withFailedAttempt();
            this.updatedAt = now;
            throw new InvalidEmailVerificationCodeException(this.emailVerification.remainingAttempts());
        }

        this.status = UserAccountStatus.ACTIVE;
        this.emailVerification = null;
        this.updatedAt = now;
        registerDomainEvent(new UserAccountActivatedEvent(this.id.userId(), this.email.value(), now));
    }

    /**
     * Checks whether the account is pending and had no sign-up activity (sign-up, retried sign-up or resent code)
     * for {@link #UNVERIFIED_RETENTION}.
     *
     * @param now current instant
     * @return true if the account was abandoned before verifying its email
     */
    public boolean isAbandonedUnverified(Instant now) {
        return now != null && this.status == UserAccountStatus.PENDING_VERIFICATION
                && this.updatedAt.plus(UNVERIFIED_RETENTION).isBefore(now);
    }

    /**
     * Marks an abandoned pending account for removal and registers {@link UnverifiedUserAccountDiscardedEvent}, so
     * the contexts that reacted to its sign-up can remove what they created. The repository deletes it afterwards.
     *
     * @param now the instant of the discard
     * @throws BusinessRuleException if the account is active or still within {@link #UNVERIFIED_RETENTION}
     */
    public void discardUnverified(Instant now) {
        if (!isAbandonedUnverified(now)) {
            throw new BusinessRuleException("user.account.not_discardable");
        }
        registerDomainEvent(new UnverifiedUserAccountDiscardedEvent(this.id.userId(), this.email.value(), now));
    }

    /**
     * Confirms that the owner wants to delete this account for good and registers {@link UserAccountDeletedEvent}, so
     * every context removes the data it keeps for the account. The repository deletes it afterwards.
     *
     * @param password the current plain password, typed again to confirm
     * @param hasher   the port checking passwords
     * @param now      the instant of the deletion
     * @throws IllegalArgumentException if an argument is missing
     * @throws BusinessRuleException    if the account is not active or the password is wrong
     */
    public void delete(String password, PasswordHasher hasher, Instant now) {
        if (password == null) {
            throw new IllegalArgumentException("user.password.current.null");
        }
        if (hasher == null) {
            throw new IllegalArgumentException("user.password_hasher.null");
        }
        if (now == null) {
            throw new IllegalArgumentException("user.clock.now.null");
        }
        if (this.status != UserAccountStatus.ACTIVE) {
            throw new BusinessRuleException("user.email.not_verified");
        }
        if (!hasher.matches(password, this.passwordHash)) {
            throw new BusinessRuleException("user.password.current_invalid");
        }
        registerDomainEvent(new UserAccountDeletedEvent(this.id.userId(), this.email.value(), now));
    }

    /**
     * Changes the password of an active account, after checking the current one.
     *
     * <p>The new password cannot repeat any of the last {@link PasswordHistory#REMEMBERED_PASSWORDS} passwords. The
     * change raises the credentials version, so the sessions issued before it stop working.</p>
     *
     * @param currentPassword the current plain password, as typed by the user
     * @param newPassword     the new password, already checked against the password rules
     * @param hasher          the port hashing and checking passwords
     * @param now             the instant of the change
     * @throws IllegalArgumentException if an argument is missing
     * @throws BusinessRuleException    if the account is not active, the current password is wrong, or the new one
     *                                  was used recently
     */
    public void changePassword(String currentPassword, NewPassword newPassword, PasswordHasher hasher, Instant now) {
        if (currentPassword == null) {
            throw new IllegalArgumentException("user.password.current.null");
        }
        if (newPassword == null) {
            throw new IllegalArgumentException("user.password.null");
        }
        if (hasher == null) {
            throw new IllegalArgumentException("user.password_hasher.null");
        }
        if (now == null) {
            throw new IllegalArgumentException("user.clock.now.null");
        }
        if (this.status != UserAccountStatus.ACTIVE) {
            throw new BusinessRuleException("user.email.not_verified");
        }
        if (!hasher.matches(currentPassword, this.passwordHash)) {
            throw new BusinessRuleException("user.password.current_invalid");
        }

        replacePassword(newPassword, hasher, now);
        registerDomainEvent(new PasswordChangedEvent(this.id.userId(), now));
    }

    /**
     * Stores the hash of the token of a freshly issued password reset link, replacing any previous link, and
     * registers {@link PasswordResetRequestedEvent}.
     *
     * @param tokenHash the hash of the token carried by the link
     * @param now       the instant the link is issued
     * @throws IllegalArgumentException if the hash is missing or {@code now} is null
     * @throws BusinessRuleException    if the email address is not verified yet
     */
    public void requestPasswordReset(TokenHash tokenHash, Instant now) {
        if (this.status != UserAccountStatus.ACTIVE) {
            throw new BusinessRuleException("user.email.not_verified");
        }
        var token = PasswordResetToken.issue(tokenHash, now);
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
     * Checks whether a password reset link can still be used, without consuming it.
     *
     * @param tokenHash hash of the token taken from the link
     * @param now       current instant
     * @return true if the link is the active one and has not expired
     */
    public boolean canResetPasswordWith(TokenHash tokenHash, Instant now) {
        return now != null && this.resetToken != null && this.resetToken.matches(tokenHash)
                && this.resetToken.isUsableAt(now);
    }

    /**
     * Consumes the password reset link to set a new password. The link can be used only once.
     *
     * <p>The new password cannot repeat any of the last {@link PasswordHistory#REMEMBERED_PASSWORDS} passwords; a
     * repeated one is rejected without consuming the link. The reset raises the credentials version, so the
     * sessions issued before it stop working.</p>
     *
     * @param tokenHash      hash of the token taken from the link
     * @param newPassword    the new password, already checked against the password rules
     * @param passwordHasher the port hashing and checking passwords
     * @param now            the instant of the reset
     * @throws IllegalArgumentException if an argument is missing
     * @throws BusinessRuleException    if the link is not the active one, expired or was already used, or the new
     *                                  password was used recently
     */
    public void resetPassword(TokenHash tokenHash, NewPassword newPassword, PasswordHasher passwordHasher, Instant now) {
        if (newPassword == null) {
            throw new IllegalArgumentException("user.password.null");
        }
        if (passwordHasher == null) {
            throw new IllegalArgumentException("user.password_hasher.null");
        }
        if (now == null) {
            throw new IllegalArgumentException("user.clock.now.null");
        }
        if (!canResetPasswordWith(tokenHash, now)) {
            throw new BusinessRuleException("user.password_reset.link_expired");
        }

        replacePassword(newPassword, passwordHasher, now);
        registerDomainEvent(new PasswordResetCompletedEvent(this.id.userId(), now));
    }

    private void replacePassword(NewPassword newPassword, PasswordHasher hasher, Instant now) {
        var reused = hasher.matches(newPassword.value(), this.passwordHash)
                || this.passwordHistory.previousHashes().stream()
                .anyMatch(previous -> hasher.matches(newPassword.value(), previous));
        if (reused) {
            throw new BusinessRuleException("user.password.reused");
        }
        this.passwordHistory = this.passwordHistory.remember(this.passwordHash);
        this.passwordHash = hasher.hash(newPassword);
        this.credentialsVersion++;
        this.resetToken = null;
        this.updatedAt = now;
    }

    private void ensurePendingVerification(String violationKey) {
        if (this.status != UserAccountStatus.PENDING_VERIFICATION) {
            throw new BusinessRuleException(violationKey);
        }
    }

    private void ensureResendCooldownElapsed(Instant now) {
        if (now == null) {
            throw new IllegalArgumentException("user.clock.now.null");
        }
        if (this.emailVerification == null) {
            return;
        }
        var remaining = this.emailVerification.remainingCooldown(now);
        if (!remaining.isZero()) {
            throw new EmailVerificationCooldownException(remaining);
        }
    }

    private void issueEmailVerification(String verificationCodeHash, Instant now) {
        this.emailVerification = EmailVerificationToken.issue(verificationCodeHash, now);
        registerDomainEvent(new EmailVerificationRequestedEvent(this.id.userId(), this.email.value(),
                this.emailVerification.expiresAt(), now));
    }

    private void registerSignUpDetails(OnboardingProfileDetails onboardingProfileDetails, Instant now) {
        if (onboardingProfileDetails == null) {
            return;
        }
        registerDomainEvent(new UserSignedUpWithProfileEvent(
                this.id.userId(),
                onboardingProfileDetails.fullName(),
                onboardingProfileDetails.country(),
                onboardingProfileDetails.phoneNumber(),
                now
        ));
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
                this.passwordHistory,
                this.credentialsVersion,
                this.role,
                this.status,
                this.emailVerification,
                this.termsAcceptance,
                this.resetToken,
                this.createdAt,
                this.updatedAt
        );
    }
}
