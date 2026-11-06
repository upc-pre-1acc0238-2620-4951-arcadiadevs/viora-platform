package com.arcadiadevs.viora.platform.territory.domain.model.queries;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.GeoPoint;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;

/**
 * Domain query for the plots of the members who share with the manager's license, in visit order.
 *
 * @param managerId the signed-in manager
 * @param origin    where the manager stands, to tell distances and break ties; null without GPS
 */
public record GetMemberPlotsByManagerIdQuery(ManagerId managerId, GeoPoint origin) {

    /**
     * Compact constructor validating that managerId is not null.
     *
     * @param managerId the signed-in manager
     * @param origin    the optional location
     */
    public GetMemberPlotsByManagerIdQuery {
        if (managerId == null) {
            throw new IllegalArgumentException("territory.manager.id.null_or_empty");
        }
    }

    /**
     * Convenience constructor from a raw manager id and optional coordinates, which come together or not at all.
     *
     * @param rawManagerId the raw UUID string of the manager
     * @param latitude     the latitude, or null
     * @param longitude    the longitude, or null
     * @throws IllegalArgumentException if only one coordinate is given or one is out of range
     */
    public GetMemberPlotsByManagerIdQuery(String rawManagerId, Double latitude, Double longitude) {
        this(new ManagerId(rawManagerId), originOf(latitude, longitude));
    }

    private static GeoPoint originOf(Double latitude, Double longitude) {
        if (latitude == null && longitude == null) {
            return null;
        }
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("territory.location.incomplete");
        }
        return new GeoPoint(latitude, longitude);
    }
}
