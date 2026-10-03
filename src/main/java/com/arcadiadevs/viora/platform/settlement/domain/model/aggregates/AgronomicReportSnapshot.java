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
 * @param certifications certified campaigns, at most one per campaign
 * @param revision    optimistic concurrency version
 */
public record AgronomicReportSnapshot(
        ReportId id,
        PlotId plotId,
        UserId producerId,
        List<HarvestSettlementSnapshot> settlements,
        List<DossierCertificationSnapshot> certifications,
        Long revision
) {
    public AgronomicReportSnapshot {
        settlements = settlements == null ? List.of() : List.copyOf(settlements);
        certifications = certifications == null ? List.of() : List.copyOf(certifications);
    }
}
