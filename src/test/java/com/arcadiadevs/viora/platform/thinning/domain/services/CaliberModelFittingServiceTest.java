package com.arcadiadevs.viora.platform.thinning.domain.services;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberCalibrationObservation;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.arcadiadevs.viora.platform.thinning.ThinningExecutionFixtures.*;
import static org.junit.jupiter.api.Assertions.*;

class CaliberModelFittingServiceTest {

    @Test
    void recoversTheGeneratingCurveFromExactData() {
        var calibration = CaliberModelFittingService.calibrate("SEVILLANA", exactObservations("SEVILLANA"));
        assertTrue(calibration.isCalibrated());
        var model = calibration.model();
        assertEquals("SEVILLANA", model.variety());
        assertEquals(0.6, model.beta(), 1e-9);
        assertEquals(Math.log(10.0) + 0.6 * Math.log(30.0), model.intercept(), 1e-9);
        assertEquals(0.0, model.residualStdError(), 1e-9);
        assertEquals(8, model.observationCount());
        assertEquals(4, model.plotCount());
        assertEquals(18.0, model.minResidualLoad());
        assertEquals(45.0, model.maxResidualLoad());
    }

    @Test
    void fitsNoisyDataWithAPositiveSignificantSensitivity() {
        var calibration = CaliberModelFittingService.calibrate("SEVILLANA", noisyObservations("SEVILLANA"));
        assertTrue(calibration.isCalibrated());
        assertTrue(calibration.model().beta() > 0.35 && calibration.model().beta() < 0.85);
        assertTrue(calibration.model().residualStdError() > 0.0);
    }

    @Test
    void staysUncalibratedWithFewerThanEightObservations() {
        var calibration = CaliberModelFittingService.calibrate("CRIOLLA",
                exactObservations("CRIOLLA").subList(0, 7));
        assertFalse(calibration.isCalibrated());
        assertEquals(7, calibration.observationCount());
        assertEquals("CRIOLLA", calibration.variety());
    }

    @Test
    void requiresThreeDistinctPlots() {
        var twoPlots = new ArrayList<CaliberCalibrationObservation>();
        var source = exactObservations("SEVILLANA");
        String[] plots = {UUID.randomUUID().toString(), UUID.randomUUID().toString()};
        for (int i = 0; i < source.size(); i++) {
            var original = source.get(i);
            twoPlots.add(new CaliberCalibrationObservation(new PlotId(plots[i % 2]), original.campaignYear(),
                    original.variety(), original.residualFruitsPerMeter(), original.commercialFruitsPerKg()));
        }
        assertFalse(CaliberModelFittingService.calibrate("SEVILLANA", twoPlots).isCalibrated());
    }

    @Test
    void requiresAWideEnoughLoadRange() {
        var narrow = new ArrayList<CaliberCalibrationObservation>();
        for (int i = 0; i < 8; i++) {
            double load = 30.0 + i;
            narrow.add(observation(i, load, 1000.0 / (10.0 * Math.pow(load / 30.0, -0.6))));
        }
        assertFalse(CaliberModelFittingService.calibrate("SEVILLANA", narrow).isCalibrated());
    }

    @Test
    void refusesARelationWithoutEvidenceThatLoadReducesFruitSize() {
        var flat = new ArrayList<CaliberCalibrationObservation>();
        var increasing = new ArrayList<CaliberCalibrationObservation>();
        double[] loads = {18, 22, 26, 30, 34, 38, 42, 45};
        for (int i = 0; i < loads.length; i++) {
            flat.add(observation(i, loads[i], 100.0));
            increasing.add(observation(i, loads[i], 1000.0 / (10.0 * Math.pow(loads[i] / 30.0, 0.6))));
        }
        assertFalse(CaliberModelFittingService.calibrate("SEVILLANA", flat).isCalibrated());
        assertFalse(CaliberModelFittingService.calibrate("SEVILLANA", increasing).isCalibrated());
    }

    @Test
    void returnsNoCalibrationForUnknownVariety() {
        assertFalse(CaliberModelFittingService.calibrate(null, List.of()).isCalibrated());
        assertNull(CaliberModelFittingService.calibrate(" ", exactObservations("SEVILLANA")).variety());
        assertEquals(0, CaliberModelFittingService.calibrate("ARBEQUINA", null).observationCount());
    }

    private static CaliberCalibrationObservation observation(int index, double load, double fruitsPerKg) {
        return new CaliberCalibrationObservation(new PlotId("00000000-0000-0000-0000-00000000000" + (index % 4)),
                new CampaignYear(2018 + index), "SEVILLANA", load, fruitsPerKg);
    }
}
