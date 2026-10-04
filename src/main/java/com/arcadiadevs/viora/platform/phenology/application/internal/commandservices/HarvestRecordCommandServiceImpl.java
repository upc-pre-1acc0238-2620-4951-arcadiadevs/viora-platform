package com.arcadiadevs.viora.platform.phenology.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.phenology.application.commandservices.HarvestRecordCommandService;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.domain.exceptions.HarvestRecordNotFoundException;
import com.arcadiadevs.viora.platform.phenology.domain.exceptions.TrackerRevisionMismatchException;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RecordHarvestYieldCommand;
import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RectifyHarvestYieldCommand;
import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RemoveHarvestRecordCommand;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestEntryId;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestYield;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
import com.arcadiadevs.viora.platform.phenology.domain.services.HarvestCampaignYearPolicy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Application service implementation orchestrating harvest yield recording and BBI assessments.
 */
@Service
@Transactional
public class HarvestRecordCommandServiceImpl implements HarvestRecordCommandService {

    private final ChillAccumulationTrackerRepository trackerRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ExternalOrchardService externalOrchardService;
    private final Clock clock;

    /**
     * Constructs the command service injecting dependencies.
     *
     * @param trackerRepository      the domain repository port
     * @param eventPublisher         the application event publisher
     * @param externalOrchardService the outbound ACL service for orchard verifications
     * @param clock                  the application clock used to reject future harvest campaigns
     */
    public HarvestRecordCommandServiceImpl(
            ChillAccumulationTrackerRepository trackerRepository,
            ApplicationEventPublisher eventPublisher,
            ExternalOrchardService externalOrchardService,
            Clock clock
    ) {
        this.trackerRepository = trackerRepository;
        this.eventPublisher = eventPublisher;
        this.externalOrchardService = externalOrchardService;
        this.clock = clock;
    }

    @Override
    public Result<String, ApplicationError> handle(RecordHarvestYieldCommand command) {
        try {
            var plotId = new PlotId(command.plotId());

            if (!externalOrchardService.existsActivePlot(plotId)) {
                return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
            }

            var campaignYear = HarvestCampaignYearPolicy.forHarvestRecording(command.campaignYear(), clock);
            var harvestYield = new HarvestYield(command.totalYieldKg(), command.greenKg(), command.blackKg());

            if (trackerRepository.existsByPlotIdAndCampaignYear(plotId, campaignYear)) {
                return Result.failure(ApplicationError.conflict("harvest", "phenology.harvest_yield.duplicate_campaign"));
            }

            var tracker = trackerRepository.findByPlotId(plotId)
                    .orElseGet(() -> ChillAccumulationTracker.create(plotId, campaignYear));

            var entry = tracker.recordHarvest(campaignYear, harvestYield);

            trackerRepository.save(tracker);

            for (var event : tracker.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            tracker.clearDomainEvents();

            return Result.success(entry.snapshot().id().harvestEntryId());
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("harvest-record", ex.getMessage()));
        }
    }

    @Override
    public Result<String, ApplicationError> handle(RectifyHarvestYieldCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("phenology.command.null");
        }
        try {
            var plotId = new PlotId(command.plotId());

            if (!externalOrchardService.existsActivePlot(plotId)) {
                return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
            }

            var trackerOpt = trackerRepository.findByPlotId(plotId);
            if (trackerOpt.isEmpty()) {
                return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
            }

            var tracker = trackerOpt.get();
            var recordId = new HarvestEntryId(command.recordId());
            var harvestYield = new HarvestYield(command.totalYieldKg(), command.greenKg(), command.blackKg());

            var rectifiedEntry = tracker.rectifyHarvestRecord(recordId, harvestYield, command.expectedRevision());

            trackerRepository.save(tracker);

            for (var event : tracker.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            tracker.clearDomainEvents();

            return Result.success(rectifiedEntry.snapshot().id().harvestEntryId());
        } catch (TrackerRevisionMismatchException ex) {
            return Result.failure(ApplicationError.preconditionFailed("tracker", ex.getMessage()));
        } catch (HarvestRecordNotFoundException ex) {
            return Result.failure(ApplicationError.notFound("HarvestRecord", command.recordId()));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("harvest-rectify", ex.getMessage()));
        }
    }

    @Override
    public Result<String, ApplicationError> handle(RemoveHarvestRecordCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("phenology.command.null");
        }
        try {
            var plotId = new PlotId(command.plotId());

            if (!externalOrchardService.existsActivePlot(plotId)) {
                return Result.failure(ApplicationError.notFound("Plot", command.plotId()));
            }

            var trackerOpt = trackerRepository.findByPlotId(plotId);
            if (trackerOpt.isEmpty()) {
                return Result.failure(ApplicationError.notFound("HarvestRecord", command.recordId()));
            }

            var tracker = trackerOpt.get();
            var removedEntry = tracker.removeHarvestRecord(new HarvestEntryId(command.recordId()), command.expectedRevision());

            trackerRepository.save(tracker);

            for (var event : tracker.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            tracker.clearDomainEvents();

            return Result.success(removedEntry.snapshot().id().harvestEntryId());
        } catch (TrackerRevisionMismatchException ex) {
            return Result.failure(ApplicationError.preconditionFailed("tracker", ex.getMessage()));
        } catch (HarvestRecordNotFoundException ex) {
            return Result.failure(ApplicationError.notFound("HarvestRecord", command.recordId()));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        } catch (Exception ex) {
            return Result.failure(ApplicationError.unexpected("harvest-remove", ex.getMessage()));
        }
    }
}
