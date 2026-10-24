package com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.purge;

import com.arcadiadevs.viora.platform.orchard.domain.model.events.ProducerPlotsPurgedEvent;
import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.repositories.CaliberCalibrationObservationPersistenceRepository;
import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.repositories.FruitThinningPrescriptionPersistenceRepository;
import com.arcadiadevs.viora.platform.thinning.infrastructure.persistence.jpa.repositories.SamplingRoundPersistenceRepository;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Removes the sampling rounds, thinning prescriptions (with their execution confirmations) and caliber observations
 * of the plots of a deleted account.
 */
@Component
public class ThinningAccountDataPurger {

    private final SamplingRoundPersistenceRepository rounds;
    private final FruitThinningPrescriptionPersistenceRepository prescriptions;
    private final CaliberCalibrationObservationPersistenceRepository observations;

    /**
     * Creates the purger.
     *
     * @param rounds        the sampling rounds table
     * @param prescriptions the prescriptions table
     * @param observations  the caliber observations table
     */
    public ThinningAccountDataPurger(SamplingRoundPersistenceRepository rounds,
                                     FruitThinningPrescriptionPersistenceRepository prescriptions,
                                     CaliberCalibrationObservationPersistenceRepository observations) {
        this.rounds = rounds;
        this.prescriptions = prescriptions;
        this.observations = observations;
    }

    /**
     * Removes the thinning data of the plots.
     *
     * @param event the plots of the deleted account
     */
    @EventListener
    public void on(ProducerPlotsPurgedEvent event) {
        if (event.plotIds().isEmpty()) {
            return;
        }
        var plotIds = event.plotIds().stream().map(UUID::fromString).toList();
        rounds.deleteAll(rounds.findAllByPlotIdIn(plotIds));
        prescriptions.deleteAll(prescriptions.findAllByPlotIdIn(plotIds));
        observations.deleteAll(observations.findAllByPlotIdIn(plotIds));
    }
}
