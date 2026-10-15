package com.arcadiadevs.viora.platform.profiles.domain.model.queries;

import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.UserId;

/**
 * Domain query record for retrieving a user profile by the owning user identifier.
 *
 * @param userId the user identifier VO
 */
public record GetProfileByUserIdQuery(UserId userId) {

    public GetProfileByUserIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("profile.user_id.null");
        }
    }

    public GetProfileByUserIdQuery(String rawUserId) {
        this(new UserId(rawUserId));
    }
}
