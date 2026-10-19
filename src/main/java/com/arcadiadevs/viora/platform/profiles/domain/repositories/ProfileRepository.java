package com.arcadiadevs.viora.platform.profiles.domain.repositories;

import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.ProfileId;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.UserId;

import java.util.Optional;

/**
 * Pure Domain Repository port for persisting and querying {@link Profile} aggregates.
 *
 * <p>Contains zero Spring Data or infrastructure dependencies.</p>
 */
public interface ProfileRepository {

    /**
     * Persists the profile state.
     *
     * @param profile aggregate to persist
     * @return persisted aggregate
     */
    Profile save(Profile profile);

    /**
     * Finds a profile by its unique identifier.
     *
     * @param id profile identifier VO
     * @return Optional containing the profile if present
     */
    Optional<Profile> findById(ProfileId id);

    /**
     * Finds a profile associated with a specific user account.
     *
     * @param userId user account identifier VO
     * @return Optional containing the profile if present
     */
    Optional<Profile> findByUserId(UserId userId);

    /**
     * Checks if a profile already exists for the given user identifier.
     *
     * @param userId user account identifier VO
     * @return true if a profile exists; false otherwise
     */
    boolean existsByUserId(UserId userId);

    /**
     * Removes a profile.
     *
     * @param profile aggregate to remove
     */
    void delete(Profile profile);
}
