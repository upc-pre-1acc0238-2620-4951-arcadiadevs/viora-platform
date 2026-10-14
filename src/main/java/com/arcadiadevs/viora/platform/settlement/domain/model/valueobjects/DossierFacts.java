package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * What the sealed dossier of a campaign states, frozen when it is sealed: the same values the PDF prints. A field is
 * null when the platform never had it (for example, no sampling or no thinning recorded).
 *
 * @param plotName                     plot name
 * @param variety                      olive variety
 * @param areaHectares                 plot area
 * @param preThinningFruitsPerShoot    mean load of the representative sampling, before thinning
 * @param targetFruitsPerShoot         sustainable target of the prescription
 * @param prescribedRemovalPercentage  percentage the prescription asked to remove
 * @param windowOpensOn                first recommended thinning date
 * @param windowClosesOn               latest recommended thinning date
 * @param actualRemovalPercentage      percentage actually removed
 * @param thinnedOn                    date of the thinning labor
 * @param thinningCompliance           {@code EXECUTED_ON_TIME}, {@code EXECUTED_LATE} or {@code NOT_RECORDED}
 * @param totalKg                      settled harvest
 * @param greenKg                      settled green olives
 * @param blackKg                      settled black olives
 * @param receiptNumber                settlement receipt number
 * @param weighedOn                    weighing date at the mill
 * @param linkedNodes                  climate nodes linked to the plot
 * @param daysWithReadings             days of the campaign with climate readings, up to the settlement
 * @param historyFromYear              first harvested campaign in the history, or null without history
 * @param historyToYear                last harvested campaign in the history, or null without history
 * @param harvestCampaigns             harvested campaigns in the history, this one included
 */
public record DossierFacts(
        String plotName,
        String variety,
        Double areaHectares,
        Double preThinningFruitsPerShoot,
        Double targetFruitsPerShoot,
        Double prescribedRemovalPercentage,
        LocalDate windowOpensOn,
        LocalDate windowClosesOn,
        Double actualRemovalPercentage,
        LocalDate thinnedOn,
        String thinningCompliance,
        double totalKg,
        double greenKg,
        double blackKg,
        String receiptNumber,
        LocalDate weighedOn,
        int linkedNodes,
        int daysWithReadings,
        Integer historyFromYear,
        Integer historyToYear,
        int harvestCampaigns
) {

    public DossierFacts {
        if (thinningCompliance == null || thinningCompliance.isBlank() || linkedNodes < 0 || daysWithReadings < 0
                || harvestCampaigns < 0 || totalKg < 0 || greenKg < 0 || blackKg < 0) {
            throw new IllegalArgumentException("settlement.dossier.content.invalid");
        }
    }

    /**
     * Percentage of fruit above the sustainable target before thinning.
     *
     * @return {@code (load / target - 1) x 100}, or null when either is unknown
     */
    public Double overTargetPercentage() {
        if (preThinningFruitsPerShoot == null || targetFruitsPerShoot == null || targetFruitsPerShoot <= 0.0) {
            return null;
        }
        return (preThinningFruitsPerShoot / targetFruitsPerShoot - 1.0) * 100.0;
    }

    /**
     * Settled harvest per hectare.
     *
     * @return kilograms per hectare, or null when the area is unknown
     */
    public Double kgPerHectare() {
        return areaHectares == null || areaHectares <= 0.0 ? null : totalKg / areaHectares;
    }
}
