package com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.ThinningEvent;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.ThinningEventItemResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.ThinningEventsResource;

import java.util.List;

/**
 * REST assembler mapping domain {@link ThinningEvent} records into presentation {@link ThinningEventsResource}.
 */
public final class ThinningEventsResourceAssembler {

    private ThinningEventsResourceAssembler() {
    }

    /**
     * Converts a domain list of thinning events to the envelope resource.
     *
     * @param events domain events list
     * @return presentation resource envelope
     */
    public static ThinningEventsResource toResourceFromDomain(List<ThinningEvent> events) {
        if (events == null) {
            return new ThinningEventsResource(List.of());
        }
        var items = events.stream()
                .map(ThinningEventsResourceAssembler::toItemResource)
                .toList();
        return new ThinningEventsResource(items);
    }

    private static ThinningEventItemResource toItemResource(ThinningEvent event) {
        return new ThinningEventItemResource(
                event.id(),
                event.eventType(),
                event.prescriptionId(),
                event.confirmationId(),
                event.plotId().plotId(),
                event.plotName(),
                event.campaignYear().value(),
                event.occurredAt(),
                event.evaluatedTreesCount(),
                event.totalShootsCount(),
                event.totalFruitsCount(),
                event.meanFruitsPerShoot(),
                event.isRepresentative(),
                event.removalPercentage(),
                event.removedKg(),
                event.executedDate(),
                event.laborCrewSize(),
                event.timeliness()
        );
    }
}
