package com.arcadiadevs.viora.platform.settlement.domain.model.aggregates;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ReportId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;

import java.util.List;

/**
 * Immutable state of an agronomic report.
 *
 * @param id          report identifier
 * @param plotId      plot of the report (one report per plot)
 * @param producerId  owner producer
 * @param settlements settled campaigns
 * @param revision    optimistic concurrency version
 */
public record AgronomicReportSnapshot(
        ReportId id,
        PlotId plotId,
        UserId producerId,
        List<HarvestSettlementSnapshot> settlements,
        Long revision
) {
    public AgronomicReportSnapshot {
        settlements = settlements == null ? List.of() : List.copyOf(settlements);
    }
}
