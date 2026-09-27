package com.arcadiadevs.viora.platform.phenology.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.phenology.application.commandservices.HarvestRecordCommandService;
import com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker;
import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RecordHarvestYieldCommand;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestYield;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementation orchestrating harvest yield recording and BBI assessments.
 */
@Service
@Transactional
public class HarvestRecordCommandServiceImpl implements HarvestRecordCommandService {

    private final ChillAccumulationTrackerRepository trackerRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs the command service injecting dependencies.
     *
     * @param trackerRepository the domain repository port
     * @param eventPublisher    the application event publisher
     */
    public HarvestRecordCommandServiceImpl(
            ChillAccumulationTrackerRepository trackerRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.trackerRepository = trackerRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<String, ApplicationError> handle(RecordHarvestYieldCommand command) {
        try {
            var plotId = new PlotId(command.plotId());
            var campaignYear = new CampaignYear(command.campaignYear());
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
}
