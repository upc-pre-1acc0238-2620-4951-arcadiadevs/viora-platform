package com.arcadiadevs.viora.platform.profiles.domain.model.queries;

import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.UserId;

/**
 * Domain query for retrieving the profile of a user account.
 *
 * @param userId the account whose profile is requested
 */
public record GetProfileByUserIdQuery(UserId userId) {

    public GetProfileByUserIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("profile.user_id.null");
        }
    }

    /**
     * Creates the query from a raw user identifier.
     *
     * @param rawUserId the account whose profile is requested
     */
    public GetProfileByUserIdQuery(String rawUserId) {
        this(new UserId(rawUserId));
    }
}
