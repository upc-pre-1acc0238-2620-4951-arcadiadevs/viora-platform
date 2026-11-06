package com.arcadiadevs.viora.platform.territory.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.internal.scope.LicenseScope;
import com.arcadiadevs.viora.platform.territory.application.internal.scope.LicenseScopeResolver;
import com.arcadiadevs.viora.platform.territory.application.queryservices.SectorZoneQueryService;
import com.arcadiadevs.viora.platform.territory.application.queryservices.SectorZoneView;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignment;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignmentSnapshot;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZone;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetSectorZoneByIdAndUserIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetSectorZonesByUserIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorStatus;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.repositories.SectorZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reads the sector list of a license with how many member plots each sector has.
 */
@Service
@Transactional(readOnly = true)
public class SectorZoneQueryServiceImpl implements SectorZoneQueryService {

    private final SectorZoneRepository sectorZoneRepository;
    private final LicenseScopeResolver licenseScopeResolver;
    private final VisiblePlotSectors visiblePlotSectors;

    /**
     * Creates the query service.
     *
     * @param sectorZoneRepository the sector zones
     * @param licenseScopeResolver the license each user reaches
     * @param visiblePlotSectors   the plot sectors each user sees
     * @throws IllegalArgumentException if any dependency is null
     */
    public SectorZoneQueryServiceImpl(SectorZoneRepository sectorZoneRepository,
                                      LicenseScopeResolver licenseScopeResolver,
                                      VisiblePlotSectors visiblePlotSectors) {
        if (sectorZoneRepository == null || licenseScopeResolver == null || visiblePlotSectors == null) {
            throw new IllegalArgumentException("territory.sector_zone_queries.dependency.null");
        }
        this.sectorZoneRepository = sectorZoneRepository;
        this.licenseScopeResolver = licenseScopeResolver;
        this.visiblePlotSectors = visiblePlotSectors;
    }

    @Override
    public Result<List<SectorZoneView>, ApplicationError> handle(GetSectorZonesByUserIdQuery query) {
        var scope = licenseScopeResolver.resolve(query.userId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var counts = plotCounts(scope.success().orElseThrow());
        return Result.success(sectorZoneRepository.findAllByLicenseId(scope.success().orElseThrow().licenseId())
                .stream()
                .sorted(Comparator.comparing(zone -> zone.snapshot().name().normalized()))
                .map(zone -> toView(zone, counts))
                .toList());
    }

    @Override
    public Result<SectorZoneView, ApplicationError> handle(GetSectorZoneByIdAndUserIdQuery query) {
        var scope = licenseScopeResolver.resolve(query.userId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var licenseScope = scope.success().orElseThrow();
        return sectorZoneRepository.findByIdAndLicenseId(query.sectorZoneId(), licenseScope.licenseId())
                .<Result<SectorZoneView, ApplicationError>>map(zone ->
                        Result.success(toView(zone, plotCounts(licenseScope))))
                .orElseGet(() -> Result.failure(ApplicationError.notFound("sector-zone",
                        query.sectorZoneId().sectorZoneId())));
    }

    private Map<SectorZoneId, Long> plotCounts(LicenseScope scope) {
        // A member sees the same totals as the manager: how many member plots each sector of the list has.
        return visiblePlotSectors.ofLicense(scope.licenseId()).stream()
                .map(PlotSectorAssignment::snapshot)
                .filter(snapshot -> snapshot.status() == PlotSectorStatus.ASSIGNED)
                .collect(Collectors.groupingBy(PlotSectorAssignmentSnapshot::sectorZoneId, Collectors.counting()));
    }

    private static SectorZoneView toView(SectorZone zone, Map<SectorZoneId, Long> counts) {
        var snapshot = zone.snapshot();
        return new SectorZoneView(snapshot.id().sectorZoneId(), snapshot.name().value(),
                counts.getOrDefault(snapshot.id(), 0L).intValue());
    }
}
