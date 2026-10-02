package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Projected commercial caliber at harvest, kept with the execution evidence so the producer always sees
 * the projection that was issued, together with its uncertainty.
 *
 * <p>A lower fruits-per-kilogram value means bigger fruit, so {@code fruitsPerKgLow} is the large-fruit end
 * of the interval and {@code fruitsPerKgHigh} the small-fruit end.</p>
 *
 * @param status                  projection outcome
 * @param mostLikelyFruitsPerKg   central estimate, only when {@code ESTIMATED}
 * @param fruitsPerKgLow          large-fruit end of the prediction interval, only when {@code ESTIMATED}
 * @param fruitsPerKgHigh         small-fruit end of the prediction interval, only when {@code ESTIMATED}
 * @param confidenceLevel         coverage of the prediction interval, only when {@code ESTIMATED}
 * @param calibrationObservations real observations available for the variety when projecting
 * @param modelVersion            identifier of the projection method
 */
public record CaliberProjection(
        CaliberProjectionStatus status,
        Double mostLikelyFruitsPerKg,
        Double fruitsPerKgLow,
        Double fruitsPerKgHigh,
        Double confidenceLevel,
        int calibrationObservations,
        String modelVersion
) {

    /** Identifier of the self-calibrated load-response method. */
    public static final String MODEL_VERSION = "LOAD_RESPONSE_V1";

    public CaliberProjection {
        if (status == null || modelVersion == null || modelVersion.isBlank() || calibrationObservations < 0) {
            throw new IllegalArgumentException("thinning.caliber.projection.invalid");
        }
        if (status == CaliberProjectionStatus.ESTIMATED) {
            if (!isPositive(mostLikelyFruitsPerKg) || !isPositive(fruitsPerKgLow) || !isPositive(fruitsPerKgHigh)
                    || fruitsPerKgLow > mostLikelyFruitsPerKg || mostLikelyFruitsPerKg > fruitsPerKgHigh
                    || confidenceLevel == null || confidenceLevel <= 0.0 || confidenceLevel >= 1.0) {
                throw new IllegalArgumentException("thinning.caliber.projection.invalid");
            }
        } else if (mostLikelyFruitsPerKg != null || fruitsPerKgLow != null || fruitsPerKgHigh != null
                || confidenceLevel != null) {
            throw new IllegalArgumentException("thinning.caliber.projection.invalid");
        }
    }

    /**
     * Creates a projection without figures.
     *
     * @param status                  reason why no caliber is estimated
     * @param calibrationObservations real observations available for the variety
     * @return projection without numbers
     */
    public static CaliberProjection withoutEstimate(CaliberProjectionStatus status, int calibrationObservations) {
        return new CaliberProjection(status, null, null, null, null, calibrationObservations, MODEL_VERSION);
    }

    /** Size grade of the central estimate, or {@code null} when not estimated. */
    public String mostLikelySizeGrade() {
        return mostLikelyFruitsPerKg == null ? null : CommercialSizeScale.gradeOf(mostLikelyFruitsPerKg);
    }

    /** Size grade of the large-fruit end of the interval, or {@code null} when not estimated. */
    public String sizeGradeLow() {
        return fruitsPerKgLow == null ? null : CommercialSizeScale.gradeOf(fruitsPerKgLow);
    }

    /** Size grade of the small-fruit end of the interval, or {@code null} when not estimated. */
    public String sizeGradeHigh() {
        return fruitsPerKgHigh == null ? null : CommercialSizeScale.gradeOf(fruitsPerKgHigh);
    }

    private static boolean isPositive(Double value) {
        return value != null && Double.isFinite(value) && value > 0.0;
    }
}
