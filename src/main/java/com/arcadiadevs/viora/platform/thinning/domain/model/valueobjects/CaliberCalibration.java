package com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects;

/**
 * Calibration state of the caliber model for the variety of a plot.
 *
 * @param variety          variety evaluated, or {@code null} when the plot variety is unknown
 * @param observationCount real observations available for the variety
 * @param model            fitted model, or {@code null} while the activation conditions are not met
 */
public record CaliberCalibration(String variety, int observationCount, VarietyCaliberModel model) {

    public CaliberCalibration {
        if (observationCount < 0) {
            throw new IllegalArgumentException("thinning.calibration.count.negative");
        }
    }

    /**
     * Calibration for a plot whose variety could not be resolved.
     *
     * @return an empty, uncalibrated state
     */
    public static CaliberCalibration none() {
        return new CaliberCalibration(null, 0, null);
    }

    /**
     * Calibration that does not meet the activation conditions yet.
     *
     * @param variety          evaluated variety
     * @param observationCount observations available so far
     * @return an uncalibrated state
     */
    public static CaliberCalibration uncalibrated(String variety, int observationCount) {
        return new CaliberCalibration(variety, observationCount, null);
    }

    /**
     * Calibration backed by a fitted model.
     *
     * @param model fitted variety model
     * @return a calibrated state
     */
    public static CaliberCalibration calibrated(VarietyCaliberModel model) {
        return new CaliberCalibration(model.variety(), model.observationCount(), model);
    }

    public boolean isCalibrated() {
        return model != null;
    }
}
