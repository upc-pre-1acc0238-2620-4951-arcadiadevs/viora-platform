package com.arcadiadevs.viora.platform.settlement.domain.repositories;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ThinningExecutionRecord;

import java.util.Optional;

/** Domain port for Settlement's projection of confirmed thinning executions. */
public interface ThinningExecutionRecordRepository {

    Optional<ThinningExecutionRecord> findByPlotIdAndCampaignYear(PlotId plotId, Integer campaignYear);

    /**
     * Stores the execution once; a repeated event or a second record for the same plot and campaign is ignored.
     *
     * @param record projected execution
     * @return {@code true} when stored, {@code false} when it was already known
     */
    boolean saveIfAbsent(ThinningExecutionRecord record);
}
