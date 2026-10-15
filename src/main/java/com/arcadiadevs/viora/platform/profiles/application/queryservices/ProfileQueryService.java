package com.arcadiadevs.viora.platform.profiles.application.queryservices;

import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByIdQuery;
import com.arcadiadevs.viora.platform.profiles.domain.model.queries.GetProfileByUserIdQuery;

import java.util.Optional;

/**
 * Application query service port for querying user profiles.
 * Dispatches domain queries via uniform {@code handle} methods.
 */
public interface ProfileQueryService {

    /**
     * Resolves a profile by its primary identifier query.
     *
     * @param query the get profile by ID domain query
     * @return Optional containing the profile aggregate if found
     */
    Optional<Profile> handle(GetProfileByIdQuery query);

    /**
     * Resolves a profile by its owner user identifier query.
     *
     * @param query the get profile by user ID domain query
     * @return Optional containing the profile aggregate if found
     */
    Optional<Profile> handle(GetProfileByUserIdQuery query);
}
