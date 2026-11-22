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
import com.arcadiadevs.viora.platform.territory.application.queryservices.CaliberIntakeView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.IntakeForecastQueryService;
import com.arcadiadevs.viora.platform.territory.application.queryservices.IntakeForecastView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.SectorIntakeView;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetIntakeForecastByCampaignYearAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetIntakeForecastsByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberRange;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.CaliberSplit;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ConfidenceThresholds;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.DataSharingState;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.IntakeConfidence;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.IntakeProjection;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSettlement;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotYield;
import com.arcadiadevs.viora.platform.territory.domain.repositories.CaliberRangeSetRepository;
import com.arcadiadevs.viora.platform.territory.domain.services.YieldAggregationDomainService;
import org.springframework.beans.factory.annotation.Value;
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
    private final CaliberRangeSetRepository caliberRangeSetRepository;
    private final ConfidenceThresholds confidenceThresholds;
    private final Clock clock;

    /**
     * Creates the query service.
     *
     * @param licenseAccessPolicy         the license each manager is in charge of
     * @param externalSubscriptionService the members and whether they share
     * @param externalThinningService     the yield estimates
     * @param externalSettlementService   the settled harvests
     * @param memberPlotReader            the member plots and their sectors
     * @param caliberRangeSetRepository   the caliber ranges of each license
     * @param mediumConfidencePercent     the coverage from which a projection has medium confidence
     * @param highConfidencePercent       the coverage from which a projection has high confidence
     * @param clock                       the application clock
     * @throws IllegalArgumentException if any dependency is null or the thresholds are inconsistent
     */
    public IntakeForecastQueryServiceImpl(LicenseAccessPolicy licenseAccessPolicy,
                                          ExternalSubscriptionService externalSubscriptionService,
                                          ExternalThinningService externalThinningService,
                                          ExternalSettlementService externalSettlementService,
                                          MemberPlotReader memberPlotReader,
                                          CaliberRangeSetRepository caliberRangeSetRepository,
                                          @Value("${viora.intake.confidence.medium-percent:50}")
                                          double mediumConfidencePercent,
                                          @Value("${viora.intake.confidence.high-percent:80}")
                                          double highConfidencePercent,
                                          Clock clock) {
        if (licenseAccessPolicy == null || externalSubscriptionService == null || externalThinningService == null
                || externalSettlementService == null || memberPlotReader == null
                || caliberRangeSetRepository == null || clock == null) {
            throw new IllegalArgumentException("territory.intake_queries.dependency.null");
        }
        this.caliberRangeSetRepository = caliberRangeSetRepository;
        this.confidenceThresholds = new ConfidenceThresholds(mediumConfidencePercent, highConfidencePercent);
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
        var licenseId = scope.success().orElseThrow().licenseId();
        var plots = memberPlots(licenseId);
        var ranges = rangesOf(licenseId);
        var current = currentCampaign();
        var years = new TreeSet<Integer>(Comparator.reverseOrder());
        years.add(current);
        plots.forEach(plot -> plot.harvests().stream()
                .filter(harvest -> harvest.campaignYear() < current)
                .forEach(harvest -> years.add(harvest.campaignYear())));
        return Result.success(years.stream()
                .map(year -> intakeOf(year, current, plots, ranges, false, Map.of()))
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
        return Result.success(intakeOf(query.campaignYear(), current, plots, rangesOf(licenseId), true,
                zoneNames(licenseId)));
    }

    /**
     * The intake of a campaign: in progress, projected with its confidence and caliber split; past, settled.
     */
    private IntakeForecastView intakeOf(int year, int current, List<MemberPlot> plots, List<CaliberRange> ranges,
                                        boolean withSectors, Map<String, String> zoneNames) {
        if (year == current) {
            var yields = plots.stream()
                    .map(plot -> {
                        var outlook = externalThinningService.findYieldOutlook(plot.placement().plotId(), year);
                        var kgPerHa = outlook.map(YieldOutlookFacts::expectedKgPerHa).orElse(null);
                        var area = plot.placement().areaHa();
                        return new PlotYield(plot.placement().plotId(), plot.placement().placement(),
                                outlook.map(YieldOutlookFacts::sampled).orElse(false),
                                kgPerHa == null || area == null ? null : kgPerHa * area, greenShare(plot.harvests()),
                                area, outlook.map(YieldOutlookFacts::fruitsPerKg).orElse(null));
                    })
                    .toList();
            return toView(year, YieldAggregationDomainService.project(yields), withSectors, zoneNames,
                    YieldAggregationDomainService.confidence(yields, confidenceThresholds),
                    YieldAggregationDomainService.splitByCaliber(yields, ranges));
        }
        return toView(year, settledOf(year, plots), withSectors, zoneNames, null, CaliberSplit.NONE);
    }

    private IntakeProjection settledOf(int year, List<MemberPlot> plots) {
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

    private List<CaliberRange> rangesOf(LicenseId licenseId) {
        return caliberRangeSetRepository.findByLicenseId(licenseId)
                .map(set -> set.snapshot().ranges())
                .orElse(List.of());
    }

    private Map<String, String> zoneNames(LicenseId licenseId) {
        return memberPlotReader.startRead(licenseId).zones().entrySet().stream()
                .collect(Collectors.toMap(entry -> entry.getKey().sectorZoneId(), Map.Entry::getValue));
    }

    private int currentCampaign() {
        return LocalDate.ofInstant(clock.instant(), ProducerTimeZone.ZONE).getYear();
    }

    private IntakeForecastView toView(int year, IntakeProjection intake, boolean withSectors,
                                      Map<String, String> zoneNames, IntakeConfidence confidence,
                                      CaliberSplit caliber) {
        Function<Double, Double> tons = kilograms -> kilograms == null ? null
                : Math.round(kilograms / 100.0) / 10.0;
        var sectors = !withSectors ? List.<SectorIntakeView>of() : intake.sectors().stream()
                .map(sector -> {
                    var zoneId = sector.sector().sectorZoneId() == null ? null
                            : sector.sector().sectorZoneId().sectorZoneId();
                    return new SectorIntakeView(zoneId, zoneId == null ? null : zoneNames.get(zoneId),
                            sector.sector().status(), sector.plots(), sector.sampledPlots(),
                            tons.apply(sector.kilograms()), tons.apply(sector.greenKg()),
                            tons.apply(sector.blackKg()), tons.apply(sector.unclassifiedKg()));
                })
                .sorted(Comparator.comparing((SectorIntakeView view) -> view.sectorZoneId() == null)
                        .thenComparing(view -> view.tons() == null ? 0.0 : -view.tons()))
                .toList();
        var currentKind = year == currentCampaign() ? PROJECTION : SETTLED;
        return new IntakeForecastView(year, currentKind, intake.status().name(), tons.apply(intake.totalKg()),
                tons.apply(intake.greenKg()), tons.apply(intake.blackKg()), tons.apply(intake.unclassifiedKg()),
                intake.plots(), intake.sampledPlots(), intake.estimablePlots(), intake.coveragePercent(),
                YieldAggregationDomainService.PRELIMINARY_THRESHOLD_PERCENT, sectors,
                confidence == null ? null : confidence.level().name(),
                confidence == null ? null : confidence.pendingPlots(),
                confidence == null ? null : confidence.pendingHectares(),
                caliber.ranges().stream()
                        .map(range -> new CaliberIntakeView(range.range().minFruitsPerKg(),
                                range.range().maxFruitsPerKg(), tons.apply(range.kilograms()),
                                tons.apply(range.greenKg()), tons.apply(range.blackKg()),
                                tons.apply(range.unclassifiedKg())))
                        .toList(),
                tons.apply(caliber.unsizedKg()), tons.apply(caliber.outOfRangeKg()), clock.instant());
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
