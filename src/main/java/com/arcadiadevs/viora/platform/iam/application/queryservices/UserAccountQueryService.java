package com.arcadiadevs.viora.platform.iam.application.queryservices;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.CheckPasswordResetTokenQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.CheckUserAccountExistsByEmailQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByEmailQuery;
import com.arcadiadevs.viora.platform.iam.domain.model.queries.GetUserAccountByIdQuery;

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
     * Checks whether a password reset link can still be used, without consuming it.
     *
     * @param query the query carrying the token of the link
     * @return true if the link is the active one and has not expired; false otherwise
     */
    boolean handle(CheckPasswordResetTokenQuery query);
}
