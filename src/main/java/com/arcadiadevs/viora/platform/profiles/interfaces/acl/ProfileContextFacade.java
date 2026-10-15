package com.arcadiadevs.viora.platform.profiles.interfaces.acl;

import java.util.Optional;

/**
 * Open Host Service (OHS) facade exposed by User Profiles to other Bounded Contexts.
 *
 * <p>Exclusively returns Java standard and primitive types.</p>
 */
public interface ProfileContextFacade {

    /**
     * Checks if a profile exists for the specified user account.
     *
     * @param userId user account identifier string
     * @return true if profile exists; false otherwise
     */
    boolean existsProfileByUserId(String userId);

    /**
     * Resolves the full civil name for a user account.
     *
     * @param userId user account identifier string
     * @return Optional containing the full name if profile exists
     */
    Optional<String> findFullNameByUserId(String userId);

    /**
     * Resolves the normalized E.164 phone number for a user account.
     *
     * @param userId user account identifier string
     * @return Optional containing the phone number if profile exists
     */
    Optional<String> findPhoneNumberByUserId(String userId);
}
