package com.arcadiadevs.viora.platform.phenology.application.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.HistoricalHarvestEntrySnapshot;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetChillAccumulationTrackerByPlotIdQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetHarvestRecordsByPlotIdQuery;

import java.util.List;
import java.util.Optional;

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

    /**
     * Retrieves the chill accumulation tracker of a plot, with its harvest history and calculated BBI.
     *
     * @param query the query carrying the plot identifier
     * @return the tracker, or empty when the plot has none
     */
    Optional<ChillAccumulationTracker> handle(GetChillAccumulationTrackerByPlotIdQuery query);
}
