package com.arcadiadevs.viora.platform.profiles.domain.model.queries;

import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.ProfileId;

/**
 * Domain query record for retrieving a user profile by its primary identifier.
 *
 * @param profileId the profile identifier VO
 */
public record GetProfileByIdQuery(ProfileId profileId) {

    public GetProfileByIdQuery {
        if (profileId == null) {
            throw new IllegalArgumentException("profile.id.null_or_empty");
        }
    }

    public GetProfileByIdQuery(String rawProfileId) {
        this(new ProfileId(rawProfileId));
    }
}
