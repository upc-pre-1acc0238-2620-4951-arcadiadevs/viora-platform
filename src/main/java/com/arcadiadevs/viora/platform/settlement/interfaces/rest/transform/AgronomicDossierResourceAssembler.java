package com.arcadiadevs.viora.platform.settlement.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.DossierCertificationSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.SealedDossierSnapshot;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.AgronomicDossierResource;
import com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources.DossierVerificationResource;

import java.text.Normalizer;

/** Maps sealed dossiers to their REST resources; only display values are rounded. */
public final class AgronomicDossierResourceAssembler {

    private AgronomicDossierResourceAssembler() {
    }

    public static AgronomicDossierResource toResource(SealedDossierSnapshot dossier) {
        return toResource(dossier, null);
    }

    public static AgronomicDossierResource toResource(SealedDossierSnapshot dossier,
            DossierCertificationSnapshot certification) {
        var facts = dossier.facts();
        return new AgronomicDossierResource(
                dossier.id().value(),
                dossier.plotId().plotId(),
                dossier.producerId().userId(),
                facts.holderName(),
                dossier.campaignYear().value(),
                facts.plotName(),
                facts.variety(),
                facts.areaHectares(),
                new AgronomicDossierResource.FruitLoad(round(facts.preThinningFruitsPerShoot(), 3),
                        round(facts.targetFruitsPerShoot(), 3), round(facts.overTargetPercentage(), 1),
                        "FRUITS_PER_SHOOT"),
                new AgronomicDossierResource.Prescription(round(facts.prescribedRemovalPercentage(), 2),
                        facts.windowOpensOn(), facts.windowClosesOn()),
                new AgronomicDossierResource.Thinning(round(facts.actualRemovalPercentage(), 2), facts.thinnedOn(),
                        facts.thinningCompliance()),
                new AgronomicDossierResource.Harvest(facts.totalKg(), facts.greenKg(), facts.blackKg(),
                        round(facts.kgPerHectare(), 1), facts.receiptNumber(), facts.weighedOn()),
                new AgronomicDossierResource.Climate(facts.linkedNodes(), facts.daysWithReadings()),
                new AgronomicDossierResource.History(facts.historyFromYear(), facts.historyToYear(),
                        facts.harvestCampaigns()),
                dossier.verificationHash().value(),
                dossier.sealedAt(),
                dossier.pageCount(),
                dossier.sizeBytes(),
                fileName(dossier),
                certification == null ? null : new AgronomicDossierResource.Certification(
                        certification.certifier().name(), certification.certifier().cipNumber(),
                        certification.metadata().certifiedAt(), certification.metadata().verificationHash().value()));
    }

    public static DossierVerificationResource toVerification(SealedDossierSnapshot dossier) {
        var facts = dossier.facts();
        return new DossierVerificationResource(true, dossier.verificationHash().value(), dossier.plotId().plotId(),
                facts.plotName(), facts.areaHectares(), dossier.producerId().userId(), facts.holderName(),
                dossier.campaignYear().value(),
                dossier.sealedAt(), dossier.pageCount(), facts.historyFromYear(), facts.historyToYear());
    }

    /** "Expediente_LaYarada02_2026.pdf": the plot name without accents, spaces or symbols. */
    public static String fileName(SealedDossierSnapshot dossier) {
        String name = dossier.facts().plotName() == null ? "Lote" : dossier.facts().plotName();
        String plain = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "");
        return "Expediente_" + (plain.isEmpty() ? "Lote" : plain) + "_" + dossier.campaignYear().value() + ".pdf";
    }

    private static Double round(Double value, int decimals) {
        if (value == null) {
            return null;
        }
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
