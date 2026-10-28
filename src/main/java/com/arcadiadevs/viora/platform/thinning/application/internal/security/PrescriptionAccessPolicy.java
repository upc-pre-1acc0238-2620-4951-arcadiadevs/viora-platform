package com.arcadiadevs.viora.platform.thinning.application.internal.security;

import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionId;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who may read or act on a thinning prescription reached by its own id: only the producer who owns its plot. Meant
 * for method security, as in {@code @PreAuthorize("@prescriptionAccess.isAccessible(#id)")}; a denial answers 403.
 */
@Component("prescriptionAccess")
@Transactional(readOnly = true)
public class PrescriptionAccessPolicy {

    private final FruitThinningPrescriptionRepository prescriptionRepository;
    private final ExternalOrchardService externalOrchardService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the policy.
     *
     * @param prescriptionRepository finds the plot of a prescription
     * @param externalOrchardService resolves the owner of that plot
     * @param currentUserProvider    resolves the user behind the request
     * @throws IllegalArgumentException if any dependency is null
     */
    public PrescriptionAccessPolicy(
            FruitThinningPrescriptionRepository prescriptionRepository,
            @Qualifier("thinningExternalOrchardService") ExternalOrchardService externalOrchardService,
            CurrentUserProvider currentUserProvider
    ) {
        if (prescriptionRepository == null) {
            throw new IllegalArgumentException("prescription_access.repository.null");
        }
        if (externalOrchardService == null) {
            throw new IllegalArgumentException("prescription_access.external_orchard_service.null");
        }
        if (currentUserProvider == null) {
            throw new IllegalArgumentException("prescription_access.current_user_provider.null");
        }
        this.prescriptionRepository = prescriptionRepository;
        this.externalOrchardService = externalOrchardService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Whether the current user may reach the prescription.
     *
     * @param prescriptionId the prescription identifier from the path
     * @return {@code false} for a prescription on a plot owned by another producer; an unknown or malformed
     *         prescription passes, so the endpoint keeps answering its own 404 or 400 without revealing anything
     */
    public boolean isAccessible(String prescriptionId) {
        final PrescriptionId id;
        try {
            id = new PrescriptionId(prescriptionId);
        } catch (IllegalArgumentException exception) {
            return true;
        }
        return prescriptionRepository.findById(id)
                .flatMap(prescription -> externalOrchardService.findPlotOwnerId(prescription.snapshot().plotId()))
                .map(ownerId -> ownerId.trim().equalsIgnoreCase(currentUserProvider.requireCurrentUserId()))
                .orElse(true);
    }
}
