package com.arcadiadevs.viora.platform.territory.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalProfileService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSettlementService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ProvisionalDossierFacts;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.SponsoredMember;
import com.arcadiadevs.viora.platform.territory.application.internal.security.LicenseAccessPolicy;
import com.arcadiadevs.viora.platform.territory.application.queryservices.MemberPlotDetailView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.MemberPlotQueryService;
import com.arcadiadevs.viora.platform.territory.application.queryservices.MemberPlotView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.ProvisionalDossierFile;
import com.arcadiadevs.viora.platform.territory.application.queryservices.ProvisionalDossierView;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetMemberPlotByIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetMemberPlotsByManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetProvisionalDossierByPlotIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.queries.GetProvisionalDossierPdfByPlotIdAndManagerIdQuery;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.DataSharingState;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
    private final ExternalProfileService externalProfileService;
    private final ExternalSettlementService externalSettlementService;

    /**
     * Creates the query service.
     *
     * @param licenseAccessPolicy         the license each manager is in charge of
     * @param externalSubscriptionService the members and whether they share
     * @param externalOrchardService      the owners of plots
     * @param memberPlotReader            the reader of member plots
     * @param externalProfileService      the names of the members
     * @param externalSettlementService   the provisional dossiers
     * @throws IllegalArgumentException if any dependency is null
     */
    public MemberPlotQueryServiceImpl(LicenseAccessPolicy licenseAccessPolicy,
                                      ExternalSubscriptionService externalSubscriptionService,
                                      ExternalOrchardService externalOrchardService,
                                      MemberPlotReader memberPlotReader,
                                      ExternalProfileService externalProfileService,
                                      ExternalSettlementService externalSettlementService) {
        if (licenseAccessPolicy == null || externalSubscriptionService == null || externalOrchardService == null
                || memberPlotReader == null || externalProfileService == null || externalSettlementService == null) {
            throw new IllegalArgumentException("territory.member_plot_queries.dependency.null");
        }
        this.licenseAccessPolicy = licenseAccessPolicy;
        this.externalSubscriptionService = externalSubscriptionService;
        this.externalOrchardService = externalOrchardService;
        this.memberPlotReader = memberPlotReader;
        this.externalProfileService = externalProfileService;
        this.externalSettlementService = externalSettlementService;
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
        return sharingOwnerOf(licenseId, query.plotId())
                .flatMap(producerId -> memberPlotReader.detailOf(memberPlotReader.startRead(licenseId), producerId,
                        query.plotId()))
                .<Result<MemberPlotDetailView, ApplicationError>>map(Result::success)
                .orElseGet(() -> notFound(query.plotId()));
    }

    @Override
    public Result<ProvisionalDossierView, ApplicationError> handle(
            GetProvisionalDossierByPlotIdAndManagerIdQuery query) {
        var scope = licenseAccessPolicy.resolveManager(query.managerId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var licenseId = scope.success().orElseThrow().licenseId();
        return sharingOwnerOf(licenseId, query.plotId())
                .flatMap(producerId -> {
                    var holder = externalProfileService.findFullName(producerId).orElse(null);
                    var organization = externalSubscriptionService.findOrganizationName(licenseId).orElse(null);
                    return externalSettlementService.findProvisionalDossier(query.plotId(), holder, organization)
                            .map(facts -> toView(query.plotId(), holder, organization, facts));
                })
                .<Result<ProvisionalDossierView, ApplicationError>>map(Result::success)
                .orElseGet(() -> notFound(query.plotId()));
    }

    @Override
    public Result<ProvisionalDossierFile, ApplicationError> handle(
            GetProvisionalDossierPdfByPlotIdAndManagerIdQuery query) {
        var scope = licenseAccessPolicy.resolveManager(query.managerId());
        if (scope.isFailure()) {
            return Result.failure(scope.failure().orElseThrow());
        }
        var licenseId = scope.success().orElseThrow().licenseId();
        return sharingOwnerOf(licenseId, query.plotId())
                .flatMap(producerId -> externalSettlementService.renderProvisionalDossier(query.plotId(),
                        externalProfileService.findFullName(producerId).orElse(null),
                        externalSubscriptionService.findOrganizationName(licenseId).orElse(null)))
                .map(pdf -> new ProvisionalDossierFile(pdf.content(), fileNameOf(pdf.cutOffAt()), pdf.cutOffAt()))
                .<Result<ProvisionalDossierFile, ApplicationError>>map(Result::success)
                .orElseGet(() -> notFound(query.plotId()));
    }

    /**
     * The member who owns an active plot, only while they share with the license. A plot of someone who is not a
     * member, or who does not share, answers the same as one that does not exist.
     */
    private Optional<ProducerId> sharingOwnerOf(LicenseId licenseId, PlotId plotId) {
        return externalOrchardService.findActivePlotOwner(plotId)
                .flatMap(producerId -> externalSubscriptionService.findMembers(licenseId).stream()
                        .filter(member -> member.producerId().equals(producerId))
                        .filter(member -> member.sharing() == DataSharingState.SHARING)
                        .map(SponsoredMember::producerId)
                        .findFirst());
    }

    private static ProvisionalDossierView toView(PlotId plotId, String holder, String organization,
                                                 ProvisionalDossierFacts facts) {
        return new ProvisionalDossierView(plotId.plotId(), facts.plotName(), holder, organization, facts.variety(),
                facts.areaHectares(), facts.campaignYear(), facts.cutOffAt(), facts.preThinningFruitsPerShoot(),
                facts.targetFruitsPerShoot(), facts.overTargetPercentage(), facts.prescribedRemovalPercentage(),
                facts.windowOpensOn(), facts.windowClosesOn(), facts.sampledTrees(), facts.lastSampledOn(),
                facts.thinnedOn(), facts.linkedNodes(), facts.daysWithReadings());
    }

    private static String fileNameOf(Instant cutOffAt) {
        return "expediente-provisional-" + LocalDate.ofInstant(cutOffAt, ProducerTimeZone.ZONE) + ".pdf";
    }

    private static <T> Result<T, ApplicationError> notFound(PlotId plotId) {
        return Result.failure(ApplicationError.notFound("member-plot", plotId.plotId()));
    }
}
