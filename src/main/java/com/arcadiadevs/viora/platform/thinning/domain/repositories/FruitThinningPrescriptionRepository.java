package com.arcadiadevs.viora.platform.thinning.domain.repositories;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionId;

import java.util.Optional;

/**
 * Domain repository contract defining persistence operations for {@link FruitThinningPrescription} aggregates.
 */
public interface FruitThinningPrescriptionRepository {

    /**
     * Finds a thinning prescription aggregate by its domain identifier.
     *
     * @param id the prescription identifier
     * @return an {@link Optional} containing the aggregate if found, or empty otherwise
     */
    Optional<FruitThinningPrescription> findById(PrescriptionId id);

    /**
     * Finds a thinning prescription aggregate associated with a plot and campaign year.
     *
     * @param plotId the plot identifier
     * @param year   the campaign year
     * @return an {@link Optional} containing the aggregate if found, or empty otherwise
     */
    Optional<FruitThinningPrescription> findByPlotIdAndCampaignYear(PlotId plotId, CampaignYear year);

    /**
     * Persists the given prescription aggregate and returns the updated domain model.
     *
     * @param prescription the aggregate to save
     * @return the saved aggregate
     */
    FruitThinningPrescription save(FruitThinningPrescription prescription);
}
