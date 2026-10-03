package com.arcadiadevs.viora.platform.settlement.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceConflictException;

/**
 * Raised when a campaign already has its dossier certified: a certification is immutable and can never be
 * overwritten. It is a state conflict, never a server fault.
 */
public class DossierAlreadyCertifiedException extends ResourceConflictException {

    /**
     * @param message i18n key describing the conflict
     */
    public DossierAlreadyCertifiedException(String message) {
        super(message);
    }
}
