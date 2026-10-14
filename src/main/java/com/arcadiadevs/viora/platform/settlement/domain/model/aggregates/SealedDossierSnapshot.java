package com.arcadiadevs.viora.platform.settlement.domain.model.aggregates;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierFacts;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.SealedDossierId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.VerificationHash;

import java.time.Instant;

/**
 * Immutable state of a dossier sealed by the producer. The PDF bytes are kept apart, so reading the metadata never
 * loads them.
 *
 * @param id               dossier identity
 * @param plotId           plot of the campaign
 * @param producerId       owner of the plot
 * @param campaignYear     sealed campaign
 * @param facts            values the dossier states
 * @param verificationHash SHA-256 of the stored PDF bytes
 * @param sealedAt         instant of the seal
 * @param pageCount        pages of the PDF
 * @param sizeBytes        size of the PDF
 */
public record SealedDossierSnapshot(
        SealedDossierId id,
        PlotId plotId,
        UserId producerId,
        CampaignYear campaignYear,
        DossierFacts facts,
        VerificationHash verificationHash,
        Instant sealedAt,
        int pageCount,
        int sizeBytes
) {

    public SealedDossierSnapshot {
        if (id == null || plotId == null || producerId == null || campaignYear == null || facts == null
                || verificationHash == null || sealedAt == null || pageCount < 1 || sizeBytes < 1) {
            throw new IllegalArgumentException("settlement.dossier.content.invalid");
        }
    }
}
