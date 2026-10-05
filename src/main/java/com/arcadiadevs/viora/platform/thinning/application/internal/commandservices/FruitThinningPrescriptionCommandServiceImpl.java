package com.arcadiadevs.viora.platform.thinning.application.internal.commandservices;

import java.time.Clock;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.RecordFullBloomCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.EvaluateThinningPrescriptionCommand;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.ThinningPrescriptionIssuer;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.application.commandservices.FruitThinningPrescriptionCommandService;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.IngestFieldSamplingsBatchCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.TreeSampleItem;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Command Service implementation orchestrating operations on the FruitThinningPrescription aggregate.
 */
@Service
@Transactional
public class FruitThinningPrescriptionCommandServiceImpl implements FruitThinningPrescriptionCommandService {

    private final FruitThinningPrescriptionRepository prescriptionRepository;
    private final ExternalOrchardService externalOrchardService;
    private final ApplicationEventPublisher eventPublisher;
    private final ThinningPrescriptionIssuer issuer;
    private final Clock clock;

    /**
     * Constructs the command service with required dependencies.
     *
     * @param prescriptionRepository the domain repository
     * @param externalOrchardService the outbound orchard ACL
     * @param eventPublisher         the spring application event publisher
     * @param issuer                 issues the prescription once its inputs are known
     * @param clock                  clock used to reject future dates
     */
    public FruitThinningPrescriptionCommandServiceImpl(
            FruitThinningPrescriptionRepository prescriptionRepository,
            ExternalOrchardService externalOrchardService,
            ApplicationEventPublisher eventPublisher,
            ThinningPrescriptionIssuer issuer,
            Clock clock
    ) {
        this.prescriptionRepository = prescriptionRepository;
        this.externalOrchardService = externalOrchardService;
        this.eventPublisher = eventPublisher;
        this.issuer = issuer;
        this.clock = clock;
    }

    @Override
    public Result<FruitThinningPrescription, ApplicationError> handle(IngestFieldSamplingsBatchCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "thinning.command.null"));
        }

        var plotId = new PlotId(command.plotId());
        var actorId = new UserId(command.actorId());
        var campaignYear = new CampaignYear(command.campaignYear());

        // 1. Verify that plot exists and is active in Orchard context via ACL
        if (!externalOrchardService.existsActivePlot(plotId)) {
            return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
        }

        // 2. Map command items to domain entities
        List<TreeSamplingRecord> domainRecords = new ArrayList<>();
        try {
            for (TreeSampleItem item : command.samples()) {
                domainRecords.add(TreeSamplingRecord.create(
                        item.treeTag(),
                        item.shootCount(),
                        item.fruitSetCount(),
                        item.trunkDiameterMm(),
                        item.samplingDate()
                ));
            }
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("samples", e.getMessage()));
        }

        // 3. Retrieve or create prescription aggregate for plot and campaign
        Optional<FruitThinningPrescription> existingPrescription =
                prescriptionRepository.findByPlotIdAndCampaignYear(plotId, campaignYear);

        FruitThinningPrescription prescription = existingPrescription.orElseGet(() ->
                FruitThinningPrescription.createForPlot(plotId, campaignYear, 1L)
        );

        // 4. Ingest sampling batch into aggregate (handles deduplication and idempotency)
        try {
            SamplingBatchId batchId = new SamplingBatchId(command.clientBatchId());
            prescription.ingestSamplingsBatch(actorId, batchId, domainRecords);
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.conflict("SamplingRound", e.getMessage()));
        }

        // 5. Persist aggregate atomically
        FruitThinningPrescription saved = prescriptionRepository.save(prescription);

        // Publish domain events
        for (var event : prescription.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        prescription.clearDomainEvents();

        return Result.success(saved);
    }

    @Override
    public Result<FruitThinningPrescription, ApplicationError> handle(RecordFullBloomCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "thinning.command.null"));
        }
        final PlotId plotId;
        final CampaignYear campaignYear;
        try {
            plotId = new PlotId(command.plotId());
            campaignYear = new CampaignYear(command.campaignYear());
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("request", e.getMessage()));
        }
        if (!externalOrchardService.existsActivePlot(plotId)) {
            return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
        }
        FruitThinningPrescription prescription = prescriptionRepository.findByPlotIdAndCampaignYear(plotId, campaignYear)
                .orElseGet(() -> FruitThinningPrescription.createForPlot(plotId, campaignYear, 1L));
        // Work on a candidate: a failed reissue must not mutate the original issued prescription.
        prescription = FruitThinningPrescription.reconstitute(prescription.snapshot());
        boolean requiresReissue = prescription.snapshot().status()
                == com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus.PRESCRIBED;
        try {
            prescription.recordFullBloom(command.observedOn(), clock);
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("observedOn", e.getMessage()));
        } catch (IllegalStateException e) {
            return Result.failure(ApplicationError.conflict("ThinningPrescription", e.getMessage()));
        }
        boolean issued = issuer.issueIfReady(prescription, true);
        if (requiresReissue && !issued) {
            return Result.failure(ApplicationError.conflict("ThinningPrescription",
                    "thinning.full_bloom.reissue_unavailable"));
        }
        return Result.success(saveAndPublish(prescription));
    }

    @Override
    public Result<FruitThinningPrescription, ApplicationError> handle(EvaluateThinningPrescriptionCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "thinning.command.null"));
        }
        final PlotId plotId;
        final CampaignYear campaignYear;
        try {
            plotId = new PlotId(command.plotId());
            campaignYear = new CampaignYear(command.campaignYear());
        } catch (IllegalArgumentException e) {
            return Result.failure(ApplicationError.validationError("request", e.getMessage()));
        }
        var found = prescriptionRepository.findByPlotIdAndCampaignYear(plotId, campaignYear);
        if (found.isEmpty()) {
            return Result.failure(ApplicationError.notFound("ThinningPrescription", command.plotId()));
        }
        var prescription = found.get();
        if (!issuer.issueIfReady(prescription, false)) {
            return Result.success(prescription);
        }
        return Result.success(saveAndPublish(prescription));
    }

    private FruitThinningPrescription saveAndPublish(FruitThinningPrescription prescription) {
        FruitThinningPrescription saved = prescriptionRepository.save(prescription);
        for (var event : prescription.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        prescription.clearDomainEvents();
        return saved;
    }
}
