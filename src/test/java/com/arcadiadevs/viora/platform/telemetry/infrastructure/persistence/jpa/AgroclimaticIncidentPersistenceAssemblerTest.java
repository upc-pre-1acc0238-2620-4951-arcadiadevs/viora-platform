package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.AgroclimaticIncident;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.assemblers.AgroclimaticIncidentPersistenceAssembler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AgroclimaticIncidentPersistenceAssembler Unit Tests")
class AgroclimaticIncidentPersistenceAssemblerTest {

    @Test
    @DisplayName("Should assemble JPA entity from domain aggregate and back to domain faithfully")
    void shouldAssembleToEntityAndBackToDomain() {
        var plotId = new PlotId(UUID.randomUUID().toString());
        var breach = new ThresholdBreachInfo("SOIL_MOISTURE_30CM", 11.0, 16.0, "%");
        var incident = AgroclimaticIncident.raise(
                plotId,
                IncidentType.HYDRIC_STRESS,
                IncidentSeverity.CRITICAL,
                breach,
                List.of("hydric_stress.step.check_drippers", "hydric_stress.step.schedule_emergency_irrigation"),
                Instant.now()
        );

        var entity = AgroclimaticIncidentPersistenceAssembler.toPersistenceFromDomain(incident);
        assertThat(entity.getId().toString()).isEqualTo(incident.snapshot().id().incidentId());
        assertThat(entity.getPlotId()).isEqualTo(plotId);
        assertThat(entity.getType()).isEqualTo(IncidentType.HYDRIC_STRESS);
        assertThat(entity.getSeverity()).isEqualTo(IncidentSeverity.CRITICAL);
        assertThat(entity.getStatus()).isEqualTo(IncidentStatus.ACTIVE);
        assertThat(entity.getBreachInfo().getMetricName()).isEqualTo("SOIL_MOISTURE_30CM");
        assertThat(entity.getBreachInfo().getCurrentValue()).isEqualTo(11.0);
        assertThat(entity.getMitigationSteps()).hasSize(2);

        var reconstituted = AgroclimaticIncidentPersistenceAssembler.toDomainFromPersistence(entity);
        assertThat(reconstituted).isNotNull();
        assertThat(reconstituted.snapshot()).isEqualTo(incident.snapshot());
    }
}
