package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * Settlement's own projection of a thinning execution confirmed in the Thinning context.
 *
 * @param eventId                     published event identifier, used for idempotent consumption
 * @param confirmationId              Thinning confirmation identifier
 * @param plotId                      plot of the execution
 * @param campaignYear                campaign of the execution
 * @param executedDate                date the labor was completed
 * @param prescribedRemovalPercentage percentage of fruits the prescription asked to remove
 * @param actualRemovalPercentage     percentage of fruits actually removed
 * @param onTime                      whether the labor was executed before the window closed
 */
public record ThinningExecutionRecord(
        String eventId,
        String confirmationId,
        PlotId plotId,
        Integer campaignYear,
        LocalDate executedDate,
        Double prescribedRemovalPercentage,
        Double actualRemovalPercentage,
        boolean onTime
) {

    public ThinningExecutionRecord {
        if (eventId == null || eventId.isBlank() || confirmationId == null || confirmationId.isBlank()
                || plotId == null || campaignYear == null || executedDate == null) {
            throw new IllegalArgumentException("settlement.thinning_record.invalid");
        }
        if (!isPercentage(actualRemovalPercentage)
                || (prescribedRemovalPercentage != null && !isPercentage(prescribedRemovalPercentage))) {
            throw new IllegalArgumentException("settlement.thinning_record.invalid");
        }
    }

    private static boolean isPercentage(Double value) {
        return value != null && Double.isFinite(value) && value >= 0.0 && value <= 100.0;
    }
}
