package com.arcadiadevs.viora.platform.settlement.application.internal.commandservices;

import com.arcadiadevs.viora.platform.settlement.application.commandservices.HarvestSettlementCommandService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SettleCampaignHarvestCommand;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.ThinningExecutionRecordRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Settles a campaign: verifies the plot and its owner, loads or opens the plot report, freezes the thinning
 * balance and the stabilization curve, persists and publishes {@code CampaignHarvestSettledEvent}.
 */
@Service
@Transactional
public class HarvestSettlementCommandServiceImpl implements HarvestSettlementCommandService {
    private final AgronomicReportRepository reportRepository;
    private final ThinningExecutionRecordRepository thinningRecordRepository;
    private final ExternalOrchardService externalOrchardService;
    private final ExternalPhenologyService externalPhenologyService;
    private final ApplicationEventPublisher publisher;

    public HarvestSettlementCommandServiceImpl(AgronomicReportRepository reportRepository,
            ThinningExecutionRecordRepository thinningRecordRepository, ExternalOrchardService externalOrchardService,
            ExternalPhenologyService externalPhenologyService, ApplicationEventPublisher publisher) {
        this.reportRepository = reportRepository;
        this.thinningRecordRepository = thinningRecordRepository;
        this.externalOrchardService = externalOrchardService;
        this.externalPhenologyService = externalPhenologyService;
        this.publisher = publisher;
    }

    @Override
    public Result<HarvestSettlementSnapshot, ApplicationError> handle(SettleCampaignHarvestCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "settlement.command.null"));
        }
        var plotId = new PlotId(command.plotId());
        var owner = externalOrchardService.findActivePlotOwner(plotId);
        if (owner.isEmpty()) {
            return Result.failure(ApplicationError.notFound("Plot", plotId.plotId()));
        }
        if (!owner.get().equals(new UserId(command.actorId()))) {
            return Result.failure(ApplicationError.forbidden("Plot", "settlement.plot.not_owner"));
        }

        var campaignYear = new CampaignYear(command.campaignYear());
        var report = reportRepository.findByPlotIdForUpdate(plotId)
                .orElseGet(() -> AgronomicReport.createForPlot(plotId, owner.get()));
        var balance = ThinningBalance.of(
                thinningRecordRepository.findByPlotIdAndCampaignYear(plotId, campaignYear.value()).orElse(null));
        var history = externalPhenologyService.findHistoricalYields(plotId);

        HarvestSettlementSnapshot settlement;
        try {
            settlement = report.settleCampaign(campaignYear, new OliveWeight(command.greenOlivesKg()),
                    new OliveWeight(command.blackOlivesKg()), command.commercialFruitsPerKg(), command.notes(),
                    balance, history, Clock.systemUTC());
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError("settlement", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return Result.failure(ApplicationError.conflict("HarvestSettlement", exception.getMessage()));
        }
        reportRepository.save(report);
        // Projections listening with BEFORE_COMMIT join this transaction: a failure rolls the settlement back.
        report.domainEvents().forEach(publisher::publishEvent);
        report.clearDomainEvents();
        return Result.success(settlement);
    }
}
