package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.settlement.interfaces.acl.SettlementContextFacade;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Anti-corruption layer asking Settlement, owner of the dossiers, for the provisional dossier of a member plot.
 */
@Service("territoryExternalSettlementService")
public class ExternalSettlementService {

    private final SettlementContextFacade settlementContextFacade;

    /**
     * Creates the service.
     *
     * @param settlementContextFacade the Settlement facade
     */
    public ExternalSettlementService(SettlementContextFacade settlementContextFacade) {
        this.settlementContextFacade = settlementContextFacade;
    }

    /**
     * Reads the provisional dossier of a plot now.
     *
     * @param plotId           the plot
     * @param holderName       the producer's name to print
     * @param organizationName the organization to print
     * @return the dossier, or empty if the plot is not active
     */
    public Optional<ProvisionalDossierFacts> findProvisionalDossier(PlotId plotId, String holderName,
                                                                    String organizationName) {
        return settlementContextFacade.findProvisionalDossier(plotId.plotId(), holderName, organizationName)
                .map(dossier -> new ProvisionalDossierFacts(dossier.plotName(), dossier.variety(),
                        dossier.areaHectares(), dossier.campaignYear(), dossier.cutOffAt(),
                        dossier.preThinningFruitsPerShoot(), dossier.targetFruitsPerShoot(),
                        dossier.overTargetPercentage(), dossier.prescribedRemovalPercentage(),
                        dossier.windowOpensOn(), dossier.windowClosesOn(), dossier.sampledTrees(),
                        dossier.lastSampledOn(), dossier.thinnedOn(), dossier.linkedNodes(),
                        dossier.daysWithReadings()));
    }

    /**
     * Renders the provisional dossier of a plot now as a PDF.
     *
     * @param plotId           the plot
     * @param holderName       the producer's name to print
     * @param organizationName the organization to print
     * @return the PDF, or empty if the plot is not active
     */
    public Optional<RenderedDossier> renderProvisionalDossier(PlotId plotId, String holderName,
                                                              String organizationName) {
        return settlementContextFacade.renderProvisionalDossier(plotId.plotId(), holderName, organizationName)
                .map(document -> new RenderedDossier(document.content(), document.cutOffAt()));
    }

    /**
     * A provisional dossier rendered as a PDF.
     *
     * @param content  the PDF bytes
     * @param cutOffAt when its data was read
     */
    public record RenderedDossier(byte[] content, Instant cutOffAt) {
    }

    /**
     * Lists the harvest settled for each campaign of a plot.
     *
     * @param plotId the plot
     * @return the settled harvests, oldest first
     */
    public List<SettledHarvestFacts> findSettledHarvests(PlotId plotId) {
        return settlementContextFacade.findSettledHarvests(plotId.plotId()).stream()
                .map(harvest -> new SettledHarvestFacts(harvest.campaignYear(), harvest.greenKg(), harvest.blackKg(),
                        harvest.settledAt()))
                .toList();
    }

    /**
     * The harvest settled for one campaign of a plot.
     *
     * @param campaignYear the campaign
     * @param greenKg      green olives, in kilograms
     * @param blackKg      black olives, in kilograms
     * @param settledAt    when it was settled
     */
    public record SettledHarvestFacts(int campaignYear, double greenKg, double blackKg, Instant settledAt) {
    }
}
