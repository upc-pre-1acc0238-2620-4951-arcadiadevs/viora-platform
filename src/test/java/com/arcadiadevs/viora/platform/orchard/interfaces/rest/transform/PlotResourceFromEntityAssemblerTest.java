package com.arcadiadevs.viora.platform.orchard.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PlotResourceFromEntityAssembler Unit Tests")
class PlotResourceFromEntityAssemblerTest {

    private final String producerId = "550e8400-e29b-41d4-a716-446655440000";
    private final String validGeoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}";

    @Test
    @DisplayName("Should transform Plot entity into PlotResource with String IDs and correct attributes")
    void shouldTransformPlotEntityToResource() {
        Plot plot = Plot.delimit(
                new ProducerId(producerId),
                new PlotName("Cuartel San Jerónimo"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );

        var resource = PlotResourceFromEntityAssembler.toResourceFromEntity(plot);

        assertThat(resource).isNotNull();
        assertThat(resource.id()).isEqualTo(plot.snapshot().id().plotId());
        assertThat(resource.producerId()).isEqualTo(producerId);
        assertThat(resource.name()).isEqualTo("Cuartel San Jerónimo");
        assertThat(resource.variety()).isEqualTo("CRIOLLA");
        assertThat(resource.areaHa()).isEqualTo(1.25);
        assertThat(resource.treeDensity()).isEqualTo(286);
        assertThat(resource.rowSpacingM()).isEqualTo(7.0);
        assertThat(resource.treeSpacingM()).isEqualTo(5.0);
        assertThat(resource.polygonGeoJson()).isEqualTo(validGeoJson);
        assertThat(resource.lastPruningDate()).isNull();
        assertThat(resource.status()).isEqualTo("ACTIVE");
        assertThat(resource.revision()).isZero();
    }

    @Test
    @DisplayName("Should transform List of Plot entities into List of PlotResources")
    void shouldTransformPlotListToResourceList() {
        Plot plot = Plot.delimit(
                new ProducerId(producerId),
                new PlotName("Cuartel San Jerónimo"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );

        var resourceList = PlotResourceFromEntityAssembler.toResourceList(java.util.List.of(plot));

        assertThat(resourceList).hasSize(1);
        assertThat(resourceList.getFirst().id()).isEqualTo(plot.snapshot().id().plotId());
        assertThat(PlotResourceFromEntityAssembler.toResourceList(java.util.List.of())).isEmpty();
    }
}
