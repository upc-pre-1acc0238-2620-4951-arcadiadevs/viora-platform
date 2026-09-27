package com.arcadiadevs.viora.platform.phenology.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RecordHarvestYieldCommand;

/**
 * Application service port orchestrating harvest record mutations in phenology.
 */
public interface HarvestRecordCommandService {

    /**
     * Handles the recording of an annual campaign harvest yield.
     *
     * @param command the command specifying plot, campaign, and yield details
     * @return {@link Result} containing the created harvest record ID on success, or an {@link ApplicationError}
     */
    Result<String, ApplicationError> handle(RecordHarvestYieldCommand command);
}
