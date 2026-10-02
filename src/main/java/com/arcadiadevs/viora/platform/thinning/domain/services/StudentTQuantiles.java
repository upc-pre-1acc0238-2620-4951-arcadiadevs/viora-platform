package com.arcadiadevs.viora.platform.thinning.domain.services;

/**
 * One-sided quantiles of the Student t distribution used by the caliber calibration statistics.
 *
 * <p>Up to 30 degrees of freedom the values come from the standard t table; above that, the Cornish-Fisher
 * expansion {@code t = z + (z^3 + z) / (4v) + (5z^5 + 16z^3 + 3z) / (96v^2)} is accurate to three decimals.</p>
 */
public final class StudentTQuantiles {

    private static final double[] P90 = {
            3.078, 1.886, 1.638, 1.533, 1.476, 1.440, 1.415, 1.397, 1.383, 1.372,
            1.363, 1.356, 1.350, 1.345, 1.341, 1.337, 1.333, 1.330, 1.328, 1.325,
            1.323, 1.321, 1.319, 1.318, 1.316, 1.315, 1.314, 1.313, 1.311, 1.310
    };

    private static final double[] P95 = {
            6.314, 2.920, 2.353, 2.132, 2.015, 1.943, 1.895, 1.860, 1.833, 1.812,
            1.796, 1.782, 1.771, 1.761, 1.753, 1.746, 1.740, 1.734, 1.729, 1.725,
            1.721, 1.717, 1.714, 1.711, 1.708, 1.706, 1.703, 1.701, 1.699, 1.697
    };

    private static final double Z90 = 1.281552;
    private static final double Z95 = 1.644854;

    private StudentTQuantiles() {
    }

    /**
     * Quantile 0.90: half width multiplier of a two-sided 80% interval.
     *
     * @param degreesOfFreedom positive degrees of freedom
     * @return {@code t(0.90, df)}
     */
    public static double p90(int degreesOfFreedom) {
        return quantile(P90, Z90, degreesOfFreedom);
    }

    /**
     * Quantile 0.95: one-sided 95% test, equivalent to the bound of a two-sided 90% interval.
     *
     * @param degreesOfFreedom positive degrees of freedom
     * @return {@code t(0.95, df)}
     */
    public static double p95(int degreesOfFreedom) {
        return quantile(P95, Z95, degreesOfFreedom);
    }

    private static double quantile(double[] table, double z, int degreesOfFreedom) {
        if (degreesOfFreedom < 1) {
            throw new IllegalArgumentException("thinning.calibration.degrees_of_freedom.invalid");
        }
        if (degreesOfFreedom <= table.length) {
            return table[degreesOfFreedom - 1];
        }
        double v = degreesOfFreedom;
        double z3 = z * z * z;
        double z5 = z3 * z * z;
        return z + (z3 + z) / (4.0 * v) + (5.0 * z5 + 16.0 * z3 + 3.0 * z) / (96.0 * v * v);
    }
}
