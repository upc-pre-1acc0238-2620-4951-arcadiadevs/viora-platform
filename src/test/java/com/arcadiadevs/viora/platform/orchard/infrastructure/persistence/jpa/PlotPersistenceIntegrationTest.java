package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.OliveVariety;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlantationFrame;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotGeometry;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotName;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.adapters.PlotRepositoryImpl;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
@DisplayName("Plot JPA Persistence and Adapter Integration Tests")
class PlotPersistenceIntegrationTest {

    private static final String GEO_JSON =
            "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}";

    @Autowired
    private PlotRepositoryImpl plotRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Plot newPlot() {
        return Plot.delimit(
                new ProducerId(UUID.randomUUID().toString()),
                new PlotName("Cuartel Persistido"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(GEO_JSON, 1.25),
                new PlantationFrame(7.0, 5.0)
        );
    }

    @Test
    @DisplayName("Should return the stored revision after each save, so an If-Match built from it matches")
    void shouldReturnTheStoredRevisionAfterEachSave() {
        var created = plotRepository.save(newPlot());
        entityManager.clear();
        assertThat(created.snapshot().revision()).isEqualTo(0L);

        var plot = plotRepository.findById(created.snapshot().id()).orElseThrow();
        plot.remove("No longer farmed");
        var archived = plotRepository.save(plot);
        assertThat(archived.snapshot().revision()).isEqualTo(1L);
        entityManager.clear();

        var archivedAgain = plotRepository.findById(created.snapshot().id()).orElseThrow();
        archivedAgain.restore();
        var restored = plotRepository.save(archivedAgain);
        assertThat(restored.snapshot().revision()).isEqualTo(2L);
        entityManager.clear();

        var stored = plotRepository.findById(created.snapshot().id()).orElseThrow();
        assertThat(stored.snapshot().revision()).isEqualTo(restored.snapshot().revision());
    }
}
