package com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

/** Commercial caliber expected at harvest; figures are only present when the status is ESTIMATED. */
@Schema(description = "Caliber projection in fruits per kilogram graded with the IOC table olive size scale "
        + "(COI/OT/NC no. 1). Lower fruits/kg means bigger fruit. Without a reliable calibration the status "
        + "explains why no figure is given.")
public record CaliberProjectionResource(
        @Schema(allowableValues = {"ESTIMATED", "NOT_CALIBRATED", "NOT_ESTIMATED_LATE",
                "OUTSIDE_CALIBRATION_RANGE", "NOT_APPLICABLE"})
        String status,
        @Schema(description = "Central estimate in fruits per kilogram", example = "104.3", nullable = true)
        Double mostLikelyFruitsPerKg,
        @Schema(description = "Large-fruit end of the prediction interval", example = "93.8", nullable = true)
        Double fruitsPerKgLow,
        @Schema(description = "Small-fruit end of the prediction interval", example = "116.0", nullable = true)
        Double fruitsPerKgHigh,
        @Schema(description = "IOC size grade of the central estimate", example = "101/110", nullable = true)
        String mostLikelySizeGrade,
        @Schema(description = "IOC size grade of the large-fruit end", example = "91/100", nullable = true)
        String sizeGradeLow,
        @Schema(description = "IOC size grade of the small-fruit end", example = "111/120", nullable = true)
        String sizeGradeHigh,
        @Schema(description = "Coverage of the prediction interval", example = "0.8", nullable = true)
        Double confidenceLevel,
        @Schema(description = "Real harvest observations available for the plot variety", example = "3")
        Integer calibrationObservations,
        @Schema(description = "Minimum observations before the variety model can be activated", example = "8")
        Integer requiredObservations,
        @Schema(description = "Projection method identifier", example = "LOAD_RESPONSE_V1")
        String model) {
}
