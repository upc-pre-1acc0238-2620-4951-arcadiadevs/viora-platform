package com.arcadiadevs.viora.platform.phenology.domain.model.commands;

/**
 * Domain command to record the volume and composition of an olive harvest for a specific campaign year.
 * Follows strict Model A rules: validates non-null mandatory fields with i18n keys without business logic.
 *
 * @param plotId       the unique identifier of the plot
 * @param campaignYear the agricultural campaign year
 * @param totalYieldKg the total fruit volume harvested in kilograms
 * @param greenKg      the weight in kilograms of green olives (optional)
 * @param blackKg      the weight in kilograms of black olives (optional)
 * @param notes        agronomic remarks or weighbridge references (optional)
 */
public record RecordHarvestYieldCommand(
        String plotId,
        Integer campaignYear,
        Double totalYieldKg,
        Double greenKg,
        Double blackKg,
        String notes
) {

    /**
     * Compact constructor enforcing non-null presence of mandatory command attributes.
     *
     * @throws IllegalArgumentException if plotId, campaignYear, or totalYieldKg is null
     */
    public RecordHarvestYieldCommand {
        if (plotId == null || plotId.isBlank()) {
            throw new IllegalArgumentException("plot.id.null_or_empty");
        }
        if (campaignYear == null) {
            throw new IllegalArgumentException("phenology.campaign_year.null");
        }
        if (totalYieldKg == null) {
            throw new IllegalArgumentException("phenology.harvest_yield.null");
        }
    }
}
