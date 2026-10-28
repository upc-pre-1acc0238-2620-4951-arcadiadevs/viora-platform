package com.arcadiadevs.viora.platform.thinning.application.acl;

import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CommercialSizeScale;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import com.arcadiadevs.viora.platform.thinning.domain.services.SamplingCoverageEvaluator;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of the Thinning ACL facade, publishing the commercial size scale of the context to the contexts
 * that settle a harvest and have to name the caliber of the olives they delivered.
 *
 * <p>The label comes from {@link CommercialSizeScale}, the single owner of the scale, so no consumer has to know
 * how a caliber maps onto a grade.</p>
 */
@Service
public class ThinningContextFacadeImpl implements ThinningContextFacade {

    private final FruitThinningPrescriptionRepository prescriptionRepository;

    public ThinningContextFacadeImpl(FruitThinningPrescriptionRepository prescriptionRepository) {
        this.prescriptionRepository = prescriptionRepository;
    }

    @Override
    public Optional<String> findCommercialSizeGrade(Double fruitsPerKg) {
        if (fruitsPerKg == null) {
            return Optional.empty();
        }
        return Optional.of(CommercialSizeScale.gradeOf(fruitsPerKg));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CampaignThinningPlan> findCampaignThinningPlan(String plotId, int campaignYear) {
        final PlotId id;
        final CampaignYear year;
        try {
            id = new PlotId(plotId);
            year = new CampaignYear(campaignYear);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
        return prescriptionRepository.findByPlotIdAndCampaignYear(id, year).map(prescription -> {
            var snapshot = prescription.snapshot();
            var rounds = snapshot.samplingRounds();
            Double preThinning = SamplingCoverageEvaluator.isRepresentativeFromSnapshots(rounds)
                    ? SamplingCoverageEvaluator.computeMeanFruitsPerShootFromSnapshots(rounds)
                    : null;
            var load = snapshot.sustainableLoad();
            return new CampaignThinningPlan(
                    preThinning,
                    load == null ? null : load.targetFruitsPerShoot(),
                    load == null ? null : load.percentageToRemove(),
                    load == null ? null : load.windowOpensOn(),
                    load == null ? null : load.windowClosesOn());
        });
    }
}
