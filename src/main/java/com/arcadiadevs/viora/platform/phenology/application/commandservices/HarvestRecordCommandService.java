package com.arcadiadevs.viora.platform.phenology.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RecordHarvestYieldCommand;

import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RectifyHarvestYieldCommand;
import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RemoveHarvestRecordCommand;

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

    /**
     * Handles the rectification of an existing annual campaign harvest record.
     *
     * @param command the command specifying plot, entry ID, updated yields, and revision
     * @return {@link Result} containing the rectified harvest record ID on success, or an {@link ApplicationError}
     */
    Result<String, ApplicationError> handle(RectifyHarvestYieldCommand command);

    /**
     * Handles the removal of an erroneous or duplicated harvest record, recalculating the BBI over the
     * campaigns that remain valid.
     *
     * @param command the command specifying plot, entry ID and optional revision
     * @return {@link Result} containing the removed harvest record ID on success, or an {@link ApplicationError}
     */
    Result<String, ApplicationError> handle(RemoveHarvestRecordCommand command);
}
