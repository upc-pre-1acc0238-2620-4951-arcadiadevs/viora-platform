package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.AgronomicReportPersistenceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** Spring Data repository for agronomic reports. */
@Repository
public interface AgronomicReportPersistenceRepository extends JpaRepository<AgronomicReportPersistenceEntity, UUID> {

    Optional<AgronomicReportPersistenceEntity> findByPlotId(UUID plotId);

    /** Locks the report row so two settlements of the same plot are serialized. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AgronomicReportPersistenceEntity r where r.plotId = :plotId")
    Optional<AgronomicReportPersistenceEntity> findByPlotIdForUpdate(@Param("plotId") UUID plotId);
}
