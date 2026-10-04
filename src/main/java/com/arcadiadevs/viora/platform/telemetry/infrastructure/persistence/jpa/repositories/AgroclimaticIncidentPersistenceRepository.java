package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentSeverity;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentStatus;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.IncidentType;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities.AgroclimaticIncidentPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link AgroclimaticIncidentPersistenceEntity}.
 */
public interface AgroclimaticIncidentPersistenceRepository extends JpaRepository<AgroclimaticIncidentPersistenceEntity, UUID> {

    List<AgroclimaticIncidentPersistenceEntity> findByPlotIdOrderByTriggeredAtDesc(PlotId plotId);

    List<AgroclimaticIncidentPersistenceEntity> findByPlotIdAndStatusOrderByTriggeredAtDesc(PlotId plotId, IncidentStatus status);

    @Query("SELECT i FROM AgroclimaticIncidentPersistenceEntity i WHERE " +
           "(:plotId IS NULL OR i.plotId = :plotId) AND " +
           "(:status IS NULL OR i.status = :status) AND " +
           "(:severity IS NULL OR i.severity = :severity) " +
           "ORDER BY i.triggeredAt DESC")
    List<AgroclimaticIncidentPersistenceEntity> findByCriteria(
            @Param("plotId") PlotId plotId,
            @Param("status") IncidentStatus status,
            @Param("severity") IncidentSeverity severity
    );

    Optional<AgroclimaticIncidentPersistenceEntity> findFirstByPlotIdAndTypeAndStatus(
            PlotId plotId,
            IncidentType type,
            IncidentStatus status
    );
}
