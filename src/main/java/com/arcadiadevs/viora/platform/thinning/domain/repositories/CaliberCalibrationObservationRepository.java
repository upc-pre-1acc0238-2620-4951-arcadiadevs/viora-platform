package com.arcadiadevs.viora.platform.thinning.domain.repositories;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberCalibrationObservation;

import java.util.List;

/**
 * Domain port for the real harvest observations that calibrate the caliber model of each variety.
 */
public interface CaliberCalibrationObservationRepository {

    /**
     * Lists every observation of a variety.
     *
     * @param variety olive variety (Orchard published name)
     * @return observations of the variety, possibly empty
     */
    List<CaliberCalibrationObservation> findByVariety(String variety);

    /**
     * Stores an observation; a second observation for the same plot and campaign replaces the first one.
     *
     * @param observation observation to store
     * @return the stored observation
     */
    CaliberCalibrationObservation save(CaliberCalibrationObservation observation);
}
