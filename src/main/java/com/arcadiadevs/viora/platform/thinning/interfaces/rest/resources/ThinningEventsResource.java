package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * REST response envelope resource representing the chronological list of thinning events for the agronomic logbook.
 *
 * @param events collection of thinning events ordered chronologically descending
 */
@Schema(name = "ThinningEventsResource", description = "Collection of fruit thinning lifecycle events")
@NullMarked
public record ThinningEventsResource(
        @ArraySchema(schema = @Schema(implementation = ThinningEventItemResource.class))
        List<ThinningEventItemResource> events
) {

    /**
     * Compact constructor creating an immutable defensive copy of the event list.
     */
    public ThinningEventsResource {
        events = events == null ? List.of() : List.copyOf(events);
    }
}
