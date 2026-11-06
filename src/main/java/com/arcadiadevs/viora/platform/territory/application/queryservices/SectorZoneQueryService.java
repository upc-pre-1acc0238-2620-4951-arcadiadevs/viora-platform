package com.arcadiadevs.viora.platform.territory.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetSectorZoneByIdAndUserIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetSectorZonesByUserIdQuery;

import java.util.List;

/**
 * Query service port of the sector list of a license.
 */
public interface SectorZoneQueryService {

    /**
     * Lists the sectors of the license the user reaches, alphabetically.
     *
     * @param query the signed-in user
     * @return the sectors, or a 403 when the user reaches no license
     */
    Result<List<SectorZoneView>, ApplicationError> handle(GetSectorZonesByUserIdQuery query);

    /**
     * Reads one sector of the license the user reaches.
     *
     * @param query the sector and the signed-in user
     * @return the sector, a 404 when it is not in that list, or a 403 when the user reaches no license
     */
    Result<SectorZoneView, ApplicationError> handle(GetSectorZoneByIdAndUserIdQuery query);
}
