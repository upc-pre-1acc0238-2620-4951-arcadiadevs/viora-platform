package com.arcadiadevs.viora.platform.thinning.application.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.EvaluateThinningPrescriptionCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.IngestFieldSamplingsBatchCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.RecordFullBloomCommand;

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

    /**
     * Records the full bloom date of a plot and campaign and issues the prescription if that was the
     * last missing input. Recording it again corrects it, and recalculates an issued prescription.
     *
     * @param command plot, campaign and observed full bloom date
     * @return the updated or created prescription or an application error
     */
    Result<FruitThinningPrescription, ApplicationError> handle(RecordFullBloomCommand command);

    /**
     * Issues the prescription of a plot and campaign if everything it needs is known by now.
     *
     * @param command plot and campaign to evaluate
     * @return the prescription (issued or not) or an application error when there is none
     */
    Result<FruitThinningPrescription, ApplicationError> handle(EvaluateThinningPrescriptionCommand command);
}
