package com.arcadiadevs.viora.platform.iam.domain.model;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccountSnapshot;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailVerificationToken;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHistory;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TermsAcceptance;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
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
                PasswordHistory.empty(),
                0L,
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
                PasswordHistory.empty(),
                0L,
                UserRole.ROLE_PRODUCER,
                UserAccountStatus.PENDING_VERIFICATION,
                EmailVerificationToken.issue("hashed:482913", now),
                new TermsAcceptance("2026-10", now),
                null,
                now,
                now
        ));
    }

    /**
     * Returns the SHA-256 token hash of a plain test token, as the real token generator would.
     *
     * @param token the plain token
     * @return its hash
     */
    public static TokenHash tokenHashOf(String token) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return new TokenHash(java.util.HexFormat.of().formatHex(digest));
        } catch (java.security.NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
