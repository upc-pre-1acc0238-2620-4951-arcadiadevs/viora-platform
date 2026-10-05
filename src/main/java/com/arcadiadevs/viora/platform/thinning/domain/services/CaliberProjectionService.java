package com.arcadiadevs.viora.platform.thinning.domain.services;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberCalibration;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberProjection;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberProjectionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.ExecutionTimeliness;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.LoadBalance;

/**
 * Pure domain service projecting the commercial caliber expected at harvest after a thinning labor.
 *
 * <p>Steps: {@code ln W = a - beta x ln L_res}; 80% prediction interval
 * {@code ln W +/- t(0.90, n-2) x s x sqrt(1 + 1/n + (ln L_res - mean)^2 / Sxx)};
 * caliber {@code N = 1000 / W} fruits per kilogram, graded with the IOC size scale.</p>
 *
 * <p>Guards, evaluated in order: no fruit left, late execution, uncalibrated variety and load outside the
 * calibrated range. Each one returns a status without numbers instead of an unreliable figure.</p>
 */
public final class CaliberProjectionService {

    /** Coverage of the prediction interval shown to the producer. */
    public static final double CONFIDENCE_LEVEL = 0.80;

    private CaliberProjectionService() {
    }

    /**
     * Projects the caliber for a confirmed thinning labor.
     *
     * @param loadBalance  load balance left by the labor
     * @param timeliness   biological timeliness of the labor
     * @param calibration  calibration state of the plot variety
     * @return the caliber projection
     */
    public static CaliberProjection project(LoadBalance loadBalance, ExecutionTimeliness timeliness,
            CaliberCalibration calibration) {
        if (loadBalance == null || timeliness == null) {
            throw new IllegalArgumentException("thinning.caliber.projection.invalid");
        }
        CaliberCalibration state = calibration == null ? CaliberCalibration.none() : calibration;
        int observations = state.observationCount();
        double residualLoad = loadBalance.residualFruitsPerShoot();

        if (residualLoad <= 0.0) {
            return CaliberProjection.withoutEstimate(CaliberProjectionStatus.NOT_APPLICABLE, observations);
        }
        if (timeliness == ExecutionTimeliness.LATE) {
            return CaliberProjection.withoutEstimate(CaliberProjectionStatus.NOT_ESTIMATED_LATE, observations);
        }
        if (!state.isCalibrated()) {
            return CaliberProjection.withoutEstimate(CaliberProjectionStatus.NOT_CALIBRATED, observations);
        }
        var model = state.model();
        if (!model.covers(residualLoad)) {
            return CaliberProjection.withoutEstimate(CaliberProjectionStatus.OUTSIDE_CALIBRATION_RANGE, observations);
        }

        double logWeight = model.predictLogWeight(residualLoad);
        double halfWidth = model.predictionHalfWidth(residualLoad,
                StudentTQuantiles.p90(model.observationCount() - 2));
        // Full precision: size grades are decided on these values; rounding belongs to the presentation layer
        double mostLikely = 1000.0 / Math.exp(logWeight);
        double low = 1000.0 / Math.exp(logWeight + halfWidth);
        double high = 1000.0 / Math.exp(logWeight - halfWidth);
        return new CaliberProjection(CaliberProjectionStatus.ESTIMATED, mostLikely, low, high,
                CONFIDENCE_LEVEL, observations, CaliberProjection.MODEL_VERSION);
    }
}
