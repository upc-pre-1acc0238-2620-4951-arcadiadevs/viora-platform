package com.arcadiadevs.viora.platform.phenology.domain.services;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.DynamicErezPortion;

import java.util.List;

/**
 * Pure domain service implementing the Dynamic Model for chill accumulation
 * developed by Fishman, Erez and Couvillon (1987).
 *
 * <p>The model simulates a two-step biological process:
 * <ol>
 *   <li>Synthesis of a thermally reversible intermediate product ($x$) at cool temperatures:
 *       $$\frac{dx}{dt} = \frac{E_0 \cdot A_0 \cdot e^{-E_0/T_K}}{1 + e^{(E_1 - E_0)/T_K}} - x \cdot A_1 \cdot e^{-E_1/T_K}$$
 *       Where temperatures between 2°C and 12°C promote accumulation, and high temperatures (&gt; 24°C)
 *       cause rapid thermal destruction of the intermediate.</li>
 *   <li>When the intermediate $x$ reaches a critical threshold of 1.0, an irreversible portion is fixed:
 *       $x \leftarrow x - 1.0$ (or multiplied by transfer factor), incrementing accumulated portions.</li>
 * </ol>
 * </p>
 */
public final class ErezDynamicModelCalculator {

    // Standard biological kinetic parameters (Erez, Fishman, Couvillon 1987)
    private static final double A0 = 139500.0;
    private static final double A1 = 2570000000000000.0; // 2.57e15
    private static final double E0 = 4153.5;
    private static final double E1 = 12888.8;
    private static final double SLP = 1.6;
    private static final double TETMLT = 277.0; // ~ 4.0 °C in Kelvin
    private static final double KELVIN_OFFSET = 273.15;

    /**
     * Standard varietal chilling portions target required for olive floral bud release.
     */
    public static final double DEFAULT_VARIETAL_CHILL_THRESHOLD = 27.0;

    private ErezDynamicModelCalculator() {
    }

    /**
     * Evaluates whether the accumulated chilling portions fulfill the varietal physiological requirement.
     *
     * @param portions the accumulated chilling portions
     * @param targetThreshold the target threshold
     * @return "SATISFIED" if portions >= targetThreshold; otherwise "DEFICIENT"
     */
    public static String evaluateSatisfactionStatus(double portions, double targetThreshold) {
        return portions >= targetThreshold ? "SATISFIED" : "DEFICIENT";
    }

    /**
     * Computes the percentage of the chilling requirement completed, rounded to 2 decimal places.
     *
     * @param portions the accumulated chilling portions
     * @param targetThreshold the target threshold
     * @return the percentage completion
     */
    public static double computeCompletionPercentage(double portions, double targetThreshold) {
        if (targetThreshold <= 0.0) {
            return 0.0;
        }
        return Math.round((portions / targetThreshold) * 10000.0) / 100.0;
    }

    /**
     * Computes the cumulative chilling portions from a chronological series of hourly temperatures in Celsius.
     *
     * @param hourlyTemps the list of hourly temperatures in °C
     * @return the {@link DynamicErezPortion} accumulated
     */
    public static DynamicErezPortion computePortions(List<Double> hourlyTemps) {
        if (hourlyTemps == null || hourlyTemps.isEmpty()) {
            return DynamicErezPortion.zero();
        }

        double accumulatedPortions = 0.0;
        double intermediateProduct = 0.0;

        for (Double tempCelsius : hourlyTemps) {
            if (tempCelsius == null) {
                continue;
            }

            double tk = tempCelsius + KELVIN_OFFSET;
            if (tk <= 0.0) {
                continue;
            }

            // Temperature factors
            double ftm = (SLP * TETMLT * (tk - TETMLT)) / tk;
            double s = Math.exp(ftm) / (1.0 + Math.exp(ftm));

            double ak0 = A0 * Math.exp(-E0 / tk);
            double ak1 = A1 * Math.exp(-E1 / tk);

            // Equilibrium level and kinetic rate
            double xi = ak0 / ak1;
            double rate = ak1;

            // Hourly transition
            double nextInter = xi - (xi - intermediateProduct) * Math.exp(-rate);

            // High heat destruction (> 24 °C accelerates breakdown)
            if (tempCelsius >= 24.0) {
                nextInter = Math.max(0.0, nextInter * 0.75);
            }

            // Transfer to irreversible chilling portions when threshold reached
            if (nextInter >= 1.0) {
                double portionsToAdd = Math.floor(nextInter);
                accumulatedPortions += portionsToAdd;
                intermediateProduct = nextInter - portionsToAdd;
            } else {
                intermediateProduct = Math.max(0.0, nextInter);
            }
        }

        double rounded = Math.round(accumulatedPortions * 100.0) / 100.0;
        return new DynamicErezPortion(Math.max(0.0, rounded));
    }
}
