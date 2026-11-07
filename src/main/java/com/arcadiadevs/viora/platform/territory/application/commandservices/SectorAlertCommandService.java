package com.arcadiadevs.viora.platform.territory.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.EvaluateSectorAlertsCommand;

/**
 * Command service port of the sector alerts.
 */
public interface SectorAlertCommandService {

    /**
     * Opens, updates and normalizes the alerts of a license's sectors from the colors of its member plots now.
     *
     * @param command the license
     * @return how many alerts opened, changed or normalized, or why it could not run
     */
    Result<Integer, ApplicationError> handle(EvaluateSectorAlertsCommand command);
}
