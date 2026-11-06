package com.arcadiadevs.viora.platform.territory.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.commandservices.PlotSectorAssignmentCommandService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.territory.application.internal.security.LicenseAccessPolicy;
import com.arcadiadevs.viora.platform.territory.domain.exceptions.PlotSectorNotPendingException;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignment;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZone;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.AssignPlotSectorCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.ResolvePlotSectorCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;
import com.arcadiadevs.viora.platform.territory.domain.repositories.PlotSectorAssignmentRepository;
import com.arcadiadevs.viora.platform.territory.domain.repositories.SectorZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Map;
import java.util.Optional;

/**
 * Application service through which members give their plots a sector and managers review the ones «Por confirmar».
 * The plot's row is locked while it changes.
 */
@Service
public class PlotSectorAssignmentCommandServiceImpl implements PlotSectorAssignmentCommandService {

    private static final String RESOURCE = "plot-sector-assignment";

    private final PlotSectorAssignmentRepository plotSectorAssignmentRepository;
    private final SectorZoneRepository sectorZoneRepository;
    private final LicenseAccessPolicy licenseAccessPolicy;
    private final ExternalOrchardService externalOrchardService;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param plotSectorAssignmentRepository the sectors given to plots
     * @param sectorZoneRepository           the sector zones
     * @param licenseAccessPolicy            the license each user reaches
     * @param externalOrchardService         the owners of plots
     * @param clock                          the application clock
     * @throws IllegalArgumentException if any dependency is null
     */
    public PlotSectorAssignmentCommandServiceImpl(PlotSectorAssignmentRepository plotSectorAssignmentRepository,
                                                  SectorZoneRepository sectorZoneRepository,
                                                  LicenseAccessPolicy licenseAccessPolicy,
                                                  ExternalOrchardService externalOrchardService, Clock clock) {
        if (plotSectorAssignmentRepository == null || sectorZoneRepository == null || licenseAccessPolicy == null
                || externalOrchardService == null || clock == null) {
            throw new IllegalArgumentException("territory.plot_sector_service.dependency.null");
        }
        this.plotSectorAssignmentRepository = plotSectorAssignmentRepository;
        this.sectorZoneRepository = sectorZoneRepository;
        this.licenseAccessPolicy = licenseAccessPolicy;
        this.externalOrchardService = externalOrchardService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<PlotSectorAssignment, ApplicationError> handle(AssignPlotSectorCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "territory.plot_sector.command.null"));
        }
        try {
            var producerId = new ProducerId(command.producerId());
            var scope = licenseAccessPolicy.resolveMember(producerId);
            if (scope.isFailure()) {
                return Result.failure(scope.failure().orElseThrow());
            }
            var licenseId = scope.success().orElseThrow().licenseId();
            var plotId = new PlotId(command.plotId());
            // Another producer's plot, or an archived one, answers the same as one that does not exist.
            if (externalOrchardService.findActivePlotOwner(plotId).filter(producerId::equals).isEmpty()) {
                return Result.failure(ApplicationError.notFound("plot", plotId.plotId()));
            }
            if ((command.sectorZoneId() == null) == (command.name() == null)) {
                return Result.failure(ApplicationError.validationError("sector",
                        "territory.plot_sector.choice.one_of"));
            }
            Optional<SectorZone> zone;
            SectorZoneName written = null;
            if (command.sectorZoneId() != null) {
                var zoneId = new SectorZoneId(command.sectorZoneId());
                zone = sectorZoneRepository.findByIdAndLicenseId(zoneId, licenseId);
                if (zone.isEmpty()) {
                    return Result.failure(ApplicationError.notFound("sector-zone", zoneId.sectorZoneId()));
                }
            } else {
                written = new SectorZoneName(command.name());
                // A written name that matches the list, ignoring accents and case, is that sector.
                zone = sectorZoneRepository.findByLicenseIdAndName(licenseId, written);
            }
            var now = clock.instant();
            var chosen = zone.orElse(null);
            var proposed = chosen == null ? written : null;
            var assignment = plotSectorAssignmentRepository.findByLicenseIdAndPlotIdForUpdate(licenseId, plotId);
            if (assignment.isPresent()) {
                assignment.get().change(chosen, proposed, now);
                return Result.success(plotSectorAssignmentRepository.save(assignment.get()));
            }
            return Result.success(plotSectorAssignmentRepository.save(
                    PlotSectorAssignment.start(licenseId, plotId, producerId, chosen, proposed, now)));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    @Override
    @Transactional
    public Result<PlotSectorAssignment, ApplicationError> handle(ResolvePlotSectorCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "territory.plot_sector.command.null"));
        }
        try {
            var scope = licenseAccessPolicy.resolveManager(new ManagerId(command.managerId()));
            if (scope.isFailure()) {
                return Result.failure(scope.failure().orElseThrow());
            }
            var licenseId = scope.success().orElseThrow().licenseId();
            var plotId = new PlotId(command.plotId());
            var assignment = plotSectorAssignmentRepository.findByLicenseIdAndPlotIdForUpdate(licenseId, plotId);
            if (assignment.isEmpty()) {
                return Result.failure(ApplicationError.notFound(RESOURCE, plotId.plotId()));
            }
            if (!assignment.get().isPending()) {
                return notPending();
            }
            var written = assignment.get().snapshot().proposedName();
            var zone = command.sectorZoneId() == null
                    ? Optional.of(existingOrNew(licenseId, written))
                    : sectorZoneRepository.findByIdAndLicenseId(new SectorZoneId(command.sectorZoneId()), licenseId);
            if (zone.isEmpty()) {
                return Result.failure(ApplicationError.notFound("sector-zone", command.sectorZoneId()));
            }
            var now = clock.instant();
            assignment.get().resolve(zone.get(), now);
            var resolved = plotSectorAssignmentRepository.save(assignment.get());
            // Every other plot where the same name was written means the same sector.
            for (var sameName : plotSectorAssignmentRepository.findAllPendingByLicenseIdAndNameForUpdate(
                    licenseId, written)) {
                sameName.resolve(zone.get(), now);
                plotSectorAssignmentRepository.save(sameName);
            }
            return Result.success(resolved);
        } catch (PlotSectorNotPendingException ex) {
            return notPending();
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    private SectorZone existingOrNew(LicenseId licenseId, SectorZoneName name) {
        return sectorZoneRepository.findByLicenseIdAndName(licenseId, name)
                .orElseGet(() -> sectorZoneRepository.save(SectorZone.create(licenseId, name)));
    }

    private static Result<PlotSectorAssignment, ApplicationError> notPending() {
        return Result.failure(ApplicationError.conflict(RESOURCE, PlotSectorNotPendingException.MESSAGE_KEY,
                Map.of("reason", PlotSectorNotPendingException.MESSAGE_KEY)));
    }
}
