package com.arcadiadevs.viora.platform.iam.domain.model.aggregates;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.*;

import java.time.Instant;

/**
 * Immutable state snapshot of a {@link UserAccount} aggregate root.
 *
 * @param id user identifier VO
 * @param email email address VO
 * @param passwordHash hashed password VO
 * @param role assigned role VO
 * @param status lifecycle status of the account
 * @param emailVerification active email verification code VO (null once verified)
 * @param termsAcceptance accepted terms VO (null for accounts created before terms were recorded)
 * @param resetToken optional active reset token VO (may be null)
 * @param createdAt creation timestamp
 * @param updatedAt last modification timestamp
 */
public record UserAccountSnapshot(
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
    public UserAccountSnapshot {
        if (id == null) {
            throw new IllegalArgumentException("user.snapshot.id.null");
        }
        if (email == null) {
            throw new IllegalArgumentException("user.snapshot.email.null");
        }
        if (passwordHash == null) {
            throw new IllegalArgumentException("user.snapshot.password_hash.null");
        }
        if (role == null) {
            throw new IllegalArgumentException("user.snapshot.role.null");
        }
        if (status == null) {
            throw new IllegalArgumentException("user.snapshot.status.null");
        }
    }
}
