package com.arcadiadevs.viora.platform.settlement.domain.model.aggregates;

import com.arcadiadevs.viora.platform.settlement.domain.exceptions.DossierRenderingException;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.SealedDossierContent;
import com.arcadiadevs.viora.platform.settlement.domain.model.ports.SealedDossierPdfGenerator;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierDocument;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.SealedDossierId;
import com.arcadiadevs.viora.platform.settlement.domain.services.CryptographicHashService;

/**
 * Agronomic dossier of one settled campaign, sealed by the producer (US30).
 *
 * <p>Unlike the certification of ADR-002 it carries no collegiate signature and needs no stabilization history: it
 * can be sealed from the first settled campaign, and the PDF says what is not determinable yet. One dossier per plot
 * and campaign; the settlement it reads is immutable, so the stored bytes and their SHA-256 never change.</p>
 */
public final class SealedDossier {

    private final SealedDossierSnapshot snapshot;

    private SealedDossier(SealedDossierSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("settlement.dossier.content.invalid");
        }
        this.snapshot = snapshot;
    }

    /**
     * Rebuilds a stored dossier.
     *
     * @param snapshot stored state
     * @return the dossier
     */
    public static SealedDossier reconstitute(SealedDossierSnapshot snapshot) {
        return new SealedDossier(snapshot);
    }

    /**
     * Renders the PDF, computes its SHA-256 over the final bytes and seals the dossier.
     *
     * @param content     frozen data of the campaign
     * @param generator   PDF renderer
     * @param hashService SHA-256 service
     * @return the sealed dossier and the exact document to store
     * @throws DossierRenderingException when the PDF cannot be rendered or is not a valid PDF
     */
    public static Sealed seal(SealedDossierContent content, SealedDossierPdfGenerator generator,
            CryptographicHashService hashService) {
        if (content == null || generator == null || hashService == null) {
            throw new IllegalArgumentException("settlement.dossier.content.invalid");
        }
        var rendered = generator.render(content);
        final DossierDocument document;
        try {
            document = new DossierDocument(rendered == null ? null : rendered.content());
        } catch (IllegalArgumentException exception) {
            throw new DossierRenderingException("settlement.dossier.render.failed", exception);
        }
        var snapshot = new SealedDossierSnapshot(SealedDossierId.generate(), content.plotId(), content.producerId(),
                content.campaignYear(), content.facts(), hashService.sha256(document.content()), content.sealedAt(),
                Math.max(1, rendered.pageCount()), document.size());
        return new Sealed(new SealedDossier(snapshot), document);
    }

    public SealedDossierSnapshot snapshot() {
        return snapshot;
    }

    /**
     * A freshly sealed dossier and the bytes to store with it.
     *
     * @param dossier  the dossier
     * @param document the exact PDF
     */
    public record Sealed(SealedDossier dossier, DossierDocument document) {
    }
}
