package com.arcadiadevs.viora.platform.thinning.domain.services;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberCalibration;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberCalibrationObservation;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.VarietyCaliberModel;

import java.util.List;

/**
 * Pure domain service fitting the caliber model of a variety from real Viora harvest observations.
 *
 * <p>Ordinary least squares over {@code (x = ln L, y = ln W)}: {@code ln W = a - beta x ln L}.
 * No literature coefficient is hardcoded: the model is only activated when the data justify it.</p>
 *
 * <p>Activation conditions (all required):</p>
 * <ul>
 *     <li>at least {@value #MIN_OBSERVATIONS} observations;</li>
 *     <li>at least {@value #MIN_PLOTS} distinct plots;</li>
 *     <li>highest load at least {@value #MIN_LOAD_SPREAD} times the lowest load;</li>
 *     <li>beta significantly positive: the one-sided 95% lower bound of beta is above zero.</li>
 * </ul>
 */
public final class CaliberModelFittingService {

    public static final int MIN_OBSERVATIONS = 8;
    public static final int MIN_PLOTS = 3;
    public static final double MIN_LOAD_SPREAD = 1.5;

    private CaliberModelFittingService() {
    }

    /**
     * Evaluates the calibration of a variety.
     *
     * @param variety      variety whose observations are evaluated
     * @param observations real observations of that variety
     * @return calibrated state with the fitted model, or an uncalibrated state with the observation count
     */
    public static CaliberCalibration calibrate(String variety, List<CaliberCalibrationObservation> observations) {
        if (variety == null || variety.isBlank()) {
            return CaliberCalibration.none();
        }
        List<CaliberCalibrationObservation> data = observations == null ? List.of() : observations;
        int n = data.size();
        if (n < MIN_OBSERVATIONS) {
            return CaliberCalibration.uncalibrated(variety, n);
        }
        long plots = data.stream().map(CaliberCalibrationObservation::plotId).distinct().count();
        double minLoad = data.stream().mapToDouble(CaliberCalibrationObservation::residualFruitsPerMeter).min().orElseThrow();
        double maxLoad = data.stream().mapToDouble(CaliberCalibrationObservation::residualFruitsPerMeter).max().orElseThrow();
        if (plots < MIN_PLOTS || maxLoad < MIN_LOAD_SPREAD * minLoad) {
            return CaliberCalibration.uncalibrated(variety, n);
        }

        double[] x = new double[n];
        double[] y = new double[n];
        double meanX = 0.0;
        double meanY = 0.0;
        for (int i = 0; i < n; i++) {
            x[i] = Math.log(data.get(i).residualFruitsPerMeter());
            y[i] = Math.log(data.get(i).fruitWeightGrams());
            meanX += x[i] / n;
            meanY += y[i] / n;
        }
        double sxx = 0.0;
        double sxy = 0.0;
        for (int i = 0; i < n; i++) {
            sxx += (x[i] - meanX) * (x[i] - meanX);
            sxy += (x[i] - meanX) * (y[i] - meanY);
        }
        double slope = sxy / sxx;
        double intercept = meanY - slope * meanX;
        double sse = 0.0;
        for (int i = 0; i < n; i++) {
            double residual = y[i] - (intercept + slope * x[i]);
            sse += residual * residual;
        }
        int degreesOfFreedom = n - 2;
        double residualStdError = Math.sqrt(sse / degreesOfFreedom);
        double beta = -slope;
        double betaStdError = residualStdError / Math.sqrt(sxx);
        if (beta - StudentTQuantiles.p95(degreesOfFreedom) * betaStdError <= 0.0) {
            return CaliberCalibration.uncalibrated(variety, n);
        }
        return CaliberCalibration.calibrated(new VarietyCaliberModel(variety, intercept, beta, residualStdError,
                n, (int) plots, meanX, sxx, minLoad, maxLoad));
    }
}
