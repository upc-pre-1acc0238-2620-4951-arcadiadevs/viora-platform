package com.arcadiadevs.viora.platform.thinning.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.*;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.ConfirmThinningExecutionCommand;

/** Records field execution of an existing prescription. */
public interface ConfirmThinningExecutionCommandService {
    Result<FruitThinningPrescription, ApplicationError> handle(ConfirmThinningExecutionCommand command);
}
