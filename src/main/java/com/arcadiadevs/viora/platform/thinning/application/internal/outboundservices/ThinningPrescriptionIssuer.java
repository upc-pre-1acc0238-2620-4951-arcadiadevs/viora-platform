package com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices;

import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionBlocker;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.services.CropLoadBalancingCalculatorService;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Issues the thinning prescription once everything it needs is known: representative sampling, the
 * technical profile of the plot variety and the observed full bloom. Until then it only says what is
 * missing; it never fills a gap with a made-up figure.
 */
@Service
public class ThinningPrescriptionIssuer {

    private final ExternalOrchardService externalOrchardService;
    private final ThinningProfiles profiles;

    /**
     * Constructs the issuer.
     *
     * @param externalOrchardService outbound ACL used to read the plot variety
     * @param profiles               configured technical profiles
     */
    public ThinningPrescriptionIssuer(ExternalOrchardService externalOrchardService, ThinningProfiles profiles) {
        this.externalOrchardService = externalOrchardService;
        this.profiles = profiles;
    }

    /**
     * Finds the technical profile that applies to a plot.
     *
     * @param plotId the plot
     * @return the profile of the plot variety, or empty when the variety has none
     */
    public Optional<ThinningProfile> profileOf(PlotId plotId) {
        return externalOrchardService.findPlotVariety(plotId).flatMap(profiles::forVariety);
    }

    /**
     * Lists what still prevents the prescription from being issued.
     *
     * @param prescription the prescription
     * @return the missing inputs, empty when it was issued or can be issued now
     */
    public List<PrescriptionBlocker> blockers(FruitThinningPrescription prescription) {
        return prescription.blockers(profileOf(prescription.snapshot().plotId()).isPresent());
    }

    /**
     * Issues the prescription when nothing is missing.
     *
     * @param prescription the prescription to issue; it is changed but not saved
     * @param reissue      whether an already issued (not confirmed) prescription is recalculated, for
     *                     example because the full bloom date was corrected
     * @return true when the prescription was issued or recalculated
     */
    public boolean issueIfReady(FruitThinningPrescription prescription, boolean reissue) {
        var snapshot = prescription.snapshot();
        boolean pending = snapshot.status() == PrescriptionStatus.SAMPLING_IN_PROGRESS;
        boolean issued = snapshot.status() == PrescriptionStatus.PRESCRIBED;
        if (!(pending || (reissue && issued)) || snapshot.fullBloomOn() == null) {
            return false;
        }
        var profile = profileOf(snapshot.plotId());
        if (profile.isEmpty() || (pending && !blockers(prescription).isEmpty())) {
            return false;
        }
        var rules = profile.get();
        prescription.determineSustainableCropLoad(
                new CropLoadBalancingCalculatorService(rules.targetFruitsPerShoot()),
                snapshot.fullBloomOn().plusDays(rules.windowOpensDaysAfterBloom()),
                snapshot.fullBloomOn().plusDays(rules.windowClosesDaysAfterBloom()),
                rules.version(),
                rules.status()
        );
        return true;
    }
}
