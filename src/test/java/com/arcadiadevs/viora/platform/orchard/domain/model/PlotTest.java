package com.arcadiadevs.viora.platform.orchard.domain.model;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.events.PlotDelimited;
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
        var event = (PlotDelimited) plot.domainEvents().iterator().next();
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
}
