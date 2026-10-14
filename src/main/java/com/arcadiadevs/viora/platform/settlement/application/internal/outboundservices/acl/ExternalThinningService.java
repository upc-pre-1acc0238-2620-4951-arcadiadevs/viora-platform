package com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

/** Outbound ACL used by Settlement to name the settled caliber and to read the thinning plan of a campaign. */
@Service("settlementExternalThinningService")
public class ExternalThinningService {

    private final ThinningContextFacade thinningContextFacade;

    /**
     * Constructs the outbound service injecting the Thinning ACL facade.
     *
     * @param thinningContextFacade the Thinning context facade
     */
    public ExternalThinningService(ThinningContextFacade thinningContextFacade) {
        this.thinningContextFacade = thinningContextFacade;
    }

    /**
     * Resolves the commercial size grade of a caliber.
     *
     * @param fruitsPerKg number of fruits per kilogram, or null when the caliber is unknown
     * @return the grade label, or empty when there is no caliber to grade
     */
    public Optional<String> findCommercialSizeGrade(Double fruitsPerKg) {
        if (fruitsPerKg == null) {
            return Optional.empty();
        }
        return thinningContextFacade.findCommercialSizeGrade(fruitsPerKg);
    }

    /**
     * Reads the crop load and the prescription of a campaign for the agronomic dossier.
     *
     * @param plotId       plot of the campaign
     * @param campaignYear the campaign
     * @return the plan, or empty when the plot was never sampled that campaign
     */
    public Optional<CampaignThinningPlan> findCampaignThinningPlan(PlotId plotId, CampaignYear campaignYear) {
        if (plotId == null || campaignYear == null) {
            return Optional.empty();
        }
        return thinningContextFacade.findCampaignThinningPlan(plotId.plotId(), campaignYear.value())
                .map(plan -> new CampaignThinningPlan(plan.preThinningFruitsPerShoot(), plan.targetFruitsPerShoot(),
                        plan.percentageToRemove(), plan.windowOpensOn(), plan.windowClosesOn()));
    }

    /**
     * Crop load in fruits per shoot and prescription of a campaign; each field is null when unknown.
     *
     * @param preThinningFruitsPerShoot mean load of the representative sampling
     * @param targetFruitsPerShoot      sustainable target
     * @param percentageToRemove        prescribed removal
     * @param windowOpensOn             first recommended thinning date
     * @param windowClosesOn            latest recommended thinning date
     */
    public record CampaignThinningPlan(Double preThinningFruitsPerShoot, Double targetFruitsPerShoot,
            Double percentageToRemove, LocalDate windowOpensOn, LocalDate windowClosesOn) {
    }
}
