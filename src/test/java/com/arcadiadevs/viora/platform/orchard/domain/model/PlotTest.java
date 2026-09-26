package com.arcadiadevs.viora.platform.orchard.domain.model;

import com.arcadiadevs.viora.platform.orchard.domain.exceptions.PlotAlreadyRemovedException;
import com.arcadiadevs.viora.platform.orchard.domain.exceptions.PlotRevisionMismatchException;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.events.PlotDelimitedEvent;
import com.arcadiadevs.viora.platform.orchard.domain.model.events.PlotRemovedEvent;
import com.arcadiadevs.viora.platform.orchard.domain.model.events.PlotUpdatedEvent;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Plot Aggregate Root and Value Objects Domain Unit Tests")
class PlotTest {

    private final ProducerId producerId = new ProducerId();
    private final String validGeoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}";

    @Test
    @DisplayName("Should successfully delimit plot with valid parameters and calculate density")
    void shouldSuccessfullyDelimitPlot() {
        var name = new PlotName("Cuartel San Jerónimo");
        var variety = OliveVariety.CRIOLLA;
        var polygon = new PlotGeometry(validGeoJson, 1.25);
        var frame = new PlantationFrame(7.0, 5.0);

        var plot = Plot.delimit(producerId, name, variety, polygon, frame);

        assertThat(plot).isNotNull();
        var snapshot = plot.snapshot();
        assertThat(snapshot.id()).isNotNull();
        assertThat(snapshot.id().plotId()).isNotBlank();
        assertThat(snapshot.producerId().producerId()).isEqualTo(producerId.producerId());
        assertThat(snapshot.name().value()).isEqualTo("Cuartel San Jerónimo");
        assertThat(snapshot.variety()).isEqualTo(OliveVariety.CRIOLLA);
        assertThat(snapshot.geometry().areaHa()).isEqualTo(1.25);
        assertThat(snapshot.density().treesPerHectare()).isEqualTo(286);
        assertThat(snapshot.status()).isEqualTo(PlotStatus.ACTIVE);
        assertThat(snapshot.revision()).isEqualTo(0L);

        assertThat(plot.domainEvents()).hasSize(1);
        var event = (PlotDelimitedEvent) plot.domainEvents().iterator().next();
        assertThat(event.plotId()).isEqualTo(snapshot.id().plotId());
        assertThat(event.producerId()).isEqualTo(producerId.producerId());
        assertThat(event.variety()).isEqualTo("CRIOLLA");
    }

    @Test
    @DisplayName("Should validate PlotId correctly for null, blank, and invalid UUID formats")
    void shouldValidatePlotIdCorrectly() {
        var generatedId = new PlotId();
        assertThat(generatedId.plotId()).isNotBlank();
        assertThat(UUID.fromString(generatedId.plotId())).isNotNull();

        assertThatThrownBy(() -> new PlotId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.id.null_or_empty");

        assertThatThrownBy(() -> new PlotId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.id.null_or_empty");

        assertThatThrownBy(() -> new PlotId("not-a-valid-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.id.invalid_uuid");
    }

    @Test
    @DisplayName("Should validate ProducerId correctly for null, blank, and invalid UUID formats")
    void shouldValidateProducerIdCorrectly() {
        var generatedId = new ProducerId();
        assertThat(generatedId.producerId()).isNotBlank();
        assertThat(UUID.fromString(generatedId.producerId())).isNotNull();

        assertThatThrownBy(() -> new ProducerId(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("producer.id.null_or_empty");

        assertThatThrownBy(() -> new ProducerId("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("producer.id.null_or_empty");

        assertThatThrownBy(() -> new ProducerId("not-a-valid-uuid"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("producer.id.invalid_uuid");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when plot name length is less than 3 characters")
    void shouldThrowExceptionWhenPlotNameIsTooShort() {
        assertThatThrownBy(() -> new PlotName("ab"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.name.invalid_length");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when plantation frame spacing is non-positive")
    void shouldThrowExceptionWhenPlantationFrameIsInvalid() {
        assertThatThrownBy(() -> new PlantationFrame(0.0, 5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.spacing.positive");

        assertThatThrownBy(() -> new PlantationFrame(7.0, -1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.spacing.positive");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when calculating TreeDensity from null PlantationFrame")
    void shouldThrowExceptionWhenPlantationFrameIsNullInTreeDensity() {
        assertThatThrownBy(() -> TreeDensity.from(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.frame.null");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when tree density is less than 50")
    void shouldThrowExceptionWhenTreeDensityIsLessThan50() {
        assertThatThrownBy(() -> new TreeDensity(49))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.density.min");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when polygon area is non-positive")
    void shouldThrowExceptionWhenPolygonAreaIsNonPositive() {
        assertThatThrownBy(() -> new PlotGeometry(validGeoJson, 0.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.area.positive");
    }

    @Test
    @DisplayName("Should reconstitute aggregate preserving exact state from snapshot")
    void shouldReconstituteAggregateFromSnapshot() {
        var name = new PlotName("Cuartel Santa Rosa");
        var variety = OliveVariety.ARBEQUINA;
        var polygon = new PlotGeometry(validGeoJson, 2.5);
        var frame = new PlantationFrame(6.0, 4.0);

        var original = Plot.delimit(producerId, name, variety, polygon, frame);
        var snapshot = original.snapshot();

        var reconstituted = Plot.reconstitute(snapshot);

        assertThat(reconstituted.snapshot()).isEqualTo(snapshot);
    }

    @Test
    @DisplayName("Should successfully update plot, increment revision, recalculate density, and emit PlotUpdated event")
    void shouldSuccessfullyUpdatePlot() {
        var plot = Plot.delimit(
                producerId,
                new PlotName("Cuartel Original"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );
        plot.clearDomainEvents();

        var updatedName = new PlotName("Cuartel Rectificado");
        var updatedPolygon = new PlotGeometry(validGeoJson, 1.50);
        var updatedFrame = new PlantationFrame(6.0, 4.0);
        var pruningDate = java.time.LocalDate.of(2026, 9, 15);

        plot.update(updatedName, updatedPolygon, updatedFrame, pruningDate, 0L);

        var snap = plot.snapshot();
        assertThat(snap.name().value()).isEqualTo("Cuartel Rectificado");
        assertThat(snap.geometry().areaHa()).isEqualTo(1.50);
        assertThat(snap.frame().rowSpacingM()).isEqualTo(6.0);
        assertThat(snap.frame().treeSpacingM()).isEqualTo(4.0);
        assertThat(snap.density().treesPerHectare()).isEqualTo(417);
        assertThat(snap.lastPruningDate()).isEqualTo(pruningDate);
        assertThat(snap.revision()).isEqualTo(1L);

        assertThat(plot.domainEvents()).hasSize(1);
        var event = (PlotUpdatedEvent) plot.domainEvents().iterator().next();
        assertThat(event.plotId()).isEqualTo(snap.id().plotId());
        assertThat(event.producerId()).isEqualTo(producerId.producerId());
        assertThat(event.name()).isEqualTo("Cuartel Rectificado");
        assertThat(event.revision()).isEqualTo(1L);
        assertThat(event.areaHa()).isEqualTo(1.50);
    }

    @Test
    @DisplayName("Should throw PlotRevisionMismatchException when expected revision does not match aggregate revision")
    void shouldThrowExceptionWhenRevisionMismatch() {
        var plot = Plot.delimit(
                producerId,
                new PlotName("Cuartel Concurrente"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );

        var updatedName = new PlotName("Cuartel Modificado");
        var updatedPolygon = new PlotGeometry(validGeoJson, 1.25);
        var updatedFrame = new PlantationFrame(7.0, 5.0);

        assertThatThrownBy(() -> plot.update(updatedName, updatedPolygon, updatedFrame, null, 5L))
                .isInstanceOf(PlotRevisionMismatchException.class);
    }

    @Test
    @DisplayName("Should successfully soft delete plot and register PlotRemovedEvent domain event")
    void shouldSuccessfullyRemovePlot() {
        var plot = Plot.delimit(
                producerId,
                new PlotName("Cuartel A Eliminar"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );
        plot.clearDomainEvents();

        plot.remove("Desafectado por salinizacion");

        var snap = plot.snapshot();
        assertThat(snap.status()).isEqualTo(PlotStatus.REMOVED_SOFT_DELETE);
        assertThat(snap.revision()).isEqualTo(1L);

        assertThat(plot.domainEvents()).hasSize(1);
        var event = (PlotRemovedEvent) plot.domainEvents().iterator().next();
        assertThat(event.plotId()).isEqualTo(snap.id().plotId());
        assertThat(event.producerId()).isEqualTo(producerId.producerId());
        assertThat(event.reason()).isEqualTo("Desafectado por salinizacion");
        assertThat(event.occurredOn()).isNotNull();
    }

    @Test
    @DisplayName("Should throw PlotAlreadyRemovedException when removing an already soft-deleted plot")
    void shouldThrowExceptionWhenAlreadyRemoved() {
        var plot = Plot.delimit(
                producerId,
                new PlotName("Cuartel Ya Eliminado"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );
        plot.remove("Primera remocion");

        assertThatThrownBy(() -> plot.remove("Segunda remocion"))
                .isInstanceOf(PlotAlreadyRemovedException.class);
    }
}
