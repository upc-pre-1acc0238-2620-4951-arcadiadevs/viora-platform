package com.arcadiadevs.viora.platform.settlement.application.internal.queryservices;

import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.settlement.application.queryservices.HarvestSettlementQueryService;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetHarvestSettlementByPlotIdAndCampaignYearQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetHarvestSettlementsByPlotIdQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Reads the settled campaigns of a plot: verifies the plot and its owner, then serves the frozen settlement
 * vouchers of its report, newest campaign first. A plot with no report yet simply has no settlements.
 */
@Service
@Transactional(readOnly = true)
public class HarvestSettlementQueryServiceImpl implements HarvestSettlementQueryService {
    private static final Comparator<HarvestSettlementSnapshot> BY_CAMPAIGN_DESCENDING =
            Comparator.<HarvestSettlementSnapshot>comparingInt(settlement -> settlement.campaignYear().value())
                    .reversed();

    private final AgronomicReportRepository reportRepository;
    private final ExternalOrchardService externalOrchardService;

    public HarvestSettlementQueryServiceImpl(AgronomicReportRepository reportRepository,
            ExternalOrchardService externalOrchardService) {
        this.reportRepository = reportRepository;
        this.externalOrchardService = externalOrchardService;
    }

    @Override
    public Result<List<HarvestSettlementSnapshot>, ApplicationError> handle(
            GetHarvestSettlementsByPlotIdQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "settlement.query.null"));
        }
        var plotId = new PlotId(query.plotId());
        var owner = externalOrchardService.findActivePlotOwner(plotId);
        if (owner.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Plot", plotId.plotId()));
        }
        if (!owner.get().equals(new UserId(query.actorId()))) {
            return Result.failure(ApplicationError.forbidden("Plot", "settlement.plot.not_owner"));
        }

        var report = reportRepository.findByPlotId(plotId);
        if (report.isEmpty()) {
            return Result.success(List.of());
        }
        return Result.success(report.get().snapshot().settlements().stream()
                .sorted(BY_CAMPAIGN_DESCENDING)
                .toList());
    }

    @Override
    public Result<HarvestSettlementSnapshot, ApplicationError> handle(
            GetHarvestSettlementByPlotIdAndCampaignYearQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "settlement.query.null"));
        }
        var plotId = new PlotId(query.plotId());
        var owner = externalOrchardService.findActivePlotOwner(plotId);
        if (owner.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Plot", plotId.plotId()));
        }
        if (!owner.get().equals(new UserId(query.actorId()))) {
            return Result.failure(ApplicationError.forbidden("Plot", "settlement.plot.not_owner"));
        }

        var settlement = reportRepository.findByPlotId(plotId)
                .flatMap(report -> report.settlementOf(new CampaignYear(query.campaignYear())));
        if (settlement.isEmpty()) {
            return Result.failure(ApplicationError.notFound("HarvestSettlement", query.campaignYear().toString()));
        }
        return Result.success(settlement.get());
    }
}
