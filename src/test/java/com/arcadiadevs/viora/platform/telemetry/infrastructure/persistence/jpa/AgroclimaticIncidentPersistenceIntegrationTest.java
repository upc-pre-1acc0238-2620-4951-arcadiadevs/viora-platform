package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.adapters.AgroclimaticIncidentRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("AgroclimaticIncident JPA Persistence and Adapter Integration Tests")
class AgroclimaticIncidentPersistenceIntegrationTest {

    @Autowired
    private AgroclimaticIncidentRepositoryImpl incidentRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private final PlotId plotId = new PlotId(UUID.randomUUID().toString());

    @Test
    @DisplayName("Should persist and reconstitute AgroclimaticIncident with steps through JPA adapter")
    void shouldPersistAndReconstituteIncident() {
        var breach = new ThresholdBreachInfo("AMBIENT_TEMPERATURE", 37.0, 36.0, "°C");
        var incident = AgroclimaticIncident.raise(
                plotId,
                IncidentType.HEAT_WAVE,
                IncidentSeverity.CRITICAL,
                breach,
                List.of("heat_wave.step.irrigate_early_morning"),
                Instant.now()
        );

        var saved = incidentRepository.save(incident);
        entityManager.flush();
        entityManager.clear();

        var reloadedOpt = incidentRepository.findById(saved.snapshot().id());
        assertThat(reloadedOpt).isPresent();
        var reloaded = reloadedOpt.get();

        assertThat(reloaded.snapshot().type()).isEqualTo(IncidentType.HEAT_WAVE);
        assertThat(reloaded.snapshot().severity()).isEqualTo(IncidentSeverity.CRITICAL);
        assertThat(reloaded.snapshot().status()).isEqualTo(IncidentStatus.ACTIVE);
        assertThat(reloaded.snapshot().mitigationSteps()).hasSize(1);

        // Verify query methods
        assertThat(incidentRepository.existsActiveByPlotIdAndType(plotId, IncidentType.HEAT_WAVE)).isTrue();
        assertThat(incidentRepository.findByPlotId(plotId)).hasSize(1);
        assertThat(incidentRepository.findByPlotIdAndStatus(plotId, IncidentStatus.ACTIVE)).hasSize(1);
    }
}
