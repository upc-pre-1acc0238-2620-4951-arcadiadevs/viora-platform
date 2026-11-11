package com.arcadiadevs.viora.platform.territory.domain.model.valueobjects;

/**
 * The intake of one sector of a campaign.
 *
 * @param sector         the sector, «Por confirmar» or no sector
 * @param plots          the member plots counted there
 * @param sampledPlots   those sampled this campaign, or those that settled a past one
 * @param kilograms      the projected or settled kilograms; null when none of its plots can tell
 * @param greenKg        the green part of them; null like kilograms
 * @param blackKg        the black part of them; null like kilograms
 * @param unclassifiedKg the part of plots without settled history; null like kilograms
 */
public record SectorIntake(SectorPlacement sector, int plots, int sampledPlots, Double kilograms, Double greenKg,
                           Double blackKg, Double unclassifiedKg) {
}
