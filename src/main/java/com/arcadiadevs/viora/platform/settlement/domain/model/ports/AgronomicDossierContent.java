package com.arcadiadevs.viora.platform.settlement.domain.model.ports;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;

import java.time.Instant;

/**
 * Frozen data of one certified campaign, handed to the PDF renderer. Every field comes from the same cut: the
 * settlement already frozen in the report plus the certification declaration and its single instant.
 *
 * @param reportId    agronomic report of the plot
 * @param plotId      certified plot
 * @param producerId  producer who owns the plot
 * @param campaignYear certified campaign
 * @param settlement  frozen settlement of the campaign: weights, caliber, status, thinning balance and curve
 * @param certifier   declared certifying professional
 * @param signature   declared collegiate signature
 * @param notes       optional certification notes
 * @param certifiedAt instant of the certification
 */
public record AgronomicDossierContent(
        ReportId reportId,
        PlotId plotId,
        UserId producerId,
        CampaignYear campaignYear,
        HarvestSettlementSnapshot settlement,
        CertifierIdentity certifier,
        AuditorSignature signature,
        String notes,
        Instant certifiedAt
) {

    public AgronomicDossierContent {
        if (reportId == null || plotId == null || producerId == null || campaignYear == null || settlement == null
                || certifier == null || signature == null || certifiedAt == null) {
            throw new IllegalArgumentException("settlement.certification.content.invalid");
        }
    }
}
