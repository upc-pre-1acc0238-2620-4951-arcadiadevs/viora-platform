package com.arcadiadevs.viora.platform.territory.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignment;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.AssignPlotSectorCommand;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.ResolvePlotSectorCommand;

/**
 * Command service port of the sectors members give their plots and managers review.
 */
public interface PlotSectorAssignmentCommandService {

    /**
     * Gives a member's plot a sector of the list, or leaves it «Por confirmar» with the name the member wrote.
     *
     * @param command the member, the plot and the zone or the written name
     * @return the plot's sector, or why it was not given
     */
    Result<PlotSectorAssignment, ApplicationError> handle(AssignPlotSectorCommand command);

    /**
     * Reviews a plot «Por confirmar»: adds the written name as a sector, or points it to an existing one. Every other
     * plot of the license with the same written name is resolved the same way.
     *
     * @param command the manager, the plot and the optional existing zone
     * @return the plot's sector, or why it was not reviewed
     */
    Result<PlotSectorAssignment, ApplicationError> handle(ResolvePlotSectorCommand command);
}
