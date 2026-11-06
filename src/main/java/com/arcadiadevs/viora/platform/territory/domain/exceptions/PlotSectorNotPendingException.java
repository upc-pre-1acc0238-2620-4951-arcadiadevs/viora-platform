package com.arcadiadevs.viora.platform.territory.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Thrown when a manager reviews the sector of a plot that is no longer «Por confirmar».
 */
public class PlotSectorNotPendingException extends BusinessRuleException {

    /** The i18n key of the message. */
    public static final String MESSAGE_KEY = "territory.plot_sector.not_pending";

    /**
     * Creates the exception with its message key.
     */
    public PlotSectorNotPendingException() {
        super(MESSAGE_KEY);
    }
}
