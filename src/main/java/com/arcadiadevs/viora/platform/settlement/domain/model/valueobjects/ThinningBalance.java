package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * Balance of the settled campaign against its thinning prescription, frozen with the settlement.
 *
 * <p>Only comparable magnitudes are compared: prescribed versus actual percentage of fruits removed.
 * Harvested kilograms are not compared with removed kilograms because they measure different things.</p>
 *
 * @param status                       thinning evidence for the campaign
 * @param executedDate                 date of the labor, when recorded
 * @param prescribedRemovalPercentage  percentage the prescription asked to remove, when recorded
 * @param actualRemovalPercentage      percentage actually removed, when recorded
 * @param deviationPercentagePoints    actual minus prescribed, in percentage points, when both are known
 */
public record ThinningBalance(
        ThinningComplianceStatus status,
        LocalDate executedDate,
        Double prescribedRemovalPercentage,
        Double actualRemovalPercentage,
        Double deviationPercentagePoints
) {

    public ThinningBalance {
        if (status == null) {
            throw new IllegalArgumentException("settlement.thinning_balance.invalid");
        }
    }

    /** Balance of a campaign without any recorded thinning labor. */
    public static ThinningBalance notRecorded() {
        return new ThinningBalance(ThinningComplianceStatus.NOT_RECORDED, null, null, null, null);
    }

    /**
     * Builds the balance from the thinning execution of the campaign.
     *
     * @param record execution projected from Thinning
     * @return balance with the deviation between actual and prescribed removal
     */
    public static ThinningBalance of(ThinningExecutionRecord record) {
        if (record == null) {
            return notRecorded();
        }
        Double deviation = record.prescribedRemovalPercentage() == null ? null
                : record.actualRemovalPercentage() - record.prescribedRemovalPercentage();
        return new ThinningBalance(
                record.onTime() ? ThinningComplianceStatus.EXECUTED_ON_TIME : ThinningComplianceStatus.EXECUTED_LATE,
                record.executedDate(), record.prescribedRemovalPercentage(), record.actualRemovalPercentage(),
                deviation);
    }
}
