package com.arcadiadevs.viora.platform.territory.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSettlementService.SettledHarvestFacts;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSettlementService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalThinningService.YieldOutlookFacts;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.territory.application.internal.queryservices.MemberPlotReader.PlotPlacement;
import com.arcadiadevs.viora.platform.territory.application.internal.security.LicenseAccessPolicy;
import com.arcadiadevs.viora.platform.territory.application.queryservices.IntakeForecastQueryService;
import com.arcadiadevs.viora.platform.territory.application.queryservices.IntakeForecastView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.SectorIntakeView;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetIntakeForecastByCampaignYearAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetIntakeForecastsByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.DataSharingState;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.IntakeProjection;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSettlement;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotYield;
import com.arcadiadevs.viora.platform.territory.domain.services.YieldAggregationDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reads the intake of a manager's license from the plots of the members who share: the campaign in progress is
 * projected from Thinning's yield estimates, a past one adds up what Settlement recorded. A harvest settled before
 * its member joined is private and never counts.
 */
@Service
@Transactional(readOnly = true)
public class IntakeForecastQueryServiceImpl implements IntakeForecastQueryService {

    private static final String PROJECTION = "PROJECTION";
    private static final String SETTLED = "SETTLED";

    private final LicenseAccessPolicy licenseAccessPolicy;
    private final ExternalSubscriptionService externalSubscriptionService;
    private final ExternalThinningService externalThinningService;
    private final ExternalSettlementService externalSettlementService;
    private final MemberPlotReader memberPlotReader;
    private final Clock clock;

    /**
     * Creates the query service.
     *
     * @param licenseAccessPolicy         the license each manager is in charge of
     * @param externalSubscriptionService the members and whether they share
     * @param externalThinningService     the yield estimates
     * @param externalSettlementService   the settled harvests
     * @param memberPlotReader            the member plots and their sectors
     * @param clock                       the application clock
     * @throws IllegalArgumentException if any dependency is null
     */
    public IntakeForecastQueryServiceImpl(LicenseAccessPolicy licenseAccessPolicy,
                                          ExternalSubscriptionService externalSubscriptionService,
                                          ExternalThinningService externalThinningService,
                                          ExternalSettlementService externalSettlementService,
                                          MemberPlotReader memberPlotReader, Clock clock) {
        if (licenseAccessPolicy == null || externalSubscriptionService == null || externalThinningService == null
                || externalSettlementService == null || memberPlotReader == null || clock == null) {
            throw new IllegalArgumentException("territory.intake_queries.dependency.null");
        }
        this.licenseAccessPolicy = licenseAccessPolicy;
        this.externalSubscriptionService = externalSubscriptionService;
        this.externalThinningService = externalThinningService;
        this.externalSettlementService = externalSettlementService;
        this.memberPlotReader = memberPlotReader;
        this.clock = clock;
    }

    @Override
    public Result<List<IntakeForecastView>, ApplicationError> handle(GetIntakeForecastsByManagerIdQuery query) {
        var scope = licenseAccessPolicy.resolveManager(query.managerId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var plots = memberPlots(scope.success().orElseThrow().licenseId());
        var current = currentCampaign();
        var years = new TreeSet<Integer>(Comparator.reverseOrder());
        years.add(current);
        plots.forEach(plot -> plot.harvests().stream()
                .filter(harvest -> harvest.campaignYear() < current)
                .forEach(harvest -> years.add(harvest.campaignYear())));
        return Result.success(years.stream()
                .map(year -> toView(year, intakeOf(year, current, plots), false))
                .toList());
    }

    @Override
    public Result<IntakeForecastView, ApplicationError> handle(
            GetIntakeForecastByCampaignYearAndManagerIdQuery query) {
        var scope = licenseAccessPolicy.resolveManager(query.managerId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var current = currentCampaign();
        if (query.campaignYear() > current) {
            return Result.failure(ApplicationError.notFound("intake-forecast",
                    String.valueOf(query.campaignYear())));
        }
        var licenseId = scope.success().orElseThrow().licenseId();
        var plots = memberPlots(licenseId);
        return Result.success(toView(query.campaignYear(), intakeOf(query.campaignYear(), current, plots), true,
                zoneNames(licenseId)));
    }

    private IntakeProjection intakeOf(int year, int current, List<MemberPlot> plots) {
        if (year == current) {
            return YieldAggregationDomainService.project(plots.stream()
                    .map(plot -> {
                        var outlook = externalThinningService.findYieldOutlook(plot.placement().plotId(), year);
                        var kgPerHa = outlook.map(YieldOutlookFacts::expectedKgPerHa).orElse(null);
                        var area = plot.placement().areaHa();
                        return new PlotYield(plot.placement().plotId(), plot.placement().placement(),
                                outlook.map(YieldOutlookFacts::sampled).orElse(false),
                                kgPerHa == null || area == null ? null : kgPerHa * area, greenShare(plot.harvests()));
                    })
                    .toList());
        }
        return YieldAggregationDomainService.settled(plots.stream()
                .map(plot -> plot.harvests().stream()
                        .filter(harvest -> harvest.campaignYear() == year)
                        // A harvest settled before the member joined is private.
                        .filter(harvest -> !harvest.settledAt().isBefore(plot.memberSince()))
                        .findFirst()
                        .map(harvest -> new PlotSettlement(plot.placement().plotId(), plot.placement().placement(),
                                true, harvest.greenKg(), harvest.blackKg()))
                        .orElse(new PlotSettlement(plot.placement().plotId(), plot.placement().placement(), false,
                                0, 0)))
                .toList());
    }

    private List<MemberPlot> memberPlots(LicenseId licenseId) {
        var read = memberPlotReader.startRead(licenseId);
        return externalSubscriptionService.findMembers(licenseId).stream()
                .filter(member -> member.sharing() == DataSharingState.SHARING)
                .flatMap(member -> memberPlotReader.placementsOf(read, member.producerId()).stream()
                        .map(placement -> new MemberPlot(placement, member.memberSince(),
                                externalSettlementService.findSettledHarvests(placement.plotId()))))
                .toList();
    }

    private Map<String, String> zoneNames(LicenseId licenseId) {
        return memberPlotReader.startRead(licenseId).zones().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().sectorZoneId(), Map.Entry::getValue));
    }

    private int currentCampaign() {
        return LocalDate.ofInstant(clock.instant(), ProducerTimeZone.ZONE).getYear();
    }

    private IntakeForecastView toView(int year, IntakeProjection intake, boolean withSectors) {
        return toView(year, intake, withSectors, Map.of());
    }

    private IntakeForecastView toView(int year, IntakeProjection intake, boolean withSectors,
                                      Map<String, String> zoneNames) {
        Function<Double, Double> tons = kilograms -> kilograms == null ? null
                : Math.round(kilograms / 100.0) / 10.0;
        var sectors = !withSectors ? List.<SectorIntakeView>of() : intake.sectors().stream()
                .map(sector -> {
                    var zoneId = sector.sector().sectorZoneId() == null ? null
                            : sector.sector().sectorZoneId().sectorZoneId();
                    return new SectorIntakeView(zoneId, zoneId == null ? null : zoneNames.get(zoneId),
                            sector.sector().status(), sector.plots(), sector.sampledPlots(),
                            tons.apply(sector.kilograms()));
                })
                .sorted(Comparator.comparing((SectorIntakeView view) -> view.sectorZoneId() == null)
                        .thenComparing(view -> view.tons() == null ? 0.0 : -view.tons()))
                .toList();
        var currentKind = year == currentCampaign() ? PROJECTION : SETTLED;
        return new IntakeForecastView(year, currentKind, intake.status().name(), tons.apply(intake.totalKg()),
                tons.apply(intake.greenKg()), tons.apply(intake.blackKg()), tons.apply(intake.unclassifiedKg()),
                intake.plots(), intake.sampledPlots(), intake.estimablePlots(), intake.coveragePercent(),
                YieldAggregationDomainService.PRELIMINARY_THRESHOLD_PERCENT, sectors, clock.instant());
    }

    private static Double greenShare(List<SettledHarvestFacts> harvests) {
        var green = harvests.stream().mapToDouble(SettledHarvestFacts::greenKg).sum();
        var total = green + harvests.stream().mapToDouble(SettledHarvestFacts::blackKg).sum();
        return total <= 0 ? null : green / total;
    }

    /**
     * A plot of a member who shares, with when the member joined and its settled harvests.
     */
    private record MemberPlot(PlotPlacement placement, Instant memberSince, List<SettledHarvestFacts> harvests) {
    }
}
