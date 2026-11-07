package com.arcadiadevs.viora.platform.territory.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalProfileService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.SponsoredMember;
import com.arcadiadevs.viora.platform.territory.application.internal.security.LicenseAccessPolicy;
import com.arcadiadevs.viora.platform.territory.application.queryservices.SectorAlertQueryService;
import com.arcadiadevs.viora.platform.territory.application.queryservices.SectorAlertView;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorAlert;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZone;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZoneSnapshot;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetSectorAlertsByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.DataSharingState;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.repositories.SectorAlertRepository;
import com.arcadiadevs.viora.platform.territory.domain.repositories.SectorZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads the manager's alert center: the open alerts of their sectors, critical first, and the ones normalized in the
 * last 7 days. The plot and producer that lead an alert are named only while that producer still shares.
 */
@Service
@Transactional(readOnly = true)
public class SectorAlertQueryServiceImpl implements SectorAlertQueryService {

    /** How far back normalized alerts are shown. */
    public static final Duration NORMALIZED_WINDOW = Duration.ofDays(7);

    private final LicenseAccessPolicy licenseAccessPolicy;
    private final SectorAlertRepository sectorAlertRepository;
    private final SectorZoneRepository sectorZoneRepository;
    private final ExternalSubscriptionService externalSubscriptionService;
    private final ExternalOrchardService externalOrchardService;
    private final ExternalProfileService externalProfileService;
    private final Clock clock;

    /**
     * Creates the query service.
     *
     * @param licenseAccessPolicy         the license each manager is in charge of
     * @param sectorAlertRepository       the sector alerts
     * @param sectorZoneRepository        the sector names
     * @param externalSubscriptionService the members and whether they share
     * @param externalOrchardService      the plot names
     * @param externalProfileService      the member names
     * @param clock                       the application clock
     * @throws IllegalArgumentException if any dependency is null
     */
    public SectorAlertQueryServiceImpl(LicenseAccessPolicy licenseAccessPolicy,
                                       SectorAlertRepository sectorAlertRepository,
                                       SectorZoneRepository sectorZoneRepository,
                                       ExternalSubscriptionService externalSubscriptionService,
                                       ExternalOrchardService externalOrchardService,
                                       ExternalProfileService externalProfileService, Clock clock) {
        if (licenseAccessPolicy == null || sectorAlertRepository == null || sectorZoneRepository == null
                || externalSubscriptionService == null || externalOrchardService == null
                || externalProfileService == null || clock == null) {
            throw new IllegalArgumentException("territory.sector_alert_queries.dependency.null");
        }
        this.licenseAccessPolicy = licenseAccessPolicy;
        this.sectorAlertRepository = sectorAlertRepository;
        this.sectorZoneRepository = sectorZoneRepository;
        this.externalSubscriptionService = externalSubscriptionService;
        this.externalOrchardService = externalOrchardService;
        this.externalProfileService = externalProfileService;
        this.clock = clock;
    }

    @Override
    public Result<List<SectorAlertView>, ApplicationError> handle(GetSectorAlertsByManagerIdQuery query) {
        var scope = licenseAccessPolicy.resolveManager(query.managerId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var licenseId = scope.success().orElseThrow().licenseId();
        Map<SectorZoneId, String> zones = sectorZoneRepository.findAllByLicenseId(licenseId).stream()
                .map(SectorZone::snapshot)
                .collect(Collectors.toMap(SectorZoneSnapshot::id, snapshot -> snapshot.name().value(),
                        (first, second) -> first));
        Set<ProducerId> sharing = externalSubscriptionService.findMembers(licenseId).stream()
                .filter(member -> member.sharing() == DataSharingState.SHARING)
                .map(SponsoredMember::producerId)
                .collect(Collectors.toSet());
        var since = clock.instant().minus(NORMALIZED_WINDOW);
        return Result.success(sectorAlertRepository.findOpenOrNormalizedSince(licenseId, since).stream()
                .sorted(Comparator.comparing((SectorAlert alert) -> !alert.isOpen())
                        .thenComparing(alert -> alert.snapshot().type().severity())
                        .thenComparing(alert -> alert.snapshot().updatedAt(), Comparator.reverseOrder()))
                .map(alert -> toView(alert, zones, sharing))
                .toList());
    }

    private SectorAlertView toView(SectorAlert alert, Map<SectorZoneId, String> zones, Set<ProducerId> sharing) {
        var snapshot = alert.snapshot();
        var facts = snapshot.facts();
        // A producer who stopped sharing is no longer named, even in an alert they led.
        var leadShares = facts.leadProducerId() != null && sharing.contains(facts.leadProducerId());
        return new SectorAlertView(snapshot.id().sectorAlertId(), snapshot.sectorZoneId().sectorZoneId(),
                zones.get(snapshot.sectorZoneId()), snapshot.type().name(), snapshot.type().severity().name(),
                alert.isOpen() ? "ACTIVE" : "NORMALIZED", snapshot.openedAt(), snapshot.updatedAt(),
                snapshot.normalizedAt(),
                snapshot.levelAtNormalize() == null ? null : snapshot.levelAtNormalize().name(),
                facts.affectedPlots(), facts.sectorPlots(),
                leadShares ? facts.leadPlotId().plotId() : null,
                leadShares ? externalOrchardService.findPlotName(facts.leadPlotId()).orElse(null) : null,
                leadShares ? externalProfileService.findFullName(facts.leadProducerId()).orElse(null) : null,
                facts.leadReason() == null ? null : facts.leadReason().name(), facts.leadValue(), facts.leadDate());
    }
}
