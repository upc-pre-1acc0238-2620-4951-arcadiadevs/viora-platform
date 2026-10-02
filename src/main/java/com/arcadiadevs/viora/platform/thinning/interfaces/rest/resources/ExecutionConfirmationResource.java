package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;

/** Recorded confirmation, the qualification of its execution date, the load it leaves and the caliber outlook. */
@Schema(description = "Persisted execution confirmation; LATE is a successful registration outside the recommended window")
public record ExecutionConfirmationResource(
        String prescriptionId, String confirmationId,
        @Schema(allowableValues = {"OPTIMAL", "LATE"}) String confirmationStatus,
        LocalDate executedDate, Double actualRemovalPercentage, Double removedKg,
        Integer laborCrewSize, String notes, boolean isOpportune, Instant recordedAt,
        @Schema(description = "Localized warning about reduced effectiveness for late labor; null when optimal")
        String warning,
        @Schema(description = "Crop load left by the labor")
        LoadBalanceResource loadBalance,
        @Schema(description = "Commercial caliber projection issued with the confirmation")
        CaliberProjectionResource caliberProjection) {
}
