package com.arcadiadevs.viora.platform.thinning.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetActiveThinningPrescriptionQuery;

/**
 * Application query service for retrieving thinning prescriptions.
 */
public interface GetThinningPrescriptionQueryService {

    /**
     * Retrieves a thinning prescription without mutating aggregate state.
     *
     * @param query prescription lookup criteria
     * @return prescription snapshot or application error
     */
    Result<FruitThinningPrescriptionSnapshot, ApplicationError> handle(GetActiveThinningPrescriptionQuery query);
}
