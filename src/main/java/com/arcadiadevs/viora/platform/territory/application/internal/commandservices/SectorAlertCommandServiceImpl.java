package com.arcadiadevs.viora.platform.territory.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.commandservices.SectorAlertCommandService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.territory.application.internal.queryservices.MemberPlotReader;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorAlert;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.EvaluateSectorAlertsCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.DataSharingState;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotRisk;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneId;
import com.arcadiadevs.viora.platform.territory.domain.repositories.SectorAlertRepository;
import com.arcadiadevs.viora.platform.territory.domain.services.SectorAlertPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Application service keeping the sector alerts of a license in step with the colors of its member plots: it opens
 * the alerts a sector deserves and has not got, updates the ones still deserved and normalizes the rest.
 * <p>Only plots of members who share and that have a sector of the list count: a plot «Por confirmar» belongs to no
 * sector yet.</p>
 */
@Service
public class SectorAlertCommandServiceImpl implements SectorAlertCommandService {

    private final SectorAlertRepository sectorAlertRepository;
    private final ExternalSubscriptionService externalSubscriptionService;
    private final MemberPlotReader memberPlotReader;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param sectorAlertRepository       the sector alerts
     * @param externalSubscriptionService the members and whether they share
     * @param memberPlotReader            the colors of member plots
     * @param clock                       the application clock
     * @throws IllegalArgumentException if any dependency is null
     */
    public SectorAlertCommandServiceImpl(SectorAlertRepository sectorAlertRepository,
                                         ExternalSubscriptionService externalSubscriptionService,
                                         MemberPlotReader memberPlotReader, Clock clock) {
        if (sectorAlertRepository == null || externalSubscriptionService == null || memberPlotReader == null
                || clock == null) {
            throw new IllegalArgumentException("territory.sector_alert_service.dependency.null");
        }
        this.sectorAlertRepository = sectorAlertRepository;
        this.externalSubscriptionService = externalSubscriptionService;
        this.memberPlotReader = memberPlotReader;
        this.clock = clock;
    }

    @Override
    @Transactional
    public Result<Integer, ApplicationError> handle(EvaluateSectorAlertsCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "territory.sector_alert.command.null"));
        }
        final LicenseId licenseId;
        try {
            licenseId = new LicenseId(command.licenseId());
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("licenseId", ex.getMessage()));
        }
        var now = clock.instant();
        var read = memberPlotReader.startRead(licenseId);
        Map<SectorZoneId, List<PlotRisk>> bySector = externalSubscriptionService.findMembers(licenseId).stream()
                .filter(member -> member.sharing() == DataSharingState.SHARING)
                .flatMap(member -> memberPlotReader.risksOf(read, member.producerId()).stream())
                .collect(Collectors.groupingBy(PlotRisk::sectorZoneId));
        var changes = 0;
        var open = sectorAlertRepository.findOpenByLicenseId(licenseId);
        for (var alert : open) {
            var snapshot = alert.snapshot();
            var plots = bySector.getOrDefault(snapshot.sectorZoneId(), List.of());
            var deserved = SectorAlertPolicy.alertsFor(plots).get(snapshot.type());
            var changed = deserved == null ? alert.normalize(SectorAlertPolicy.levelOf(plots), now)
                    : alert.refresh(deserved, now);
            if (changed) {
                sectorAlertRepository.save(alert);
                changes++;
            }
        }
        for (var sector : bySector.entrySet()) {
            for (var deserved : SectorAlertPolicy.alertsFor(sector.getValue()).entrySet()) {
                // At most one open alert of each type per sector.
                var alreadyOpen = open.stream().anyMatch(alert ->
                        alert.snapshot().sectorZoneId().equals(sector.getKey())
                                && alert.snapshot().type() == deserved.getKey());
                if (!alreadyOpen) {
                    sectorAlertRepository.save(SectorAlert.open(licenseId, sector.getKey(), deserved.getKey(),
                            deserved.getValue(), now));
                    changes++;
                }
            }
        }
        return Result.success(changes);
    }
}
