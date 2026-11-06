package com.arcadiadevs.viora.platform.territory.infrastructure.persistence.jpa.purge;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.repositories.PlotSectorAssignmentRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Removes the sectors a deleted account gave its plots. The sector zones stay: they belong to the license.
 */
@Component
public class TerritoryAccountDataPurger {

    private final PlotSectorAssignmentRepository plotSectorAssignmentRepository;

    /**
     * Creates the purger.
     *
     * @param plotSectorAssignmentRepository the sectors given to plots
     */
    public TerritoryAccountDataPurger(PlotSectorAssignmentRepository plotSectorAssignmentRepository) {
        this.plotSectorAssignmentRepository = plotSectorAssignmentRepository;
    }

    /**
     * Removes the sectors of the account's plots.
     *
     * @param event the deleted account
     */
    @EventListener
    @Transactional
    public void on(UserAccountDeletedEvent event) {
        plotSectorAssignmentRepository.deleteAllByProducerId(new ProducerId(event.userId()));
    }
}
