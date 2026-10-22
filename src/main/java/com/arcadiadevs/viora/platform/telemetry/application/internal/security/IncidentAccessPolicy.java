package com.arcadiadevs.viora.platform.telemetry.application.internal.security;

import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentId;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.AgroclimaticIncidentRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who may read or act on an agroclimatic incident: only the producer who owns its plot. Meant for
 * method security, as in {@code @PreAuthorize("@incidentAccess.isAccessible(#incidentId)")}; a denial answers 403.
 */
@Component("incidentAccess")
@Transactional(readOnly = true)
public class IncidentAccessPolicy {

    private final AgroclimaticIncidentRepository incidentRepository;
    private final ExternalOrchardService externalOrchardService;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the policy.
     *
     * @param incidentRepository     finds the plot of an incident
     * @param externalOrchardService resolves the owner of that plot
     * @param currentUserProvider    resolves the user behind the request
     * @throws IllegalArgumentException if any dependency is null
     */
    public IncidentAccessPolicy(
            AgroclimaticIncidentRepository incidentRepository,
            @Qualifier("telemetryExternalOrchardService") ExternalOrchardService externalOrchardService,
            CurrentUserProvider currentUserProvider
    ) {
        if (incidentRepository == null) {
            throw new IllegalArgumentException("incident_access.repository.null");
        }
        if (externalOrchardService == null) {
            throw new IllegalArgumentException("incident_access.external_orchard_service.null");
        }
        if (currentUserProvider == null) {
            throw new IllegalArgumentException("incident_access.current_user_provider.null");
        }
        this.incidentRepository = incidentRepository;
        this.externalOrchardService = externalOrchardService;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Whether the current user may reach the incident.
     *
     * @param incidentId the incident identifier from the path
     * @return {@code false} only for an incident on a plot owned by another producer; an unknown or
     *         malformed incident passes, so the endpoint keeps answering its own 404 or 400
     */
    public boolean isAccessible(String incidentId) {
        final IncidentId id;
        try {
            id = new IncidentId(incidentId);
        } catch (IllegalArgumentException exception) {
            return true;
        }
        return incidentRepository.findById(id)
                .flatMap(incident -> externalOrchardService.findPlotOwnerId(incident.snapshot().plotId()))
                .map(ownerId -> ownerId.trim().equalsIgnoreCase(currentUserProvider.requireCurrentUserId()))
                .orElse(true);
    }
}
