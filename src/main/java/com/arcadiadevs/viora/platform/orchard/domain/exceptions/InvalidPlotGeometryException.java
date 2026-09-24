package com.arcadiadevs.viora.platform.orchard.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Domain exception thrown when cadastral polygon coordinates or topology are invalid.
 */
public class InvalidPlotGeometryException extends BusinessRuleException {

    /**
     * Constructs an InvalidPlotGeometryException with message key.
     *
     * @param messageKey the i18n message key or reason
     */
    public InvalidPlotGeometryException(String messageKey) {
        super(messageKey);
    }
}
