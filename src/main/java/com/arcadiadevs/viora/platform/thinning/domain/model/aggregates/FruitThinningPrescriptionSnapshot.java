package com.arcadiadevs.viora.platform.thinning.domain.model.aggregates;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SustainableCropLoad;

import java.util.List;

/**
 * Immutable snapshot representing the complete state of a FruitThinningPrescription aggregate root.
 *
 * @param id                    aggregate root identifier
 * @param plotId                referenced plot identifier
 * @param campaignYear          monitored campaign year
 * @param observedPlotRevision  plot revision observed when aggregate was initiated
 * @param status                lifecycle status of prescription
 * @param sustainableLoad       calculated sustainable crop load parameters
 * @param samplingRounds        list of conducted sampling rounds
 * @param executionConfirmation optional execution confirmation record
 * @param revision              optimistic concurrency version counter
 */
public record FruitThinningPrescriptionSnapshot(
        PrescriptionId id,
        PlotId plotId,
        CampaignYear campaignYear,
        Long observedPlotRevision,
        PrescriptionStatus status,
        SustainableCropLoad sustainableLoad,
        List<SamplingRoundSnapshot> samplingRounds,
        ExecutionConfirmationSnapshot executionConfirmation,
        Long revision
) {
    /**
     * Compact constructor creating an immutable defensive copy of the sampling rounds list.
     */
    public FruitThinningPrescriptionSnapshot {
        samplingRounds = (samplingRounds == null) ? List.of() : List.copyOf(samplingRounds);
    }
}
