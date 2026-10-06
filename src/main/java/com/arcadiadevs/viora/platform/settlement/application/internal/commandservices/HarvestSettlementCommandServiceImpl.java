package com.arcadiadevs.viora.platform.settlement.application.internal.commandservices;

import com.arcadiadevs.viora.platform.settlement.application.commandservices.HarvestSettlementCommandService;
import com.arcadiadevs.viora.platform.settlement.application.commandservices.SettlementOutcome;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.ReceiptCounterInitializer;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SettleCampaignHarvestCommand;
import com.arcadiadevs.viora.platform.settlement.domain.model.entities.HarvestSettlement;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.ReceiptCounterRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.SettledHarvestRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.ThinningExecutionRecordRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.NoTransactionException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

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
    private static final String IDEMPOTENCY_CONSTRAINT = "uq_settlement_producer_idempotency";
    private static final String RECEIPT_CONSTRAINT = "uq_settlement_producer_receipt";

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
        var replay = replayOf(owner.get(), plotId, campaignYear, idempotencyKey);
        if (replay.isPresent()) {
            return replay.get();
        }

        var report = reportRepository.findByPlotIdForUpdate(plotId)
                .orElseGet(() -> AgronomicReport.createForPlot(plotId, owner.get()));
        // Asked again now that the plot is locked: a request with the same key that was in flight at the same time
        // has just committed while this one waited for the lock, and it is a replay of that settlement, not a
        // conflict with it.
        replay = replayOf(owner.get(), plotId, campaignYear, idempotencyKey);
        if (replay.isPresent()) {
            return replay.get();
        }
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
            // Everything that can reject the request is judged before the counter is touched, so a rejected
            // request never consumes a receipt number.
            var weighedOn = WeighingDate.of(command.weighedOn(), clock);
            var green = new OliveWeight(command.greenOlivesKg());
            var black = new OliveWeight(command.blackOlivesKg());
            var millTicketNumber = MillTicketNumber.of(command.millTicketNumber());
            HarvestSettlement.validateFigures(green, black, command.commercialFruitsPerKg(), command.notes());
            var receiptNumber = nextReceiptNumber(owner.get(), campaignYear);
            settlement = report.settleCampaign(campaignYear, green, black, command.commercialFruitsPerKg(),
                    command.notes(), balance, history, receiptNumber, weighedOn, millTicketNumber, idempotencyKey,
                    clock);
        } catch (IllegalArgumentException exception) {
            rollBack();
            return Result.failure(ApplicationError.validationError("settlement", exception.getMessage()));
        } catch (IllegalStateException exception) {
            rollBack();
            return Result.failure(conflictWithExistingSettlement(report, campaignYear, exception.getMessage()));
        }
        try {
            reportRepository.save(report);
        } catch (DataIntegrityViolationException exception) {
            return failureOfViolated(exception);
        }
        // Projections listening with BEFORE_COMMIT join this transaction: a failure rolls the settlement back.
        report.domainEvents().forEach(publisher::publishEvent);
        report.clearDomainEvents();
        return Result.success(SettlementOutcome.created(settlement));
    }

    /**
     * Looks for the settlement the idempotency key was already used for, and answers the request from it.
     *
     * @return empty when there is no key or it was never used, a replay when it settled this very plot and
     *         campaign, a failure when it settled another plot or campaign of the producer
     */
    private Optional<Result<SettlementOutcome, ApplicationError>> replayOf(UserId producerId, PlotId plotId,
            CampaignYear campaignYear, IdempotencyKey idempotencyKey) {
        if (!idempotencyKey.isPresent()) {
            return Optional.empty();
        }
        return settledHarvestRepository.findByProducerIdAndIdempotencyKey(producerId, idempotencyKey).map(stored -> {
            if (!stored.plotId().equals(plotId) || !stored.campaignYear().equals(campaignYear)) {
                return Result.failure(keyReused());
            }
            return Result.success(SettlementOutcome.replayed(stored));
        });
    }

    private static ApplicationError keyReused() {
        return ApplicationError.businessRuleViolation("settlement", "settlement.idempotency_key.reused");
    }

    /**
     * Translates the unique constraints of a settlement that lost a race, rolling the transaction back so the
     * receipt number it had consumed is released.
     *
     * <ul>
     *   <li>{@value #IDEMPOTENCY_CONSTRAINT}: the same key reached two different plots at once. The plot lock cannot
     *       serialize those, so the loser meets the constraint instead of the replay lookup. Settling one plot can
     *       never be a replay of settling another, so this is the key being reused.</li>
     *   <li>{@value #RECEIPT_CONSTRAINT}: a receipt number was handed out twice. The counter is caught up with the
     *       stored numbers before it allocates, so this should not happen; if it does it is a server fault to
     *       retry, never a conflict of the producer. It is not retried here because Postgres leaves the
     *       transaction unusable after a violation, so a retry needs a new request.</li>
     * </ul>
     *
     * <p>Any other violation (for example a campaign settled by a concurrent request on a brand new report) is
     * rethrown untouched and keeps being a generic conflict.</p>
     */
    private Result<SettlementOutcome, ApplicationError> failureOfViolated(DataIntegrityViolationException exception) {
        if (violates(exception, IDEMPOTENCY_CONSTRAINT)) {
            rollBack();
            return Result.failure(keyReused());
        }
        if (violates(exception, RECEIPT_CONSTRAINT)) {
            rollBack();
            return Result.failure(ApplicationError.unexpected("settlement", "settlement.receipt_number.unavailable"));
        }
        throw exception;
    }

    private static boolean violates(Throwable exception, String constraint) {
        for (var cause = exception; cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().toLowerCase(Locale.ROOT).contains(constraint)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Marks the running transaction to roll back, so a failure that returns a {@link Result} instead of throwing still
     * undoes whatever it had already written, above all the receipt number it consumed.
     */
    private static void rollBack() {
        try {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
        } catch (NoTransactionException ignored) {
            // Not running inside a transaction (a plain unit test): there is nothing to roll back.
        }
    }

    /**
     * Opens the counter of the producer and campaign, locks it and catches it up with the receipt numbers already
     * issued, so the sequence is consumed exactly once and a number already printed on a receipt is never reused.
     *
     * <p>The catch-up covers settlements numbered outside of the counter: legacy rows numbered by the backfill, or a
     * counter that was opened empty after the numbers had been written.</p>
     */
    private ReceiptNumber nextReceiptNumber(UserId producerId, CampaignYear campaignYear) {
        receiptCounterInitializer.ensureExists(producerId, campaignYear);
        var counter = receiptCounterRepository.findByProducerIdAndCampaignYearForUpdate(producerId, campaignYear)
                .orElseThrow(() -> new IllegalStateException("settlement.receipt_counter.missing"));
        counter.catchUpTo(settledHarvestRepository.findHighestReceiptSequence(producerId, campaignYear));
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
