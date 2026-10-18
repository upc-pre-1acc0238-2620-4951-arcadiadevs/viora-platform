package com.arcadiadevs.viora.platform.iam.domain.repositories;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;

import java.util.Optional;

/**
 * Pure Domain Repository port for persisting and retrieving {@link UserAccount} aggregates.
 *
 * <p>Contains zero Spring Data or infrastructure dependencies.</p>
 */
public interface UserAccountRepository {

    /**
     * Persists the current state of a user account.
     *
     * @param userAccount aggregate to save
     * @return persisted aggregate
     */
    UserAccount save(UserAccount userAccount);

    /**
     * Finds a user account by its unique identifier.
     *
     * @param id user identifier VO
     * @return Optional containing the aggregate if found
     */
    Optional<UserAccount> findById(UserId id);

    /**
     * Finds a user account by its normalized email address.
     *
     * @param email normalized email VO
     * @return Optional containing the aggregate if found
     */
    Optional<UserAccount> findByEmail(EmailAddress email);

    /**
     * Checks if a user account already exists with the given email.
     *
     * @param email normalized email VO
     * @return true if an account exists; false otherwise
     */
    boolean existsByEmail(EmailAddress email);

    /**
     * Finds the account whose current password reset link carries the given token hash. It does not check whether
     * the link is still usable: that is decided by {@link UserAccount#canResetPasswordWith}.
     *
     * @param resetTokenHash hash of the token taken from the reset link
     * @return Optional containing the aggregate if found
     */
    Optional<UserAccount> findByResetTokenHash(TokenHash resetTokenHash);
}
