package com.arcadiadevs.viora.platform.orchard.application.commandservices;

import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/**
 * Application service interface for mutating commands on orchard plots.
 */
public interface PlotCommandService {

    /**
     * Handles plot cadastral delimitation and registration.
     *
     * @param command command containing initial plot delimitation data
     * @return created plot identifier or an application error
     * @see DelimitPlotCommand
     */
    Result<String, ApplicationError> handle(DelimitPlotCommand command);
}
