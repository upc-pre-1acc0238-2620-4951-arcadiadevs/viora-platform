package com.arcadiadevs.viora.platform.phenology.domain.services;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.DynamicErezPortion;

import java.util.List;

/**
 * Pure domain service implementing the Dynamic Model of chill accumulation
 * (Fishman, Erez and Couvillon, 1987; Erez et al., 1990).
 *
 * <p>The model simulates a two-step process, evaluated hour by hour:
 * <ol>
 *   <li>Cool temperatures build a thermally reversible intermediate product {@code x}; warm hours destroy it.</li>
 *   <li>Once {@code x} reaches 1, a share {@code xi} of it (larger with moderate temperatures) is fixed as an
 *       irreversible chill portion and the rest stays as intermediate.</li>
 * </ol>
 * The equations and the default parameters are the ones of the original spreadsheet by Erez and Fishman and of
 * {@code chillR::Dynamic_Model} (E0 = 4153.5, E1 = 12888.8, A0 = 139500, A1 = 2.567e18, slope = 1.6,
 * Tf = 277 K, temperatures in Kelvin as °C + 273). With them a constant 6–8 °C fixes about 0.8 portions a day,
 * and a constant 13 °C or warmer fixes none.</p>
 */
public final class ErezDynamicModelCalculator {

    private static final double E0 = 4153.5;
    private static final double E1 = 12888.8;
    private static final double A0 = 139500.0;
    private static final double A1 = 2.567e18;
    private static final double SLOPE = 1.6;
    private static final double TRANSITION_TEMPERATURE_KELVIN = 277.0;
    private static final double KELVIN_OFFSET = 273.0;

    /**
     * Chill portions Sevillana and Criolla olives need between June 1 and August 31 for an even flowering.
     */
    public static final double DEFAULT_VARIETAL_CHILL_THRESHOLD = 30.0;

    private ErezDynamicModelCalculator() {
    }

    /**
     * Computes the chill portions accumulated by a chronological series of hourly temperatures.
     *
     * @param hourlyTemps the hourly temperatures in °C, oldest first; null values are skipped
     * @return the accumulated {@link DynamicErezPortion}, rounded to 2 decimals
     */
    public static DynamicErezPortion computePortions(List<Double> hourlyTemps) {
        if (hourlyTemps == null || hourlyTemps.isEmpty()) {
            return DynamicErezPortion.zero();
        }
        double[] cumulative = cumulativePortions(hourlyTemps);
        double total = cumulative.length == 0 ? 0.0 : cumulative[cumulative.length - 1];
        return new DynamicErezPortion(Math.round(total * 100.0) / 100.0);
    }

    /**
     * Computes the running total of chill portions after each hour of the series.
     *
     * @param hourlyTemps the hourly temperatures in °C, oldest first; a null value repeats the previous total
     * @return one running total per input hour (same length as the input)
     */
    public static double[] cumulativePortions(List<Double> hourlyTemps) {
        if (hourlyTemps == null) {
            return new double[0];
        }
        double[] cumulative = new double[hourlyTemps.size()];
        double intermediate = 0.0;
        double total = 0.0;

        for (int i = 0; i < hourlyTemps.size(); i++) {
            Double celsius = hourlyTemps.get(i);
            if (celsius != null) {
                double kelvin = celsius + KELVIN_OFFSET;
                double sigmoid = Math.exp(SLOPE * TRANSITION_TEMPERATURE_KELVIN * (kelvin - TRANSITION_TEMPERATURE_KELVIN) / kelvin);
                double fixedShare = sigmoid / (1.0 + sigmoid);
                double equilibrium = (A0 / A1) * Math.exp((E1 - E0) / kelvin);
                double destructionRate = A1 * Math.exp(-E1 / kelvin);

                intermediate = equilibrium - (equilibrium - intermediate) * Math.exp(-destructionRate);
                if (intermediate >= 1.0) {
                    total += intermediate * fixedShare;
                    intermediate = intermediate * (1.0 - fixedShare);
                }
            }
            cumulative[i] = total;
        }
        return cumulative;
    }

    /**
     * Evaluates whether the accumulated chilling portions fulfill the varietal physiological requirement.
     *
     * @param portions        the accumulated chilling portions
     * @param targetThreshold the target threshold
     * @return "SATISFIED" if portions &ge; targetThreshold; otherwise "DEFICIENT"
     */
    public static String evaluateSatisfactionStatus(double portions, double targetThreshold) {
        return portions >= targetThreshold ? "SATISFIED" : "DEFICIENT";
    }

    /**
     * Computes the percentage of the chilling requirement completed, rounded to 2 decimal places.
     *
     * @param portions        the accumulated chilling portions
     * @param targetThreshold the target threshold
     * @return the percentage completion
     */
    public static double computeCompletionPercentage(double portions, double targetThreshold) {
        if (targetThreshold <= 0.0) {
            return 0.0;
        }
        return Math.round((portions / targetThreshold) * 10000.0) / 100.0;
    }
}
