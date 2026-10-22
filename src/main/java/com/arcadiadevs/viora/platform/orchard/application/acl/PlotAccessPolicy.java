package com.arcadiadevs.viora.platform.orchard.application.acl;

import com.arcadiadevs.viora.platform.orchard.interfaces.acl.OrchardContextFacade;
import com.arcadiadevs.viora.platform.shared.application.security.CurrentUserProvider;
import org.springframework.stereotype.Component;

/**
 * Who may reach the data hanging from a plot (sensors, readings, forecast, harvests, prescriptions):
 * only the producer who owns it. Meant for method security on routes under {@code /plots/{plotId}},
 * as in {@code @PreAuthorize("@plotAccess.isAccessible(#plotId)")}; a denial answers 403.
 */
@Component("plotAccess")
public class PlotAccessPolicy {

    private final OrchardContextFacade orchardContextFacade;
    private final CurrentUserProvider currentUserProvider;

    /**
     * Creates the policy.
     *
     * @param orchardContextFacade resolves the owner of a plot
     * @param currentUserProvider  resolves the user behind the request
     * @throws IllegalArgumentException if any dependency is null
     */
    public PlotAccessPolicy(OrchardContextFacade orchardContextFacade, CurrentUserProvider currentUserProvider) {
        if (orchardContextFacade == null) {
            throw new IllegalArgumentException("plot_access.orchard_facade.null");
        }
        if (currentUserProvider == null) {
            throw new IllegalArgumentException("plot_access.current_user_provider.null");
        }
        this.orchardContextFacade = orchardContextFacade;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * Whether the current user may reach the plot.
     *
     * @param plotId the plot identifier from the path
     * @return {@code false} only for a plot owned by another producer; an unknown plot passes, so the
     *         endpoint keeps answering its own 404 or 400
     */
    public boolean isAccessible(String plotId) {
        return orchardContextFacade.findPlotOwnerId(plotId)
                .map(this::isCurrentUser)
                .orElse(true);
    }

    private boolean isCurrentUser(String ownerId) {
        return ownerId.trim().equalsIgnoreCase(currentUserProvider.requireCurrentUserId());
    }
}
