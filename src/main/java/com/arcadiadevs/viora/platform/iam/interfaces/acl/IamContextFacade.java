package com.arcadiadevs.viora.platform.iam.interfaces.acl;

import java.util.Optional;

/**
 * Open Host Service (OHS) facade exposed by IAM to other Bounded Contexts.
 *
 * <p>Exclusively exposes Java standard and primitive types to avoid leaking internal domain concepts.</p>
 */
public interface IamContextFacade {

    /**
     * Checks if a user account exists, whether or not its email is verified yet.
     *
     * <p>Meant for what happens during sign-up itself, such as creating the profile, while the account is still
     * pending verification.</p>
     *
     * @param userId user account identifier string
     * @return true if the account exists; false otherwise
     */
    boolean existsUser(String userId);

    /**
     * Checks if a user account exists and is active, that is, its email address is verified.
     *
     * @param userId user account identifier string
     * @return true if the account exists and is active; false otherwise
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
