package com.arcadiadevs.viora.platform.territory.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetCooperativeMemberByIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetCooperativeMembersByManagerIdQuery;

import java.util.List;

/**
 * Query service port of the member roll of a manager's license.
 */
public interface CooperativeMemberQueryService {

    /**
     * Lists the members of the license, by name.
     *
     * @param query the manager
     * @return the members, or a 403 when the user is not a manager with a license in force
     */
    Result<List<CooperativeMemberView>, ApplicationError> handle(GetCooperativeMembersByManagerIdQuery query);

    /**
     * Reads one member of the license.
     *
     * @param query the member and the manager
     * @return the member, a 404 when they are not one, or a 403
     */
    Result<CooperativeMemberDetailView, ApplicationError> handle(GetCooperativeMemberByIdAndManagerIdQuery query);
}
