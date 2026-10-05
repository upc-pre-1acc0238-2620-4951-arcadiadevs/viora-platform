package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Load-response caliber model of one olive variety, fitted from real Viora harvest observations.
 *
 * <p>Model: {@code ln W = intercept - beta x ln L}, where {@code W} is the mean fruit weight in grams and
 * {@code L} the residual crop load in fruits per shoot. It is only valid inside the observed load range.</p>
 *
 * @param variety            olive variety the model belongs to
 * @param intercept          fitted intercept {@code a}
 * @param beta               fitted sensitivity of fruit weight to crop load (positive)
 * @param residualStdError   standard error {@code s} of the log-weight residuals
 * @param observationCount   number of observations {@code n} used in the fit
 * @param plotCount          number of distinct plots behind the observations
 * @param meanLogLoad        mean of {@code ln L} over the observations
 * @param sumSquaresLogLoad  {@code Sxx}, sum of squared deviations of {@code ln L}
 * @param minResidualLoad    lowest observed residual load
 * @param maxResidualLoad    highest observed residual load
 */
public record VarietyCaliberModel(
        String variety,
        double intercept,
        double beta,
        double residualStdError,
        int observationCount,
        int plotCount,
        double meanLogLoad,
        double sumSquaresLogLoad,
        double minResidualLoad,
        double maxResidualLoad
) {

    public VarietyCaliberModel {
        if (variety == null || variety.isBlank()) {
            throw new IllegalArgumentException("thinning.calibration.variety.null_or_empty");
        }
        if (!Double.isFinite(intercept) || !Double.isFinite(beta) || beta <= 0.0
                || !Double.isFinite(residualStdError) || residualStdError < 0.0
                || observationCount < 3 || plotCount < 1
                || !Double.isFinite(meanLogLoad) || !Double.isFinite(sumSquaresLogLoad) || sumSquaresLogLoad <= 0.0
                || !Double.isFinite(minResidualLoad) || minResidualLoad <= 0.0
                || !Double.isFinite(maxResidualLoad) || maxResidualLoad < minResidualLoad) {
            throw new IllegalArgumentException("thinning.calibration.model.invalid");
        }
    }

    /**
     * Tells whether a residual load lies inside the range observed during calibration.
     *
     * @param residualFruitsPerShoot residual load to evaluate
     * @return {@code true} when the model may be applied without extrapolating
     */
    public boolean covers(double residualFruitsPerShoot) {
        return residualFruitsPerShoot >= minResidualLoad && residualFruitsPerShoot <= maxResidualLoad;
    }

    /**
     * Predicts the natural logarithm of the mean fruit weight.
     *
     * @param residualFruitsPerShoot residual load, positive
     * @return {@code ln W}
     */
    public double predictLogWeight(double residualFruitsPerShoot) {
        return intercept - beta * Math.log(residualFruitsPerShoot);
    }

    /**
     * Half width, in log-weight units, of the prediction interval for a new harvest.
     *
     * <p>{@code t x s x sqrt(1 + 1/n + (ln L - meanLogLoad)^2 / Sxx)}</p>
     *
     * @param residualFruitsPerShoot residual load, positive
     * @param studentT               Student t quantile for {@code n - 2} degrees of freedom
     * @return half width of the interval around {@link #predictLogWeight(double)}
     */
    public double predictionHalfWidth(double residualFruitsPerShoot, double studentT) {
        double deviation = Math.log(residualFruitsPerShoot) - meanLogLoad;
        return studentT * residualStdError
                * Math.sqrt(1.0 + 1.0 / observationCount + deviation * deviation / sumSquaresLogLoad);
    }
}
