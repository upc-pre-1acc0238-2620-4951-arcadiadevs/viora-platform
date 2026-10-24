package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.purge;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.orchard.domain.model.events.ProducerPlotsPurgedEvent;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.entities.FieldNotePersistenceEntity;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.repositories.FieldNotePersistenceRepository;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.repositories.FieldNotePhotoPersistenceRepository;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.repositories.PlotPersistenceRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Removes the plots of a deleted account, archived ones included, with their logbook notes and photos.
 *
 * <p>Before removing them it publishes {@link ProducerPlotsPurgedEvent}, so the contexts that keep data per plot
 * remove it in the same transaction.</p>
 */
@Component
public class OrchardAccountDataPurger {

    private final PlotPersistenceRepository plots;
    private final FieldNotePersistenceRepository notes;
    private final FieldNotePhotoPersistenceRepository photos;
    private final ApplicationEventPublisher events;

    /**
     * Creates the purger.
     *
     * @param plots  the plots table
     * @param notes  the logbook notes table
     * @param photos the note photos table
     * @param events publisher of {@link ProducerPlotsPurgedEvent}
     */
    public OrchardAccountDataPurger(PlotPersistenceRepository plots, FieldNotePersistenceRepository notes,
                                    FieldNotePhotoPersistenceRepository photos, ApplicationEventPublisher events) {
        this.plots = plots;
        this.notes = notes;
        this.photos = photos;
        this.events = events;
    }

    /**
     * Removes the orchard data of the account.
     *
     * @param event the deleted account
     */
    @EventListener
    public void on(UserAccountDeletedEvent event) {
        var owned = plots.findAllByProducerId(new ProducerId(event.userId()));
        var plotIds = owned.stream().map(plot -> plot.getId().toString()).toList();
        events.publishEvent(new ProducerPlotsPurgedEvent(event.userId(), plotIds, event.occurredOn()));

        var ownedNotes = notes.findAllByProducerId(event.userId());
        photos.deleteAllById(ownedNotes.stream().map(FieldNotePersistenceEntity::getId).toList());
        notes.deleteAll(ownedNotes);
        plots.deleteAll(owned);
    }
}
