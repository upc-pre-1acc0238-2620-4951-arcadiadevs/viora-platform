package com.arcadiadevs.viora.platform.orchard.domain.exceptions;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotName;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceConflictException;

/**
 * Domain exception thrown when attempting to delimit a plot with a name that already exists for the producer.
 */
public class DuplicatePlotNameException extends ResourceConflictException {

    /**
     * Constructs a DuplicatePlotNameException with i18n message key.
     *
     * @param name       the duplicate plot name
     * @param producerId the producer identifier value object
     */
    public DuplicatePlotNameException(PlotName name, ProducerId producerId) {
        super("plot.name.duplicate");
    }
}
