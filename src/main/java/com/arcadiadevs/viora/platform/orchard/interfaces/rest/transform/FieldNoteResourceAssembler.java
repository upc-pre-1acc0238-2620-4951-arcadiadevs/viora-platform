package com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.FieldNote;
import com.arcadiadevs.viora.platform.orchard.interfaces.rest.resources.FieldNoteResource;

/** Turns field notes into their REST resource. */
public final class FieldNoteResourceAssembler {

    private FieldNoteResourceAssembler() {
    }

    public static FieldNoteResource toResource(FieldNote note) {
        var s = note.snapshot();
        return new FieldNoteResource(s.id().value(), s.plotId().plotId(), s.type().name(), s.text().value(),
                s.observedOn(), s.recordedAt(), s.hasPhoto());
    }
}
