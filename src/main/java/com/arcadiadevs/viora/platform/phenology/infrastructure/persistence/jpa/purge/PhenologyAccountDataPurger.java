package com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.purge;

import com.arcadiadevs.viora.platform.orchard.domain.model.events.ProducerPlotsPurgedEvent;
import com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.repositories.ChillAccumulationTrackerPersistenceRepository;
import com.arcadiadevs.viora.platform.phenology.infrastructure.persistence.jpa.repositories.HistoricalHarvestEntryPersistenceRepository;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Removes the harvest history and the winter chill trackers of the plots of a deleted account.
 */
@Component
public class PhenologyAccountDataPurger {

    private final HistoricalHarvestEntryPersistenceRepository harvests;
    private final ChillAccumulationTrackerPersistenceRepository trackers;

    /**
     * Creates the purger.
     *
     * @param harvests the harvest records table
     * @param trackers the chill trackers table
     */
    public PhenologyAccountDataPurger(HistoricalHarvestEntryPersistenceRepository harvests,
                                      ChillAccumulationTrackerPersistenceRepository trackers) {
        this.harvests = harvests;
        this.trackers = trackers;
    }

    /**
     * Removes the phenology data of the plots.
     *
     * @param event the plots of the deleted account
     */
    @EventListener
    public void on(ProducerPlotsPurgedEvent event) {
        if (event.plotIds().isEmpty()) {
            return;
        }
        var plotIds = event.plotIds().stream().map(UUID::fromString).toList();
        harvests.deleteAll(harvests.findAllByPlotIdIn(plotIds));
        trackers.deleteAll(trackers.findAllByPlotIdIn(plotIds));
    }
}
