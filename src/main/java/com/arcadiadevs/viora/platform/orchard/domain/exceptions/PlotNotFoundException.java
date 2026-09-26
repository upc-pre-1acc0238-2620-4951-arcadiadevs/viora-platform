package com.arcadiadevs.viora.platform.orchard.domain.exceptions;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceNotFoundException;

/**
 * Domain exception thrown when an orchard plot aggregate cannot be found or is inactive.
 */
public class PlotNotFoundException extends ResourceNotFoundException {

    /**
     * Constructs the exception using a typed {@link PlotId}.
     *
     * @param plotId the plot identifier that was not found
     */
    public PlotNotFoundException(PlotId plotId) {
        super("Plot", plotId != null ? plotId.plotId() : "null");
    }

    /**
     * Constructs the exception with an explicit i18n message key.
     *
     * @param message the exception message or i18n key
     */
    public PlotNotFoundException(String message) {
        super(message);
    }
}
