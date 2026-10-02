package com.arcadiadevs.viora.platform.settlement.domain.exceptions;

/**
 * Raised when the dossier cannot be rendered into a valid PDF. It is a technical failure, not a business
 * conflict: nothing is certified when it occurs.
 */
public class DossierRenderingException extends RuntimeException {

    /**
     * @param message i18n key or description of the failure
     * @param cause   underlying rendering failure
     */
    public DossierRenderingException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message i18n key or description of the failure
     */
    public DossierRenderingException(String message) {
        super(message);
    }
}
