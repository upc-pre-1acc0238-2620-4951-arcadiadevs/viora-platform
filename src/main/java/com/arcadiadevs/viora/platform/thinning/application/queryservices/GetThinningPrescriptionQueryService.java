package com.arcadiadevs.viora.platform.thinning.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetActiveThinningPrescriptionQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetThinningPrescriptionByIdQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetThinningPrescriptionsQuery;

import java.util.List;

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

    /**
     * Retrieves the prescriptions of the producer's active plots for a campaign, in any state.
     *
     * @param query producer and campaign
     * @return one snapshot per plot that has a prescription; plots never sampled have none
     */
    Result<List<FruitThinningPrescriptionSnapshot>, ApplicationError> handle(GetThinningPrescriptionsQuery query);

    /**
     * Retrieves one prescription by its identifier.
     *
     * @param query prescription identifier
     * @return the snapshot, or not found when it does not exist or its plot is no longer active
     */
    Result<FruitThinningPrescriptionSnapshot, ApplicationError> handle(GetThinningPrescriptionByIdQuery query);
}
