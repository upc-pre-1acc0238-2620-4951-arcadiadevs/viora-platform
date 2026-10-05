package com.arcadiadevs.viora.platform.thinning.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.settlement.domain.model.events.CampaignHarvestSettledEvent;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberCalibrationObservation;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.ExecutionTimeliness;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.CaliberCalibrationObservationRepository;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Feeds the caliber model with real data: when a campaign is settled with its commercial caliber, the residual
 * load left by an on-time thinning of that campaign becomes a calibration observation of the plot variety.
 *
 * <p>Nothing is recorded without caliber, without thinning, for late thinning (the model is calibrated with
 * on-time executions only) or when no fruit was left. Runs inside the settlement transaction.</p>
 */
@Component
public class CampaignHarvestSettledEventHandler {

    private final FruitThinningPrescriptionRepository prescriptionRepository;
    private final CaliberCalibrationObservationRepository observationRepository;
    private final ExternalOrchardService externalOrchardService;

    public CampaignHarvestSettledEventHandler(FruitThinningPrescriptionRepository prescriptionRepository,
            CaliberCalibrationObservationRepository observationRepository,
            ExternalOrchardService externalOrchardService) {
        this.prescriptionRepository = prescriptionRepository;
        this.observationRepository = observationRepository;
        this.externalOrchardService = externalOrchardService;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(CampaignHarvestSettledEvent event) {
        if (event.commercialFruitsPerKg() == null) {
            return;
        }
        var plotId = new PlotId(event.plotId());
        var campaignYear = new CampaignYear(event.campaignYear());
        var confirmation = prescriptionRepository.findByPlotIdAndCampaignYear(plotId, campaignYear)
                .map(prescription -> prescription.snapshot().executionConfirmation())
                .filter(evidence -> evidence.timeliness() == ExecutionTimeliness.OPTIMAL)
                .filter(evidence -> evidence.loadBalance() != null
                        && evidence.loadBalance().residualFruitsPerShoot() > 0.0);
        if (confirmation.isEmpty()) {
            return;
        }
        externalOrchardService.findPlotVariety(plotId).ifPresent(variety ->
                observationRepository.save(new CaliberCalibrationObservation(plotId, campaignYear, variety,
                        confirmation.get().loadBalance().residualFruitsPerShoot(), event.commercialFruitsPerKg())));
    }
}
