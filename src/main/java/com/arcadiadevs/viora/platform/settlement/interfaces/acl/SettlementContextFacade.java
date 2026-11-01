package com.arcadiadevs.viora.platform.settlement.interfaces.acl;

import java.util.List;

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
}
