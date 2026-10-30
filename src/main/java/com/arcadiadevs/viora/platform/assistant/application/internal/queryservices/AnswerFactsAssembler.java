package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.CampaignThinningPlan;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Year;
import java.util.Collections;
import java.util.Optional;

/**
 * Reads, through the ACL of the other contexts, the figures a screen's answers are written from. It reads only what
 * those contexts already computed and only what the screen needs, so asking a question never recalculates anything.
 * A plot that no longer exists simply has no figures.
 */
@Service
public class AnswerFactsAssembler {

    private final ExternalOrchardService externalOrchardService;
    private final ExternalPhenologyService externalPhenologyService;
    private final ExternalThinningService externalThinningService;
    private final Clock clock;

    /**
     * Constructs the assembler.
     *
     * @param externalOrchardService   reads the area of the plot
     * @param externalPhenologyService reads the harvest history and the bearing index
     * @param externalThinningService  reads the prescription of the campaign
     * @param clock                    the application clock, for the current campaign
     */
    public AnswerFactsAssembler(ExternalOrchardService externalOrchardService,
                                ExternalPhenologyService externalPhenologyService,
                                ExternalThinningService externalThinningService, Clock clock) {
        this.externalOrchardService = externalOrchardService;
        this.externalPhenologyService = externalPhenologyService;
        this.externalThinningService = externalThinningService;
        this.clock = clock;
    }

    /**
     * Reads the figures the questions of a screen need.
     *
     * @param screen the screen
     * @param plotId the plot
     * @return the figures; those the screen does not use are left empty
     */
    public AnswerFacts assemble(ScreenKey screen, PlotId plotId) {
        int campaignYear = Year.now(clock.withZone(ProducerTimeZone.ZONE)).getValue();
        return switch (screen) {
            case ALTERNATION -> alternation(plotId, campaignYear);
            case PLAN -> plan(plotId, campaignYear);
        };
    }

    private AnswerFacts alternation(PlotId plotId, int campaignYear) {
        var bearing = externalPhenologyService.findBearing(plotId);
        return new AnswerFacts(
                campaignYear,
                externalOrchardService.findAreaHectares(plotId).orElse(null),
                externalPhenologyService.findHistoricalYields(plotId),
                bearing.map(b -> b.value()).orElse(null),
                bearing.map(b -> b.category()).orElse(null),
                null, null, null, null, null, 0);
    }

    private AnswerFacts plan(PlotId plotId, int campaignYear) {
        Optional<CampaignThinningPlan> plan = externalThinningService.findCampaignThinningPlan(plotId, campaignYear);
        Optional<PrescriptionAvailability> availability =
                externalThinningService.findPrescriptionAvailability(plotId, campaignYear);
        return new AnswerFacts(
                campaignYear,
                null,
                Collections.emptySortedMap(),
                null,
                null,
                plan.map(CampaignThinningPlan::preThinningFruitsPerShoot).orElse(null),
                plan.map(CampaignThinningPlan::targetFruitsPerShoot).orElse(null),
                plan.map(CampaignThinningPlan::percentageToRemove).orElse(null),
                plan.map(CampaignThinningPlan::windowOpensOn).orElse(null),
                plan.map(CampaignThinningPlan::windowClosesOn).orElse(null),
                availability.map(PrescriptionAvailability::sampledTrees).orElse(0));
    }
}
