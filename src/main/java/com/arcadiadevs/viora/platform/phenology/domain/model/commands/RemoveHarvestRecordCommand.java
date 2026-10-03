package com.arcadiadevs.viora.platform.phenology.domain.model.commands;

/**
 * Command to remove an erroneous or duplicated historical harvest record of a plot (US21, scenario 2).
 *
 * @param plotId           the unique identifier of the plot the record belongs to
 * @param recordId         the unique identifier of the harvest record to remove
 * @param expectedRevision optional optimistic locking revision of the plot's phenology tracker
 */
public record RemoveHarvestRecordCommand(
        String plotId,
        String recordId,
        Long expectedRevision
) {

    /**
     * Canonical constructor validating the structural invariants of the command.
     */
    public RemoveHarvestRecordCommand {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        if (recordId == null || recordId.isBlank()) {
            throw new IllegalArgumentException("phenology.harvest_entry.id.null_or_empty");
        }
    }
}
