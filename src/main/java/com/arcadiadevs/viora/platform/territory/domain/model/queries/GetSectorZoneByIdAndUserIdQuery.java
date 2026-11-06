package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.UserId;

/**
 * Domain query for one sector zone, only if it is in the list the user reaches.
 *
 * @param sectorZoneId the sector zone
 * @param userId       the authenticated user
 */
public record GetSectorZoneByIdAndUserIdQuery(SectorZoneId sectorZoneId, UserId userId) {

    /**
     * Compact constructor validating that neither field is null.
     *
     * @param sectorZoneId the sector zone
     * @param userId       the authenticated user
     */
    public GetSectorZoneByIdAndUserIdQuery {
        if (sectorZoneId == null) {
            throw new IllegalArgumentException("territory.sector_zone.id.null_or_empty");
        }
        if (userId == null) {
            throw new IllegalArgumentException("user.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor from raw UUID strings.
     *
     * @param rawSectorZoneId the raw UUID string of the sector zone
     * @param rawUserId       the raw UUID string of the user
     */
    public GetSectorZoneByIdAndUserIdQuery(String rawSectorZoneId, String rawUserId) {
        this(new SectorZoneId(rawSectorZoneId), new UserId(rawUserId));
    }
}
