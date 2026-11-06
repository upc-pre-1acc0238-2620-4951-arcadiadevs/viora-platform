package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.UserId;

/**
 * Domain query for the sector list a user reaches: the list of the license a manager is in charge of, or of the
 * license that sponsors a member.
 *
 * @param userId the authenticated user
 */
public record GetSectorZonesByUserIdQuery(UserId userId) {

    /**
     * Compact constructor validating that userId is not null.
     *
     * @param userId the authenticated user
     */
    public GetSectorZonesByUserIdQuery {
        if (userId == null) {
            throw new IllegalArgumentException("user.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor accepting a raw UUID string.
     *
     * @param rawUserId the raw UUID string of the user
     */
    public GetSectorZonesByUserIdQuery(String rawUserId) {
        this(new UserId(rawUserId));
    }
}
