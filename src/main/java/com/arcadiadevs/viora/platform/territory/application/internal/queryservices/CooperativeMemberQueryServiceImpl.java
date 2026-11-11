package com.arcadiadevs.viora.platform.territory.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalProfileService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.SponsoredMember;
import com.arcadiadevs.viora.platform.territory.application.internal.security.LicenseAccessPolicy;
import com.arcadiadevs.viora.platform.territory.application.queryservices.CooperativeMemberDetailView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.CooperativeMemberQueryService;
import com.arcadiadevs.viora.platform.territory.application.queryservices.CooperativeMemberView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.MemberPlotView;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetCooperativeMemberByIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetCooperativeMembersByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.DataSharingState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Reads the member roll of a manager's license. A member who does not share shows only their name, state and
 * plan: no contact and no plots.
 */
@Service
@Transactional(readOnly = true)
public class CooperativeMemberQueryServiceImpl implements CooperativeMemberQueryService {

    private final LicenseAccessPolicy licenseAccessPolicy;
    private final ExternalSubscriptionService externalSubscriptionService;
    private final ExternalProfileService externalProfileService;
    private final ExternalIamService externalIamService;
    private final MemberPlotReader memberPlotReader;

    /**
     * Creates the query service.
     *
     * @param licenseAccessPolicy         the license each manager is in charge of
     * @param externalSubscriptionService the members and whether they share
     * @param externalProfileService      the names and phones
     * @param externalIamService          the emails
     * @param memberPlotReader            the reader of member plots
     * @throws IllegalArgumentException if any dependency is null
     */
    public CooperativeMemberQueryServiceImpl(LicenseAccessPolicy licenseAccessPolicy,
                                             ExternalSubscriptionService externalSubscriptionService,
                                             ExternalProfileService externalProfileService,
                                             ExternalIamService externalIamService,
                                             MemberPlotReader memberPlotReader) {
        if (licenseAccessPolicy == null || externalSubscriptionService == null || externalProfileService == null
                || externalIamService == null || memberPlotReader == null) {
            throw new IllegalArgumentException("territory.member_queries.dependency.null");
        }
        this.licenseAccessPolicy = licenseAccessPolicy;
        this.externalSubscriptionService = externalSubscriptionService;
        this.externalProfileService = externalProfileService;
        this.externalIamService = externalIamService;
        this.memberPlotReader = memberPlotReader;
    }

    @Override
    public Result<List<CooperativeMemberView>, ApplicationError> handle(GetCooperativeMembersByManagerIdQuery query) {
        var scope = licenseAccessPolicy.resolveManager(query.managerId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var licenseId = scope.success().orElseThrow().licenseId();
        var read = memberPlotReader.startRead(licenseId);
        return Result.success(externalSubscriptionService.findMembers(licenseId).stream()
                .map(member -> toView(member, member.sharing() == DataSharingState.SHARING
                        ? memberPlotReader.plotsOf(read, member.producerId(), null) : List.of()))
                .sorted(Comparator.comparing(view -> view.fullName() == null ? "" : view.fullName()))
                .toList());
    }

    @Override
    public Result<CooperativeMemberDetailView, ApplicationError> handle(
            GetCooperativeMemberByIdAndManagerIdQuery query) {
        var scope = licenseAccessPolicy.resolveManager(query.managerId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var licenseId = scope.success().orElseThrow().licenseId();
        var member = externalSubscriptionService.findMembers(licenseId).stream()
                .filter(each -> each.producerId().equals(query.memberId()))
                .findFirst();
        if (member.isEmpty()) {
            return Result.failure(ApplicationError.notFound("cooperative-member", query.memberId().producerId()));
        }
        var found = member.get();
        var name = externalProfileService.findFullName(found.producerId()).orElse(null);
        if (found.sharing() != DataSharingState.SHARING) {
            return Result.success(new CooperativeMemberDetailView(found.producerId().producerId(), name,
                    found.sharing().name(), found.withdrawnAt(), found.memberSince(), found.quotaHa(),
                    found.maskedCode(), null, null, null, List.of()));
        }
        var plots = memberPlotReader.plotsOf(memberPlotReader.startRead(licenseId), found.producerId(), null);
        return Result.success(new CooperativeMemberDetailView(found.producerId().producerId(), name,
                found.sharing().name(), null, found.memberSince(), found.quotaHa(), found.maskedCode(),
                externalProfileService.findPhone(found.producerId()).orElse(null),
                externalIamService.findEmail(found.producerId()).orElse(null),
                registeredArea(plots), sortedForVisit(plots)));
    }

    private CooperativeMemberView toView(SponsoredMember member, List<MemberPlotView> plots) {
        var name = externalProfileService.findFullName(member.producerId()).orElse(null);
        if (member.sharing() != DataSharingState.SHARING) {
            return new CooperativeMemberView(member.producerId().producerId(), name, member.sharing().name(),
                    member.withdrawnAt(), null, null, null, List.of(), null, null);
        }
        var worst = sortedForVisit(plots).stream().findFirst();
        return new CooperativeMemberView(member.producerId().producerId(), name, member.sharing().name(), null,
                plots.size(), (int) plots.stream().filter(plot -> !plot.sampled()).count(), registeredArea(plots),
                plots.stream().map(MemberPlotView::sectorZoneName).filter(Objects::nonNull).distinct().sorted()
                        .toList(),
                worst.map(MemberPlotView::riskLevel).orElse(null),
                worst.flatMap(plot -> plot.reasons().stream().findFirst()).orElse(null));
    }

    private static List<MemberPlotView> sortedForVisit(List<MemberPlotView> plots) {
        return plots.stream().sorted(MemberPlotReader.VISIT_ORDER).toList();
    }

    private static BigDecimal registeredArea(List<MemberPlotView> plots) {
        return plots.stream()
                .map(MemberPlotView::areaHa)
                .filter(Objects::nonNull)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
