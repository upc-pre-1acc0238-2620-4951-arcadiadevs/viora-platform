package com.arcadiadevs.viora.platform.thinning.domain.model.aggregates;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberProjection;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.ConfirmationId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.ExecutionTimeliness;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.LoadBalance;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Immutable snapshot representing the execution confirmation of thinning labor.
 *
 * @param id                      unique identifier of the confirmation
 * @param executionDate           date when thinning was conducted
 * @param actualRemovalPercentage percentage of green fruits removed
 * @param laborCrewSize           crew size
 * @param timeliness              qualification (OPTIMAL or LATE)
 * @param recordedAt              timestamp when confirmation was logged
 * @param removedKg               actual removed biomass in kilograms
 * @param notes                   optional field observations
 * @param loadBalance             crop load left by the labor
 * @param caliberProjection       commercial caliber projection issued with the confirmation
 */
public record ExecutionConfirmationSnapshot(
        ConfirmationId id,
        LocalDate executionDate,
        Double actualRemovalPercentage,
        Integer laborCrewSize,
        ExecutionTimeliness timeliness,
        Instant recordedAt,
        Double removedKg,
        String notes,
        LoadBalance loadBalance,
        CaliberProjection caliberProjection
) {
}
