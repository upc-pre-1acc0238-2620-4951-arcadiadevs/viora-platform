package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.SealedDossierSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.DossierDocument;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.SealedDossierId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.VerificationHash;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.SealedDossierRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.assemblers.SealedDossierPersistenceAssembler;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.SealedDossierDocumentPersistenceRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.SealedDossierPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JPA adapter of the sealed dossiers: metadata and bytes in two tables, both insert only. */
@Repository
public class SealedDossierRepositoryImpl implements SealedDossierRepository {

    private final SealedDossierPersistenceRepository dossiers;
    private final SealedDossierDocumentPersistenceRepository documents;

    public SealedDossierRepositoryImpl(SealedDossierPersistenceRepository dossiers,
            SealedDossierDocumentPersistenceRepository documents) {
        this.dossiers = dossiers;
        this.documents = documents;
    }

    @Override
    public void save(SealedDossierSnapshot dossier, DossierDocument document) {
        dossiers.saveAndFlush(SealedDossierPersistenceAssembler.toEntity(dossier));
        documents.saveAndFlush(SealedDossierPersistenceAssembler.toEntity(dossier.id(), document));
    }

    @Override
    public Optional<SealedDossierSnapshot> findByPlotIdAndCampaignYear(PlotId plotId, CampaignYear campaignYear) {
        return dossiers.findByPlotIdAndCampaignYear(plotId.plotId(), campaignYear.value())
                .map(SealedDossierPersistenceAssembler::toDomain);
    }

    @Override
    public List<SealedDossierSnapshot> findByPlotId(PlotId plotId) {
        return dossiers.findByPlotIdOrderByCampaignYearDesc(plotId.plotId()).stream()
                .map(SealedDossierPersistenceAssembler::toDomain)
                .toList();
    }

    @Override
    public Optional<SealedDossierSnapshot> findByVerificationHash(VerificationHash verificationHash) {
        return dossiers.findByVerificationHash(verificationHash.value()).map(SealedDossierPersistenceAssembler::toDomain);
    }

    @Override
    public Optional<DossierDocument> findDocument(SealedDossierId dossierId) {
        return documents.findById(UUID.fromString(dossierId.value())).map(entity -> new DossierDocument(entity.getContent()));
    }
}
