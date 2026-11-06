package com.arcadiadevs.viora.platform.territory.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.SponsoredMember;
import com.arcadiadevs.viora.platform.territory.application.internal.security.LicenseAccessPolicy;
import com.arcadiadevs.viora.platform.territory.application.queryservices.MemberPlotDetailView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.MemberPlotQueryService;
import com.arcadiadevs.viora.platform.territory.application.queryservices.MemberPlotView;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetMemberPlotByIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetMemberPlotsByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.DataSharingState;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reads the plots of the members who share with a manager's license, in the order the manager should visit them.
 * Members who never shared or stopped sharing contribute no plot.
 */
@Service
@Transactional(readOnly = true)
public class MemberPlotQueryServiceImpl implements MemberPlotQueryService {

    private final LicenseAccessPolicy licenseAccessPolicy;
    private final ExternalSubscriptionService externalSubscriptionService;
    private final ExternalOrchardService externalOrchardService;
    private final MemberPlotReader memberPlotReader;

    /**
     * Creates the query service.
     *
     * @param licenseAccessPolicy         the license each manager is in charge of
     * @param externalSubscriptionService the members and whether they share
     * @param externalOrchardService      the owners of plots
     * @param memberPlotReader            the reader of member plots
     * @throws IllegalArgumentException if any dependency is null
     */
    public MemberPlotQueryServiceImpl(LicenseAccessPolicy licenseAccessPolicy,
                                      ExternalSubscriptionService externalSubscriptionService,
                                      ExternalOrchardService externalOrchardService,
                                      MemberPlotReader memberPlotReader) {
        if (licenseAccessPolicy == null || externalSubscriptionService == null || externalOrchardService == null
                || memberPlotReader == null) {
            throw new IllegalArgumentException("territory.member_plot_queries.dependency.null");
        }
        this.licenseAccessPolicy = licenseAccessPolicy;
        this.externalSubscriptionService = externalSubscriptionService;
        this.externalOrchardService = externalOrchardService;
        this.memberPlotReader = memberPlotReader;
    }

    @Override
    public Result<List<MemberPlotView>, ApplicationError> handle(GetMemberPlotsByManagerIdQuery query) {
        var scope = licenseAccessPolicy.resolveManager(query.managerId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var licenseId = scope.success().orElseThrow().licenseId();
        var read = memberPlotReader.startRead(licenseId);
        return Result.success(externalSubscriptionService.findMembers(licenseId).stream()
                .filter(member -> member.sharing() == DataSharingState.SHARING)
                .flatMap(member -> memberPlotReader.plotsOf(read, member.producerId(), query.origin()).stream())
                .sorted(MemberPlotReader.VISIT_ORDER)
                .toList());
    }

    @Override
    public Result<MemberPlotDetailView, ApplicationError> handle(GetMemberPlotByIdAndManagerIdQuery query) {
        var scope = licenseAccessPolicy.resolveManager(query.managerId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var licenseId = scope.success().orElseThrow().licenseId();
        var owner = externalOrchardService.findActivePlotOwner(query.plotId());
        var sharingOwner = owner.flatMap(producerId -> externalSubscriptionService.findMembers(licenseId).stream()
                .filter(member -> member.producerId().equals(producerId))
                .filter(member -> member.sharing() == DataSharingState.SHARING)
                .map(SponsoredMember::producerId)
                .findFirst());
        // A plot of someone who is not a member, or who does not share, answers the same as one that does not exist.
        return sharingOwner
                .flatMap(producerId -> memberPlotReader.detailOf(memberPlotReader.startRead(licenseId), producerId,
                        query.plotId()))
                .<Result<MemberPlotDetailView, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(ApplicationError.notFound("member-plot", query.plotId().plotId())));
    }
}
