package com.arcadiadevs.viora.platform.phenology.domain.model.commands;

/**
 * Domain command to rectify the volume and composition of an existing olive harvest record.
 * Follows strict Model A rules: validates non-null mandatory fields with i18n keys without business logic.
 *
 * @param plotId           the unique identifier of the plot
 * @param recordId         the unique identifier of the harvest entry to rectify
 * @param totalYieldKg     the rectified total fruit volume harvested in kilograms
 * @param greenKg          the weight in kilograms of green olives (optional)
 * @param blackKg          the weight in kilograms of black olives (optional)
 * @param notes            agronomic remarks or weighbridge adjustment notes (optional)
 * @param expectedRevision the optimistic locking revision expected by the client (optional)
 */
public record RectifyHarvestYieldCommand(
        String plotId,
        String recordId,
        Double totalYieldKg,
        Double greenKg,
        Double blackKg,
        String notes,
        Long expectedRevision
) {

    /**
     * Compact constructor enforcing non-null presence of mandatory command attributes.
     *
     * @throws IllegalArgumentException if plotId, recordId, or totalYieldKg is null or blank
     */
    public RectifyHarvestYieldCommand {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        if (recordId == null || recordId.isBlank()) {
            throw new IllegalArgumentException("phenology.harvest_entry.id.null_or_empty");
        }
        if (totalYieldKg == null) {
            throw new IllegalArgumentException("phenology.harvest_yield.null");
        }
    }
}
