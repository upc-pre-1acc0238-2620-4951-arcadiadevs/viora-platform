package com.arcadiadevs.viora.platform.iam.interfaces.acl;

import java.util.Optional;

/**
 * Open Host Service (OHS) facade exposed by IAM to other Bounded Contexts.
 *
 * <p>Exclusively exposes Java standard and primitive types to avoid leaking internal domain concepts.</p>
 */
public interface IamContextFacade {

    /**
     * Checks if a user exists and is active.
     *
     * @param userId user account identifier string
     * @return true if account exists; false otherwise
     */
    boolean existsActiveUser(String userId);

    /**
     * Resolves the assigned role of a user.
     *
     * @param userId user account identifier string
     * @return Optional containing the role name if user exists
     */
    Optional<String> findUserRole(String userId);

    /**
     * Resolves the registered email address of a user.
     *
     * @param userId user account identifier string
     * @return Optional containing the email address if user exists
     */
    Optional<String> findUserEmail(String userId);
}
