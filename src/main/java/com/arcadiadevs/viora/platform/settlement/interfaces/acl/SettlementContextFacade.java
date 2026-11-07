package com.arcadiadevs.viora.platform.settlement.interfaces.acl;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ACL facade exposing read-only Settlement data to other bounded contexts, without leaking its aggregates.
 */
public interface SettlementContextFacade {

    /**
     * The caliber the mill weighed for each settled campaign of a plot, oldest first. Only settlements that recorded a
     * caliber are listed; the caller checks that the plot belongs to the asking producer.
     *
     * @param plotId the plot identifier (UUID)
     * @return the settled calibers by campaign; empty when the plot has none or the identifier is malformed
     */
    List<SettledCaliber> findSettledCalibers(String plotId);

    /**
     * The caliber of one settled campaign.
     *
     * @param campaignYear        the campaign year
     * @param fruitsPerKg         the commercial caliber in fruits per kilogram, as weighed at the mill
     * @param commercialSizeGrade the commercial size grade of that caliber (for example {@code 171/190}); null when the
     *                            scale has no grade for it
     */
    record SettledCaliber(int campaignYear, double fruitsPerKg, String commercialSizeGrade) {
    }

    /**
     * The harvest settled for each campaign of a plot, oldest first: one per campaign, the latest settlement of a year
     * wins. The caller decides who may read it.
     *
     * @param plotId the plot identifier (UUID)
     * @return the settled harvests; empty when the plot has none
     */
    List<SettledHarvest> findSettledHarvests(String plotId);

    /**
     * The harvest settled for one campaign of a plot.
     *
     * @param campaignYear the campaign
     * @param greenKg      green olives, in kilograms
     * @param blackKg      black olives, in kilograms
     * @param totalKg      the whole harvest, in kilograms
     * @param settledAt    when it was settled
     */
    record SettledHarvest(int campaignYear, double greenKg, double blackKg, double totalKg, Instant settledAt) {
    }

    /**
     * Reads the provisional dossier of a plot's campaign in progress, as it stands now. The caller decides who may
     * read it; nothing is stored and nothing is hashed.
     *
     * @param plotId           the plot identifier (UUID)
     * @param holderName       the producer's name to print; null if unknown
     * @param organizationName the organization to print; null if unknown
     * @return the dossier, or empty if the plot does not exist or is archived
     */
    Optional<ProvisionalDossier> findProvisionalDossier(String plotId, String holderName, String organizationName);

    /**
     * Renders the provisional dossier of a plot as a PDF, as it stands now.
     *
     * @param plotId           the plot identifier (UUID)
     * @param holderName       the producer's name to print; null if unknown
     * @param organizationName the organization to print; null if unknown
     * @return the PDF, or empty if the plot does not exist or is archived
     */
    Optional<ProvisionalDossierDocument> renderProvisionalDossier(String plotId, String holderName,
                                                                  String organizationName);

    /**
     * What a provisional dossier shows.
     *
     * @param plotName                    the plot's name
     * @param variety                     its variety
     * @param areaHectares                its area
     * @param campaignYear                the campaign in progress
     * @param cutOffAt                    when the data was read
     * @param preThinningFruitsPerShoot   the estimated load
     * @param targetFruitsPerShoot        the sustainable load
     * @param overTargetPercentage        the excess over the target
     * @param prescribedRemovalPercentage the prescribed thinning
     * @param windowOpensOn               when the thinning window opens
     * @param windowClosesOn              when it closes
     * @param sampledTrees                trees sampled
     * @param lastSampledOn               the last sampling round
     * @param thinnedOn                   when the thinning was registered
     * @param linkedNodes                 sensor nodes linked
     * @param daysWithReadings            days with readings this campaign
     */
    record ProvisionalDossier(String plotName, String variety, Double areaHectares, int campaignYear,
                              Instant cutOffAt, Double preThinningFruitsPerShoot, Double targetFruitsPerShoot,
                              Double overTargetPercentage, Double prescribedRemovalPercentage,
                              LocalDate windowOpensOn, LocalDate windowClosesOn, int sampledTrees,
                              LocalDate lastSampledOn, LocalDate thinnedOn, int linkedNodes, int daysWithReadings) {
    }

    /**
     * A provisional dossier rendered as a PDF.
     *
     * @param content  the PDF bytes
     * @param cutOffAt when its data was read
     */
    record ProvisionalDossierDocument(byte[] content, Instant cutOffAt) {
    }
}
