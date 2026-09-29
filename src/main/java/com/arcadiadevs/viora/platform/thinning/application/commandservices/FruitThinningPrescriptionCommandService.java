package com.arcadiadevs.viora.platform.thinning.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.IngestFieldSamplingsBatchCommand;

/**
 * Application service interface for mutating commands on fruit thinning prescriptions and field samplings.
 */
public interface FruitThinningPrescriptionCommandService {

    /**
     * Handles ingestion and validation of in-field tree samplings for an orchard plot.
     *
     * @param command command containing sampling batch details and tree evaluations
     * @return the updated or created FruitThinningPrescription or an application error
     * @see IngestFieldSamplingsBatchCommand
     */
    Result<FruitThinningPrescription, ApplicationError> handle(IngestFieldSamplingsBatchCommand command);
}
