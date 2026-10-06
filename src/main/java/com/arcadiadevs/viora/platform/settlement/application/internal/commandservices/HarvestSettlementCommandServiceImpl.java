package com.arcadiadevs.viora.platform.settlement.application.internal.commandservices;

import com.arcadiadevs.viora.platform.settlement.application.commandservices.HarvestSettlementCommandService;
import com.arcadiadevs.viora.platform.settlement.application.commandservices.SettlementOutcome;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.ReceiptCounterInitializer;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SettleCampaignHarvestCommand;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.ReceiptCounterRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.SettledHarvestRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.ThinningExecutionRecordRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Settles a campaign: verifies the plot and its owner, loads or opens the plot report, freezes the thinning
 * balance and the stabilization curve, persists and publishes {@code CampaignHarvestSettledEvent}.
 *
 * <p>Receipt numbers are numbered per producer and campaign, not per plot, so the numbering goes through
 * {@link ReceiptCounterRepository} instead of trusting the plot lock. An idempotency key short-circuits the whole
 * operation before any number is consumed: a replay returns the stored settlement and publishes nothing.</p>
 */
@Service
@Transactional
public class HarvestSettlementCommandServiceImpl implements HarvestSettlementCommandService {
    private final AgronomicReportRepository reportRepository;
    private final ThinningExecutionRecordRepository thinningRecordRepository;
    private final ReceiptCounterRepository receiptCounterRepository;
    private final SettledHarvestRepository settledHarvestRepository;
    private final ReceiptCounterInitializer receiptCounterInitializer;
    private final ExternalOrchardService externalOrchardService;
    private final ExternalPhenologyService externalPhenologyService;
    private final ApplicationEventPublisher publisher;
    private final Clock clock;

    /**
     * Constructs the service.
     *
     * @param reportRepository            domain port of the agronomic reports
     * @param thinningRecordRepository    domain port of the thinning execution projection
     * @param receiptCounterRepository    domain port of the receipt number counters
     * @param settledHarvestRepository    domain port that reaches a settlement on its own
     * @param receiptCounterInitializer   opens the counter of a producer and campaign before locking it
     * @param externalOrchardService      outbound ACL reading plots and their owners
     * @param externalPhenologyService    outbound ACL reading the historical yields of a plot
     * @param publisher                   application event publisher
     * @param clock                       clock stamping the settlement
     */
    public HarvestSettlementCommandServiceImpl(AgronomicReportRepository reportRepository,
            ThinningExecutionRecordRepository thinningRecordRepository,
            ReceiptCounterRepository receiptCounterRepository, SettledHarvestRepository settledHarvestRepository,
            ReceiptCounterInitializer receiptCounterInitializer, ExternalOrchardService externalOrchardService,
            ExternalPhenologyService externalPhenologyService, ApplicationEventPublisher publisher, Clock clock) {
        this.reportRepository = reportRepository;
        this.thinningRecordRepository = thinningRecordRepository;
        this.receiptCounterRepository = receiptCounterRepository;
        this.settledHarvestRepository = settledHarvestRepository;
        this.receiptCounterInitializer = receiptCounterInitializer;
        this.externalOrchardService = externalOrchardService;
        this.externalPhenologyService = externalPhenologyService;
        this.publisher = publisher;
        this.clock = clock;
    }

    @Override
    public Result<SettlementOutcome, ApplicationError> handle(SettleCampaignHarvestCommand command) {
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
        var idempotencyKey = IdempotencyKey.of(command.idempotencyKey());
        // The replay check comes before anything that consumes a receipt number: a replay has no side effects.
        if (idempotencyKey.isPresent()) {
            var replay = settledHarvestRepository
                    .findByProducerIdAndIdempotencyKey(new UserId(command.actorId()), idempotencyKey);
            if (replay.isPresent()) {
                var stored = replay.get();
                if (!stored.plotId().equals(plotId) || !stored.campaignYear().equals(campaignYear)) {
                    return Result.failure(ApplicationError.businessRuleViolation("settlement",
                            "settlement.idempotency_key.reused"));
                }
                return Result.success(SettlementOutcome.replayed(stored));
            }
        }

        var report = reportRepository.findByPlotIdForUpdate(plotId)
                .orElseGet(() -> AgronomicReport.createForPlot(plotId, owner.get()));
        if (report.settlementOf(campaignYear).isPresent()) {
            // Ruled out before the receipt number is consumed: a repeated settlement must not burn a number.
            return Result.failure(conflictWithExistingSettlement(report, campaignYear,
                    AgronomicReport.CAMPAIGN_ALREADY_SETTLED));
        }
        var balance = ThinningBalance.of(
                thinningRecordRepository.findByPlotIdAndCampaignYear(plotId, campaignYear.value()).orElse(null));
        var history = externalPhenologyService.findHistoricalYields(plotId);

        HarvestSettlementSnapshot settlement;
        try {
            // The weighing date is judged before the counter is touched, so a rejected weighing date does not
            // consume a receipt number.
            var weighedOn = WeighingDate.of(command.weighedOn(), clock);
            var receiptNumber = nextReceiptNumber(owner.get(), campaignYear);
            settlement = report.settleCampaign(campaignYear, new OliveWeight(command.greenOlivesKg()),
                    new OliveWeight(command.blackOlivesKg()), command.commercialFruitsPerKg(), command.notes(),
                    balance, history, receiptNumber, weighedOn, MillTicketNumber.of(command.millTicketNumber()),
                    idempotencyKey, clock);
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError("settlement", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return Result.failure(conflictWithExistingSettlement(report, campaignYear, exception.getMessage()));
        }
        reportRepository.save(report);
        // Projections listening with BEFORE_COMMIT join this transaction: a failure rolls the settlement back.
        report.domainEvents().forEach(publisher::publishEvent);
        report.clearDomainEvents();
        return Result.success(SettlementOutcome.created(settlement));
    }

    /** Opens the counter of the producer and campaign, then locks it so the sequence is consumed exactly once. */
    private ReceiptNumber nextReceiptNumber(UserId producerId, CampaignYear campaignYear) {
        receiptCounterInitializer.ensureExists(producerId, campaignYear);
        var counter = receiptCounterRepository.findByProducerIdAndCampaignYearForUpdate(producerId, campaignYear)
                .orElseThrow(() -> new IllegalStateException("settlement.receipt_counter.missing"));
        var receiptNumber = counter.nextReceiptNumber();
        receiptCounterRepository.save(counter);
        return receiptNumber;
    }

    /**
     * Builds the conflict of a campaign that is already settled, telling the client which settlement is in place so
     * it does not have to ask again.
     */
    private ApplicationError conflictWithExistingSettlement(AgronomicReport report, CampaignYear campaignYear,
            String reason) {
        // LinkedHashMap, not Map.of: the weighing data of a settlement persisted before the receipt contract may be
        // absent, and Map.of rejects a null value.
        var existing = new LinkedHashMap<String, Object>();
        report.settlementOf(campaignYear).ifPresent(settlement -> {
            existing.put("campaignYear", settlement.campaignYear().value());
            existing.put("totalYieldKg", settlement.totalHarvestWeight().kilograms());
            existing.put("receiptNumber",
                    settlement.receiptNumber() == null ? null : settlement.receiptNumber().value());
            existing.put("weighedOn", settlement.weighedOn() == null ? null : settlement.weighedOn().value());
        });
        return ApplicationError.conflict("HarvestSettlement", reason, Map.of("existingSettlement", existing));
    }
}
