package com.arcadiadevs.viora.platform.phenology.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.HistoricalHarvestEntrySnapshot;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetHarvestRecordsByPlotIdQuery;

import java.util.List;

/**
 * Application query service port handling retrieval of historical harvest records.
 */
public interface HarvestRecordQueryService {

    /**
     * Handles the retrieval of historical harvest records for an orchard plot,
     * verifying plot validity via ACL and applying optional campaign year filtering.
     *
     * @param query the domain query specification
     * @return {@link Result} with list of entry snapshots on success, or an {@link ApplicationError} on failure
     */
    Result<List<HistoricalHarvestEntrySnapshot>, ApplicationError> handle(GetHarvestRecordsByPlotIdQuery query);
}
