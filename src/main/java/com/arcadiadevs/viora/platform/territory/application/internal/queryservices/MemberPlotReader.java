package com.arcadiadevs.viora.platform.territory.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalProfileService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalTelemetryService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.PlotFacts;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ThinningFacts;
import com.arcadiadevs.viora.platform.territory.application.queryservices.MemberPlotDetailView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.MemberPlotView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.RiskReasonView;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignment;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZone;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.GeoPoint;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotRisk;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotRiskAssessment;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotRiskSignals;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskLevel;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.repositories.PlotSectorAssignmentRepository;
import com.arcadiadevs.viora.platform.territory.domain.repositories.SectorZoneRepository;
import com.arcadiadevs.viora.platform.territory.domain.services.TerritorialRiskAggregationService;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads the plots of members who share with a license from every context that knows something about them, and
 * gives each its color. Nothing is copied: each read asks the other contexts again.
 */
@Component
public class MemberPlotReader {

    /** The order a manager visits plots: the territorial priority, then the closest. */
    public static final Comparator<MemberPlotView> VISIT_ORDER = (first, second) ->
            TerritorialRiskAggregationService.comparePriority(assessmentOf(first), first.distanceKm(),
                    assessmentOf(second), second.distanceKm());

    private final ExternalOrchardService externalOrchardService;
    private final ExternalThinningService externalThinningService;
    private final ExternalTelemetryService externalTelemetryService;
    private final ExternalPhenologyService externalPhenologyService;
    private final ExternalProfileService externalProfileService;
    private final PlotSectorAssignmentRepository plotSectorAssignmentRepository;
    private final SectorZoneRepository sectorZoneRepository;
    private final Clock clock;

    /**
     * Creates the reader.
     *
     * @param externalOrchardService         the plots
     * @param externalThinningService        the sampling, load and prescription
     * @param externalTelemetryService       the incidents and forecasts
     * @param externalPhenologyService       the bearing index
     * @param externalProfileService         the names and phones
     * @param plotSectorAssignmentRepository the sectors of plots
     * @param sectorZoneRepository           the sector list
     * @param clock                          the application clock
     * @throws IllegalArgumentException if any dependency is null
     */
    public MemberPlotReader(ExternalOrchardService externalOrchardService,
                            ExternalThinningService externalThinningService,
                            ExternalTelemetryService externalTelemetryService,
                            ExternalPhenologyService externalPhenologyService,
                            ExternalProfileService externalProfileService,
                            PlotSectorAssignmentRepository plotSectorAssignmentRepository,
                            SectorZoneRepository sectorZoneRepository, Clock clock) {
        if (externalOrchardService == null || externalThinningService == null || externalTelemetryService == null
                || externalPhenologyService == null || externalProfileService == null
                || plotSectorAssignmentRepository == null || sectorZoneRepository == null || clock == null) {
            throw new IllegalArgumentException("territory.member_plot_reader.dependency.null");
        }
        this.externalOrchardService = externalOrchardService;
        this.externalThinningService = externalThinningService;
        this.externalTelemetryService = externalTelemetryService;
        this.externalPhenologyService = externalPhenologyService;
        this.externalProfileService = externalProfileService;
        this.plotSectorAssignmentRepository = plotSectorAssignmentRepository;
        this.sectorZoneRepository = sectorZoneRepository;
        this.clock = clock;
    }

    /**
     * Starts a read of a license: the sectors of its plots and the instant, shared by every plot of the read.
     *
     * @param licenseId the license
     * @return the read context
     */
    public LicenseRead startRead(LicenseId licenseId) {
        var zones = sectorZoneRepository.findAllByLicenseId(licenseId).stream()
                .map(SectorZone::snapshot)
                .collect(Collectors.toMap(snapshot -> snapshot.id(), snapshot -> snapshot.name().value(),
                        (first, second) -> first));
        var sectors = plotSectorAssignmentRepository.findAllByLicenseId(licenseId).stream()
                .collect(Collectors.toMap(assignment -> assignment.snapshot().plotId(), Function.identity(),
                        (first, second) -> first));
        return new LicenseRead(sectors, zones, clock.instant());
    }

    /**
     * Reads the active plots of a member who shares.
     *
     * @param read       the read context
     * @param producerId the member
     * @param origin     where the manager stands; null without GPS
     * @return the plots with their sector and color
     */
    public List<MemberPlotView> plotsOf(LicenseRead read, ProducerId producerId, GeoPoint origin) {
        var ownerName = externalProfileService.findFullName(producerId).orElse(null);
        return externalOrchardService.findActivePlots(producerId).stream()
                .flatMap(plotId -> externalOrchardService.findPlotFacts(plotId).stream())
                .map(facts -> toView(read, producerId, ownerName, facts, origin))
                .toList();
    }

    /**
     * Reads the color of each active plot of a member who shares that has a sector of the list, as the domain sees
     * it. Plots «Por confirmar» or without a sector belong to no sector yet and are left out.
     *
     * @param read       the read context
     * @param producerId the member
     * @return the plots with their sector and color
     */
    public List<PlotRisk> risksOf(LicenseRead read, ProducerId producerId) {
        return externalOrchardService.findActivePlots(producerId).stream()
                .flatMap(plotId -> sectorZoneOf(read, producerId, plotId)
                        .map(zone -> new PlotRisk(plotId, producerId, zone,
                                assess(read, plotId, externalThinningService.findCampaign(plotId,
                                        today(read).getYear()))))
                        .stream())
                .toList();
    }

    /**
     * Reads one plot of a member who shares, with what the manager needs to coordinate a visit.
     *
     * @param read       the read context
     * @param producerId the member
     * @param plotId     the plot
     * @return the plot, or empty if it is not an active plot of the member
     */
    public Optional<MemberPlotDetailView> detailOf(LicenseRead read, ProducerId producerId, PlotId plotId) {
        if (externalOrchardService.findActivePlotOwner(plotId).filter(producerId::equals).isEmpty()) {
            return Optional.empty();
        }
        return externalOrchardService.findPlotFacts(plotId).map(facts -> {
            var thinning = externalThinningService.findCampaign(plotId, today(read).getYear());
            var assessment = assess(read, plotId, thinning);
            var sector = sectorOf(read, producerId, plotId);
            var bearing = externalPhenologyService.findBearingIndex(plotId);
            return new MemberPlotDetailView(plotId.plotId(), facts.name(), producerId.producerId(),
                    externalProfileService.findFullName(producerId).orElse(null),
                    externalProfileService.findPhone(producerId).orElse(null),
                    facts.variety(), facts.areaHa(), sector.zoneName(), sector.status(),
                    assessment.level().name(), reasonsOf(assessment), assessment.overloadPercent(),
                    thinning.map(ThinningFacts::sampledTrees).orElse(0),
                    thinning.map(ThinningFacts::lastSampledOn).orElse(null),
                    thinning.map(ThinningFacts::preThinningFruitsPerShoot).orElse(null),
                    thinning.map(ThinningFacts::targetFruitsPerShoot).orElse(null),
                    thinning.map(ThinningFacts::percentageToRemove).orElse(null),
                    thinning.map(ThinningFacts::windowOpensOn).orElse(null),
                    thinning.map(ThinningFacts::windowClosesOn).orElse(null),
                    thinning.map(ThinningFacts::executedOn).orElse(null),
                    thinning.map(ThinningFacts::estimatedKgPerHaAfterThinning).orElse(null),
                    bearing.map(index -> index.value()).orElse(null),
                    bearing.map(index -> index.category()).orElse(null),
                    read.evaluatedAt());
        });
    }

    private MemberPlotView toView(LicenseRead read, ProducerId producerId, String ownerName, PlotFacts facts,
                                  GeoPoint origin) {
        var plotId = facts.plotId();
        var thinning = externalThinningService.findCampaign(plotId, today(read).getYear());
        var assessment = assess(read, plotId, thinning);
        var sector = sectorOf(read, producerId, plotId);
        var center = facts.center();
        Double distance = origin == null || center == null ? null
                : Math.round(origin.distanceKmTo(center) * 10.0) / 10.0;
        return new MemberPlotView(plotId.plotId(), facts.name(), producerId.producerId(), ownerName,
                facts.variety(), facts.areaHa(), center == null ? null : center.latitude(),
                center == null ? null : center.longitude(), facts.geoJson(), sector.zoneId(), sector.zoneName(),
                sector.status(), sector.proposedName(), assessment.level().name(), reasonsOf(assessment),
                assessment.overloadPercent(), distance, read.evaluatedAt());
    }

    private PlotRiskAssessment assess(LicenseRead read, PlotId plotId, Optional<ThinningFacts> thinning) {
        var sampled = thinning.map(ThinningFacts::issued).orElse(false);
        return TerritorialRiskAggregationService.assess(new PlotRiskSignals(sampled,
                thinning.map(ThinningFacts::preThinningFruitsPerShoot).orElse(null),
                thinning.map(ThinningFacts::targetFruitsPerShoot).orElse(null),
                thinning.map(facts -> facts.executedOn() != null).orElse(false),
                externalTelemetryService.findOpenIncidentTypes(plotId, "CRITICAL"),
                externalTelemetryService.findOpenIncidentTypes(plotId, "WARNING"),
                externalTelemetryService.findForecastMinimums(plotId),
                today(read)));
    }

    private static Sector sectorOf(LicenseRead read, ProducerId producerId, PlotId plotId) {
        // A sector given by a former owner of the plot does not count.
        var assignment = Optional.ofNullable(read.sectors().get(plotId))
                .map(PlotSectorAssignment::snapshot)
                .filter(snapshot -> snapshot.producerId().equals(producerId));
        if (assignment.isEmpty()) {
            return new Sector(null, null, "NONE", null);
        }
        var snapshot = assignment.get();
        if (snapshot.sectorZoneId() == null) {
            return new Sector(null, null, snapshot.status().name(), snapshot.proposedName().value());
        }
        return new Sector(snapshot.sectorZoneId().sectorZoneId(), read.zones().get(snapshot.sectorZoneId()),
                snapshot.status().name(), null);
    }

    private static Optional<SectorZoneId> sectorZoneOf(LicenseRead read, ProducerId producerId, PlotId plotId) {
        return Optional.ofNullable(read.sectors().get(plotId))
                .map(PlotSectorAssignment::snapshot)
                .filter(snapshot -> snapshot.producerId().equals(producerId))
                .map(snapshot -> snapshot.sectorZoneId());
    }

    private static List<RiskReasonView> reasonsOf(PlotRiskAssessment assessment) {
        return assessment.reasons().stream()
                .map(reason -> new RiskReasonView(reason.type().name(), reason.incidentType(), reason.value(),
                        reason.date()))
                .toList();
    }

    private static PlotRiskAssessment assessmentOf(MemberPlotView view) {
        return new PlotRiskAssessment(RiskLevel.valueOf(view.riskLevel()), List.of(), view.overloadPercent());
    }

    private static LocalDate today(LicenseRead read) {
        return LocalDate.ofInstant(read.evaluatedAt(), ProducerTimeZone.ZONE);
    }

    /**
     * What one read shares among the plots it reads.
     *
     * @param sectors     the sector of each plot of the license
     * @param zones       the names of the sectors of the license
     * @param evaluatedAt the instant of the read
     */
    public record LicenseRead(Map<PlotId, PlotSectorAssignment> sectors, Map<SectorZoneId, String> zones,
                           Instant evaluatedAt) {
    }

    private record Sector(String zoneId, String zoneName, String status, String proposedName) {
    }
}
