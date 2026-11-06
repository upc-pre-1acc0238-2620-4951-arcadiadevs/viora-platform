package com.arcadiadevs.viora.platform.territory.domain.exceptions;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;

/**
 * Thrown when a license already has a sector zone with the same name, ignoring accents and case.
 */
public class SectorZoneNameTakenException extends BusinessRuleException {

    /** The i18n key of the message. */
    public static final String MESSAGE_KEY = "territory.sector_zone.name_taken";

    /**
     * Creates the exception with its message key.
     */
    public SectorZoneNameTakenException() {
        super(MESSAGE_KEY);
    }
}
