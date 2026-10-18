package com.arcadiadevs.viora.platform.iam.domain.model;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccountSnapshot;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailVerificationToken;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TermsAcceptance;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserAccountStatus;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;

import java.time.Instant;

/** Builds user accounts in a given state for tests, as persistence would reconstitute them. */
public final class UserAccountFixtures {

    private UserAccountFixtures() {
    }

    /**
     * Builds an account whose email is already verified.
     *
     * @param email        the account email
     * @param passwordHash the stored password hash
     * @param role         the account role
     * @return an active account with no pending events
     */
    public static UserAccount activeAccount(String email, String passwordHash, UserRole role) {
        var now = Instant.now();
        return UserAccount.reconstitute(new UserAccountSnapshot(
                new UserId(),
                new EmailAddress(email),
                new PasswordHash(passwordHash),
                role,
                UserAccountStatus.ACTIVE,
                null,
                new TermsAcceptance("2026-10", now),
                null,
                now,
                now
        ));
    }

    /**
     * Builds an account that signed up but has not verified its email yet.
     *
     * @param email        the account email
     * @param passwordHash the stored password hash
     * @return a pending producer account whose verification code was issued now
     */
    public static UserAccount pendingAccount(String email, String passwordHash) {
        var now = Instant.now();
        return UserAccount.reconstitute(new UserAccountSnapshot(
                new UserId(),
                new EmailAddress(email),
                new PasswordHash(passwordHash),
                UserRole.ROLE_PRODUCER,
                UserAccountStatus.PENDING_VERIFICATION,
                EmailVerificationToken.issue("hashed:482913", now),
                new TermsAcceptance("2026-10", now),
                null,
                now,
                now
        ));
    }
}
