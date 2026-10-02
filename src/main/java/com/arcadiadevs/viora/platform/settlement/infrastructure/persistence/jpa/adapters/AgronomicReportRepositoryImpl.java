package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ReportId;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.assemblers.AgronomicReportPersistenceAssembler;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.AgronomicReportPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Adapter between the agronomic report port and Spring Data JPA; settlements and certifications are append-only. */
@Repository
public class AgronomicReportRepositoryImpl implements AgronomicReportRepository {

    private final AgronomicReportPersistenceRepository persistenceRepository;

    public AgronomicReportRepositoryImpl(AgronomicReportPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Optional<AgronomicReport> findById(ReportId id) {
        return persistenceRepository.findById(UUID.fromString(id.reportId()))
                .map(AgronomicReportPersistenceAssembler::toDomain);
    }

    @Override
    public Optional<AgronomicReport> findByPlotId(PlotId plotId) {
        return persistenceRepository.findByPlotId(UUID.fromString(plotId.plotId()))
                .map(AgronomicReportPersistenceAssembler::toDomain);
    }

    @Override
    public Optional<AgronomicReport> findByPlotIdForUpdate(PlotId plotId) {
        return persistenceRepository.findByPlotIdForUpdate(UUID.fromString(plotId.plotId()))
                .map(AgronomicReportPersistenceAssembler::toDomain);
    }

    @Override
    public AgronomicReport save(AgronomicReport report) {
        var snapshot = report.snapshot();
        var entity = persistenceRepository.findById(UUID.fromString(snapshot.id().reportId()))
                .map(existing -> {
                    AgronomicReportPersistenceAssembler.appendNewSettlements(existing, snapshot);
                    AgronomicReportPersistenceAssembler.appendNewCertifications(existing, snapshot);
                    return existing;
                })
                .orElseGet(() -> AgronomicReportPersistenceAssembler.toNewEntity(report));
        return AgronomicReportPersistenceAssembler.toDomain(persistenceRepository.saveAndFlush(entity));
    }
}
