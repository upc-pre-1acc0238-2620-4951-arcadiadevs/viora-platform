package com.arcadiadevs.viora.platform.territory.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.commandservices.SectorZoneCommandService;
import com.arcadiadevs.viora.platform.territory.application.internal.scope.LicenseScopeResolver;
import com.arcadiadevs.viora.platform.territory.domain.exceptions.SectorZoneNameTakenException;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZone;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.CreateSectorZoneCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.RenameSectorZoneCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;
import com.arcadiadevs.viora.platform.territory.domain.repositories.SectorZoneRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Application service adding and renaming the sectors of a license. Each write is its own transaction: a name taken
 * at the same moment by another request is caught by the database and answered as a 409.
 */
@Service
public class SectorZoneCommandServiceImpl implements SectorZoneCommandService {

    private static final String RESOURCE = "sector-zone";

    private final SectorZoneRepository sectorZoneRepository;
    private final LicenseScopeResolver licenseScopeResolver;

    /**
     * Creates the service.
     *
     * @param sectorZoneRepository the sector zones
     * @param licenseScopeResolver the license each manager is in charge of
     * @throws IllegalArgumentException if any dependency is null
     */
    public SectorZoneCommandServiceImpl(SectorZoneRepository sectorZoneRepository,
                                        LicenseScopeResolver licenseScopeResolver) {
        if (sectorZoneRepository == null || licenseScopeResolver == null) {
            throw new IllegalArgumentException("territory.sector_zone_service.dependency.null");
        }
        this.sectorZoneRepository = sectorZoneRepository;
        this.licenseScopeResolver = licenseScopeResolver;
    }

    @Override
    public Result<SectorZone, ApplicationError> handle(CreateSectorZoneCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "territory.sector_zone.command.null"));
        }
        try {
            var scope = licenseScopeResolver.resolveManager(new ManagerId(command.managerId()));
            if (scope.isFailure()) {
                return Result.failure(scope.failure().orElseThrow());
            }
            var licenseId = scope.success().orElseThrow().licenseId();
            var name = new SectorZoneName(command.name());
            if (sectorZoneRepository.findByLicenseIdAndName(licenseId, name).isPresent()) {
                return nameTaken();
            }
            return Result.success(sectorZoneRepository.save(SectorZone.create(licenseId, name)));
        } catch (SectorZoneNameTakenException ex) {
            return nameTaken();
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("name", ex.getMessage()));
        }
    }

    @Override
    public Result<SectorZone, ApplicationError> handle(RenameSectorZoneCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "territory.sector_zone.command.null"));
        }
        try {
            var scope = licenseScopeResolver.resolveManager(new ManagerId(command.managerId()));
            if (scope.isFailure()) {
                return Result.failure(scope.failure().orElseThrow());
            }
            var licenseId = scope.success().orElseThrow().licenseId();
            var zoneId = new SectorZoneId(command.sectorZoneId());
            var zone = sectorZoneRepository.findByIdAndLicenseId(zoneId, licenseId);
            // A zone of another license answers the same as one that does not exist.
            if (zone.isEmpty()) {
                return Result.failure(ApplicationError.notFound(RESOURCE, zoneId.sectorZoneId()));
            }
            var name = new SectorZoneName(command.name());
            var homonym = sectorZoneRepository.findByLicenseIdAndName(licenseId, name);
            if (homonym.isPresent() && !homonym.get().snapshot().id().equals(zoneId)) {
                return nameTaken();
            }
            zone.get().rename(name);
            return Result.success(sectorZoneRepository.save(zone.get()));
        } catch (SectorZoneNameTakenException ex) {
            return nameTaken();
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("name", ex.getMessage()));
        }
    }

    private static Result<SectorZone, ApplicationError> nameTaken() {
        return Result.failure(ApplicationError.conflict(RESOURCE, SectorZoneNameTakenException.MESSAGE_KEY,
                Map.of("reason", SectorZoneNameTakenException.MESSAGE_KEY)));
    }
}
