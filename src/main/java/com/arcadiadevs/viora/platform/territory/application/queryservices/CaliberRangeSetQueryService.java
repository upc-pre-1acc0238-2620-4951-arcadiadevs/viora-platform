package com.arcadiadevs.viora.platform.territory.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetCaliberRangesByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRange;

import java.util.List;

/**
 * Reads of the caliber ranges of a license.
 */
public interface CaliberRangeSetQueryService {

    /**
     * Reads the caliber ranges of the license the manager is in charge of.
     *
     * @param query the manager
     * @return the ranges, empty when none were set
     */
    Result<List<CaliberRange>, ApplicationError> handle(GetCaliberRangesByManagerIdQuery query);
}
