package com.arcadiadevs.viora.platform.phenology.domain.exceptions;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestEntryId;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.ResourceNotFoundException;

/**
 * Domain exception thrown when a historical harvest entry is not found within a tracker.
 */
public class HarvestRecordNotFoundException extends ResourceNotFoundException {

    private final HarvestEntryId entryId;

    /**
     * Constructs a HarvestRecordNotFoundException with the harvest entry identifier.
     *
     * @param entryId the harvest entry identifier
     */
    public HarvestRecordNotFoundException(HarvestEntryId entryId) {
        super("phenology.harvest_entry.not_found");
        this.entryId = entryId;
    }

    public HarvestEntryId getEntryId() {
        return entryId;
    }
}
