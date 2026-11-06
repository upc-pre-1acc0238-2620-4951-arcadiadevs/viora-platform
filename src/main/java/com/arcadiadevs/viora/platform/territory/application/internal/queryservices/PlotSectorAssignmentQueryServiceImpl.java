package com.arcadiadevs.viora.platform.territory.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalProfileService;
import com.arcadiadevs.viora.platform.territory.application.internal.scope.LicenseScopeResolver;
import com.arcadiadevs.viora.platform.territory.application.queryservices.PlotSectorAssignmentQueryService;
import com.arcadiadevs.viora.platform.territory.application.queryservices.PlotSectorAssignmentView;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignment;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZone;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZoneSnapshot;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetPlotSectorAssignmentByPlotIdAndUserIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetPlotSectorAssignmentsByUserIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorStatus;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;
import com.arcadiadevs.viora.platform.territory.domain.repositories.SectorZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reads the sectors of member plots, naming each plot, its owner and its sector.
 */
@Service
@Transactional(readOnly = true)
public class PlotSectorAssignmentQueryServiceImpl implements PlotSectorAssignmentQueryService {

    private final SectorZoneRepository sectorZoneRepository;
    private final LicenseScopeResolver licenseScopeResolver;
    private final VisiblePlotSectors visiblePlotSectors;
    private final ExternalOrchardService externalOrchardService;
    private final ExternalProfileService externalProfileService;

    /**
     * Creates the query service.
     *
     * @param sectorZoneRepository   the sector zones
     * @param licenseScopeResolver   the license each user reaches
     * @param visiblePlotSectors     the plot sectors each user sees
     * @param externalOrchardService the plot names
     * @param externalProfileService the member names
     * @throws IllegalArgumentException if any dependency is null
     */
    public PlotSectorAssignmentQueryServiceImpl(SectorZoneRepository sectorZoneRepository,
                                                LicenseScopeResolver licenseScopeResolver,
                                                VisiblePlotSectors visiblePlotSectors,
                                                ExternalOrchardService externalOrchardService,
                                                ExternalProfileService externalProfileService) {
        if (sectorZoneRepository == null || licenseScopeResolver == null || visiblePlotSectors == null
                || externalOrchardService == null || externalProfileService == null) {
            throw new IllegalArgumentException("territory.plot_sector_queries.dependency.null");
        }
        this.sectorZoneRepository = sectorZoneRepository;
        this.licenseScopeResolver = licenseScopeResolver;
        this.visiblePlotSectors = visiblePlotSectors;
        this.externalOrchardService = externalOrchardService;
        this.externalProfileService = externalProfileService;
    }

    @Override
    public Result<List<PlotSectorAssignmentView>, ApplicationError> handle(
            GetPlotSectorAssignmentsByUserIdQuery query) {
        var scope = licenseScopeResolver.resolve(query.userId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var licenseScope = scope.success().orElseThrow();
        var zones = zonesOf(licenseScope.licenseId());
        return Result.success(visiblePlotSectors.of(licenseScope).stream()
                .filter(assignment -> query.status() == null || assignment.snapshot().status() == query.status())
                .map(assignment -> toView(assignment, zones))
                .sorted(Comparator.comparing((PlotSectorAssignmentView view) ->
                                !PlotSectorStatus.PENDING.name().equals(view.status()))
                        .thenComparing(view -> view.plotName() == null ? "" : view.plotName()))
                .toList());
    }

    @Override
    public Result<PlotSectorAssignmentView, ApplicationError> handle(
            GetPlotSectorAssignmentByPlotIdAndUserIdQuery query) {
        var scope = licenseScopeResolver.resolve(query.userId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var licenseScope = scope.success().orElseThrow();
        return visiblePlotSectors.of(licenseScope).stream()
                .filter(assignment -> assignment.snapshot().plotId().equals(query.plotId()))
                .findFirst()
                .<Result<PlotSectorAssignmentView, ApplicationError>>map(assignment ->
                        Result.success(toView(assignment, zonesOf(licenseScope.licenseId()))))
                .orElseGet(() -> Result.failure(ApplicationError.notFound("plot-sector-assignment",
                        query.plotId().plotId())));
    }

    private Map<SectorZoneId, SectorZoneName> zonesOf(LicenseId licenseId) {
        return sectorZoneRepository.findAllByLicenseId(licenseId).stream()
                .map(SectorZone::snapshot)
                .collect(Collectors.toMap(SectorZoneSnapshot::id, SectorZoneSnapshot::name,
                        (first, second) -> first));
    }

    private PlotSectorAssignmentView toView(PlotSectorAssignment assignment, Map<SectorZoneId, SectorZoneName> zones) {
        var snapshot = assignment.snapshot();
        return new PlotSectorAssignmentView(snapshot.plotId().plotId(),
                externalOrchardService.findPlotName(snapshot.plotId()).orElse(null),
                snapshot.producerId().producerId(),
                externalProfileService.findFullName(snapshot.producerId()).orElse(null),
                snapshot.status().name(),
                snapshot.sectorZoneId() == null ? null : snapshot.sectorZoneId().sectorZoneId(),
                snapshot.sectorZoneId() == null || !zones.containsKey(snapshot.sectorZoneId()) ? null
                        : zones.get(snapshot.sectorZoneId()).value(),
                snapshot.proposedName() == null ? null : snapshot.proposedName().value(),
                snapshot.updatedAt());
    }
}
