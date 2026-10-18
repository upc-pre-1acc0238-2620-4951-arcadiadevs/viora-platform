package com.arcadiadevs.viora.platform.iam.domain.model.aggregates;

import com.arcadiadevs.viora.platform.iam.domain.exceptions.EmailVerificationCooldownException;
import com.arcadiadevs.viora.platform.iam.domain.exceptions.InvalidEmailVerificationCodeException;
import com.arcadiadevs.viora.platform.iam.domain.model.events.EmailVerificationRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordChangedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetCompletedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountActivatedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountRegisteredEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserSignedUpWithProfileEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.VerificationCodeHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailVerificationToken;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.OnboardingProfileDetails;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.VerificationCode;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordResetToken;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TermsAcceptance;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserAccountStatus;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

import java.time.Instant;

/**
 * Pure Domain Aggregate Root representing a security subject and access account.
 *
 * <p>Enforces identity assignment, the email verification lifecycle ({@link UserAccountStatus}), password update
 * invariants, and recovery code lifecycles. An account is born pending and can only sign in once its email address
 * is verified. Contains zero JPA, framework, or presentation annotations.</p>
 */
public class UserAccount extends AbstractDomainAggregateRoot<UserAccount> {

    private final UserId id;
    private final EmailAddress email;
    private PasswordHash passwordHash;
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
        var account = new UserAccount(id, email, passwordHash, role, UserAccountStatus.PENDING_VERIFICATION,
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
     * @throws BusinessRuleException    if the email address is not verified yet
     */
    public void requestPasswordReset(String codeHash, Instant now) {
        if (this.status != UserAccountStatus.ACTIVE) {
            throw new BusinessRuleException("user.email.not_verified");
        }
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
    public void resetPassword(VerificationCode code, VerificationCodeHasher hasher, PasswordHash newPasswordHash, Instant now) {
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
