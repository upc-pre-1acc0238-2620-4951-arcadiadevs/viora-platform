package com.arcadiadevs.viora.platform.thinning.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.*;
import com.arcadiadevs.viora.platform.thinning.application.commandservices.ConfirmThinningExecutionCommandService;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.ConfirmThinningExecutionCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberCalibration;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionId;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.CaliberCalibrationObservationRepository;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import com.arcadiadevs.viora.platform.thinning.domain.services.CaliberModelFittingService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/** Serializes confirmations per prescription and publishes events within the saving transaction. */
@Service
@Transactional
public class ConfirmThinningExecutionCommandServiceImpl implements ConfirmThinningExecutionCommandService {
    private final FruitThinningPrescriptionRepository repository;
    private final CaliberCalibrationObservationRepository observationRepository;
    private final ExternalOrchardService externalOrchardService;
    private final ApplicationEventPublisher publisher;

    public ConfirmThinningExecutionCommandServiceImpl(FruitThinningPrescriptionRepository repository,
            CaliberCalibrationObservationRepository observationRepository,
            ExternalOrchardService externalOrchardService, ApplicationEventPublisher publisher) {
        this.repository = repository;
        this.observationRepository = observationRepository;
        this.externalOrchardService = externalOrchardService;
        this.publisher = publisher;
    }

    @Override
    public Result<FruitThinningPrescription, ApplicationError> handle(ConfirmThinningExecutionCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "thinning.execution.command.null"));
        }
        var found = repository.findByIdForUpdate(new PrescriptionId(command.prescriptionId()));
        if (found.isEmpty()) {
            return Result.failure(ApplicationError.notFound("ThinningPrescription", command.prescriptionId()));
        }
        var prescription = found.get();
        var calibration = calibrationFor(prescription.snapshot().plotId());
        try {
            prescription.confirmExecution(command.executedDate(), command.removedKg(),
                    command.actualRemovalPercentage(), command.laborCrewSize(), command.notes(),
                    calibration, Clock.systemUTC());
        } catch (IllegalArgumentException exception) {
            return Result.failure(ApplicationError.validationError("execution", exception.getMessage()));
        } catch (IllegalStateException exception) {
            return Result.failure(ApplicationError.conflict("ThinningPrescription", exception.getMessage()));
        }
        var saved = repository.save(prescription);
        // Transactional consumers must use AFTER_COMMIT for external side effects, or BEFORE_COMMIT
        // for atomic database changes. Publication here is not a durable message delivery guarantee.
        prescription.domainEvents().forEach(publisher::publishEvent);
        prescription.clearDomainEvents();
        return Result.success(saved);
    }

    /** Fits the caliber model of the plot variety from the real observations stored so far. */
    private CaliberCalibration calibrationFor(PlotId plotId) {
        return externalOrchardService.findPlotVariety(plotId)
                .map(variety -> CaliberModelFittingService.calibrate(variety,
                        observationRepository.findByVariety(variety)))
                .orElseGet(CaliberCalibration::none);
    }
}
