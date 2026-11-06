package com.arcadiadevs.viora.platform.territory.application.internal.queryservices;

import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.territory.application.internal.security.LicenseScope;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignment;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.repositories.PlotSectorAssignmentRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The plot sectors a scope may see: those of members the license sponsors now, on plots that are still active and
 * still theirs. A member who left the license or an archived plot keeps its row but drops out of every list and count.
 */
@Component
public class VisiblePlotSectors {

    private final PlotSectorAssignmentRepository plotSectorAssignmentRepository;
    private final ExternalSubscriptionService externalSubscriptionService;
    private final ExternalOrchardService externalOrchardService;

    /**
     * Creates the component.
     *
     * @param plotSectorAssignmentRepository the sectors given to plots
     * @param externalSubscriptionService    the sponsored members
     * @param externalOrchardService         the plots
     * @throws IllegalArgumentException if any dependency is null
     */
    public VisiblePlotSectors(PlotSectorAssignmentRepository plotSectorAssignmentRepository,
                              ExternalSubscriptionService externalSubscriptionService,
                              ExternalOrchardService externalOrchardService) {
        if (plotSectorAssignmentRepository == null || externalSubscriptionService == null
                || externalOrchardService == null) {
            throw new IllegalArgumentException("territory.visible_plot_sectors.dependency.null");
        }
        this.plotSectorAssignmentRepository = plotSectorAssignmentRepository;
        this.externalSubscriptionService = externalSubscriptionService;
        this.externalOrchardService = externalOrchardService;
    }

    /**
     * Lists the plot sectors a scope sees.
     *
     * @param scope the license and who reads it
     * @return every visible member plot for the manager, the member's own visible plots for a producer
     */
    public List<PlotSectorAssignment> of(LicenseScope scope) {
        if (scope.isManager()) {
            return ofLicense(scope.licenseId());
        }
        return plotSectorAssignmentRepository.findAllByLicenseIdAndProducerId(scope.licenseId(), scope.producerId())
                .stream()
                .filter(this::isOnAnActivePlotOfItsOwner)
                .toList();
    }

    /**
     * Lists every visible member plot of a license, whoever reads it.
     *
     * @param licenseId the license
     * @return the plot sectors of the members it sponsors now, on their active plots
     */
    public List<PlotSectorAssignment> ofLicense(LicenseId licenseId) {
        var members = externalSubscriptionService.findSponsoredProducers(licenseId);
        return plotSectorAssignmentRepository.findAllByLicenseId(licenseId).stream()
                .filter(assignment -> members.contains(assignment.snapshot().producerId()))
                .filter(this::isOnAnActivePlotOfItsOwner)
                .toList();
    }

    private boolean isOnAnActivePlotOfItsOwner(PlotSectorAssignment assignment) {
        var snapshot = assignment.snapshot();
        return externalOrchardService.findActivePlotOwner(snapshot.plotId())
                .filter(owner -> owner.equals(snapshot.producerId()))
                .isPresent();
    }
}
