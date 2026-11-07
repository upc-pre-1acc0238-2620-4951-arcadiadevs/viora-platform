package com.arcadiadevs.viora.platform.territory.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetSectorAlertsByManagerIdQuery;

import java.util.List;

/**
 * Query service port of the manager's alert center.
 */
public interface SectorAlertQueryService {

    /**
     * Lists the open alerts of the manager's sectors, critical first, and the ones normalized in the last 7 days.
     *
     * @param query the manager
     * @return the alerts, or a 403 when the user is not a manager with a license in force
     */
    Result<List<SectorAlertView>, ApplicationError> handle(GetSectorAlertsByManagerIdQuery query);
}
