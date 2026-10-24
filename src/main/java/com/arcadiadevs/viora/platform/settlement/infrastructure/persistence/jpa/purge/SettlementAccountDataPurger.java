package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.purge;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.orchard.domain.model.events.ProducerPlotsPurgedEvent;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.DossierCertificationPersistenceEntity;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.SealedDossierPersistenceEntity;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.AgronomicReportPersistenceRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.DossierDocumentPersistenceRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.ReceiptCounterPersistenceRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.SealedDossierDocumentPersistenceRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.SealedDossierPersistenceRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.SettledHarvestPersistenceRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.ThinningExecutionRecordPersistenceRepository;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Removes what the settlement context keeps for a deleted account: its agronomic reports with their settlements,
 * certifications and documents, its receipt counters, its sealed dossiers with their PDFs, and the thinning
 * executions copied from its plots.
 */
@Component
public class SettlementAccountDataPurger {

    private final AgronomicReportPersistenceRepository reports;
    private final DossierDocumentPersistenceRepository documents;
    private final SettledHarvestPersistenceRepository settlements;
    private final ReceiptCounterPersistenceRepository counters;
    private final SealedDossierPersistenceRepository dossiers;
    private final SealedDossierDocumentPersistenceRepository dossierDocuments;
    private final ThinningExecutionRecordPersistenceRepository executions;

    /**
     * Creates the purger.
     *
     * @param reports          the agronomic reports table
     * @param documents        the certification documents table
     * @param settlements      the harvest settlements table
     * @param counters         the receipt counters table
     * @param dossiers         the sealed dossiers table
     * @param dossierDocuments the sealed dossier PDFs table
     * @param executions       the copied thinning executions table
     */
    public SettlementAccountDataPurger(AgronomicReportPersistenceRepository reports,
                                       DossierDocumentPersistenceRepository documents,
                                       SettledHarvestPersistenceRepository settlements,
                                       ReceiptCounterPersistenceRepository counters,
                                       SealedDossierPersistenceRepository dossiers,
                                       SealedDossierDocumentPersistenceRepository dossierDocuments,
                                       ThinningExecutionRecordPersistenceRepository executions) {
        this.reports = reports;
        this.documents = documents;
        this.settlements = settlements;
        this.counters = counters;
        this.dossiers = dossiers;
        this.dossierDocuments = dossierDocuments;
        this.executions = executions;
    }

    /**
     * Removes the producer's reports, settlements, counters and sealed dossiers.
     *
     * @param event the deleted account
     */
    @EventListener
    public void on(UserAccountDeletedEvent event) {
        var producerId = UUID.fromString(event.userId());
        var ownedReports = reports.findAllByProducerId(producerId);
        var certificationIds = ownedReports.stream()
                .flatMap(report -> report.getCertifications().stream())
                .map(DossierCertificationPersistenceEntity::getId)
                .toList();
        if (!certificationIds.isEmpty()) {
            documents.deleteAll(documents.findAllByCertificationIdIn(certificationIds));
        }
        // The reports cascade to their settlements and certifications; settlements left without one go too.
        reports.deleteAll(ownedReports);
        settlements.deleteAll(settlements.findAllByProducerId(producerId));
        counters.deleteAll(counters.findAllByProducerId(producerId));

        var ownedDossiers = dossiers.findAllByProducerId(event.userId());
        var dossierIds = ownedDossiers.stream().map(SealedDossierPersistenceEntity::getId).toList();
        if (!dossierIds.isEmpty()) {
            dossierDocuments.deleteAll(dossierDocuments.findAllByDossierIdIn(dossierIds));
        }
        dossiers.deleteAll(ownedDossiers);
    }

    /**
     * Removes the thinning executions copied from the plots.
     *
     * @param event the plots of the deleted account
     */
    @EventListener
    public void on(ProducerPlotsPurgedEvent event) {
        if (event.plotIds().isEmpty()) {
            return;
        }
        executions.deleteAll(executions.findAllByPlotIdIn(event.plotIds().stream().map(UUID::fromString).toList()));
    }
}
