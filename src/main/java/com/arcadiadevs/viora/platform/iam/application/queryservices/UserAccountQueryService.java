package com.arcadiadevs.viora.platform.iam.application.queryservices;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.CheckUserAccountExistsByEmailQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByEmailQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByIdQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByResetTokenQuery;

import java.util.Optional;

/**
 * Application query service port for read-only queries on user accounts.
 * Dispatches domain queries via uniform {@code handle} methods.
 */
public interface UserAccountQueryService {

    /**
     * Resolves a user account by its primary identifier query.
     *
     * @param query the query containing the user identifier VO
     * @return Optional containing the aggregate if found
     */
    Optional<UserAccount> handle(GetUserAccountByIdQuery query);

    /**
     * Resolves a user account by its normalized email query.
     *
     * @param query the query containing the email VO
     * @return Optional containing the aggregate if found
     */
    Optional<UserAccount> handle(GetUserAccountByEmailQuery query);

    /**
     * Checks if an email is already registered via query.
     *
     * @param query the check email existence query
     * @return true if registered; false otherwise
     */
    boolean handle(CheckUserAccountExistsByEmailQuery query);

    /**
     * Finds a user account matching an active reset token query.
     *
     * @param query the reset token query
     * @return Optional containing the aggregate if found
     */
    Optional<UserAccount> handle(GetUserAccountByResetTokenQuery query);
}
