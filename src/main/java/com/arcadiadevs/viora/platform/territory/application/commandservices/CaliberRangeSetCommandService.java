package com.arcadiadevs.viora.platform.territory.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.CaliberRangeSet;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.ReplaceCaliberRangesCommand;

/**
 * Use cases of the caliber ranges of a license.
 */
public interface CaliberRangeSetCommandService {

    /**
     * Replaces the caliber ranges of the license the manager is in charge of.
     *
     * @param command the manager and the new ranges
     * @return the ranges stored, or why they were refused
     */
    Result<CaliberRangeSet, ApplicationError> handle(ReplaceCaliberRangesCommand command);
}
