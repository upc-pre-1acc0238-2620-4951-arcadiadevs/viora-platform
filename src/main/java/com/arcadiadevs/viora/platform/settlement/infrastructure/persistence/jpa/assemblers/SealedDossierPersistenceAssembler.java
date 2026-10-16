package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.SealedDossierSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierDocument;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierFacts;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.SealedDossierId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.VerificationHash;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.SealedDossierDocumentPersistenceEntity;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.SealedDossierPersistenceEntity;

import java.util.UUID;

/** Maps sealed dossiers between the domain and their two tables. */
public final class SealedDossierPersistenceAssembler {

    private SealedDossierPersistenceAssembler() {
    }

    public static SealedDossierPersistenceEntity toEntity(SealedDossierSnapshot dossier) {
        var facts = dossier.facts();
        var entity = new SealedDossierPersistenceEntity();
        entity.setId(UUID.fromString(dossier.id().value()));
        entity.setPlotId(dossier.plotId().plotId());
        entity.setProducerId(dossier.producerId().userId());
        entity.setCampaignYear(dossier.campaignYear().value());
        entity.setVerificationHash(dossier.verificationHash().value());
        entity.setSealedAt(dossier.sealedAt());
        entity.setPageCount(dossier.pageCount());
        entity.setSizeBytes(dossier.sizeBytes());
        entity.setPlotName(facts.plotName());
        entity.setVariety(facts.variety());
        entity.setAreaHectares(facts.areaHectares());
        entity.setPreThinningFruitsPerShoot(facts.preThinningFruitsPerShoot());
        entity.setTargetFruitsPerShoot(facts.targetFruitsPerShoot());
        entity.setPrescribedRemovalPercentage(facts.prescribedRemovalPercentage());
        entity.setWindowOpensOn(facts.windowOpensOn());
        entity.setWindowClosesOn(facts.windowClosesOn());
        entity.setActualRemovalPercentage(facts.actualRemovalPercentage());
        entity.setThinnedOn(facts.thinnedOn());
        entity.setThinningCompliance(facts.thinningCompliance());
        entity.setTotalKg(facts.totalKg());
        entity.setGreenKg(facts.greenKg());
        entity.setBlackKg(facts.blackKg());
        entity.setReceiptNumber(facts.receiptNumber());
        entity.setWeighedOn(facts.weighedOn());
        entity.setLinkedNodes(facts.linkedNodes());
        entity.setDaysWithReadings(facts.daysWithReadings());
        entity.setHistoryFromYear(facts.historyFromYear());
        entity.setHistoryToYear(facts.historyToYear());
        entity.setHarvestCampaigns(facts.harvestCampaigns());
        entity.setHolderName(facts.holderName());
        return entity;
    }

    public static SealedDossierSnapshot toDomain(SealedDossierPersistenceEntity entity) {
        var facts = new DossierFacts(entity.getPlotName(), entity.getVariety(), entity.getAreaHectares(),
                entity.getPreThinningFruitsPerShoot(), entity.getTargetFruitsPerShoot(),
                entity.getPrescribedRemovalPercentage(), entity.getWindowOpensOn(), entity.getWindowClosesOn(),
                entity.getActualRemovalPercentage(), entity.getThinnedOn(), entity.getThinningCompliance(),
                entity.getTotalKg(), entity.getGreenKg(), entity.getBlackKg(), entity.getReceiptNumber(),
                entity.getWeighedOn(), entity.getLinkedNodes(), entity.getDaysWithReadings(),
                entity.getHistoryFromYear(), entity.getHistoryToYear(), entity.getHarvestCampaigns(),
                entity.getHolderName());
        return new SealedDossierSnapshot(new SealedDossierId(entity.getId().toString()), new PlotId(entity.getPlotId()),
                new UserId(entity.getProducerId()), new CampaignYear(entity.getCampaignYear()), facts,
                new VerificationHash(entity.getVerificationHash()), entity.getSealedAt(), entity.getPageCount(),
                entity.getSizeBytes());
    }

    public static SealedDossierDocumentPersistenceEntity toEntity(SealedDossierId id, DossierDocument document) {
        var entity = new SealedDossierDocumentPersistenceEntity();
        entity.setDossierId(UUID.fromString(id.value()));
        entity.setContent(document.content());
        return entity;
    }
}
