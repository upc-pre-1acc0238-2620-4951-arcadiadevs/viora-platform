package com.arcadiadevs.viora.platform.orchard.domain.services;

import com.arcadiadevs.viora.platform.orchard.domain.exceptions.InvalidPlotGeometryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CadastralGeometryService Domain Service Unit Tests")
class CadastralGeometryServiceTest {

    private final CadastralGeometryService service = new CadastralGeometryService();
    private final String validGeoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}";

    @Test
    @DisplayName("Should successfully validate a valid GeoJSON polygon")
    void shouldValidateValidPolygon() {
        service.validate(validGeoJson);
    }

    @Test
    @DisplayName("Should throw exception when GeoJSON is null or blank")
    void shouldThrowWhenGeoJsonIsNullOrBlank() {
        assertThatThrownBy(() -> service.validate(null))
                .isInstanceOf(InvalidPlotGeometryException.class)
                .hasMessage("plot.geometry.empty");

        assertThatThrownBy(() -> service.validate("   "))
                .isInstanceOf(InvalidPlotGeometryException.class)
                .hasMessage("plot.geometry.empty");
    }

    @Test
    @DisplayName("Should throw exception when polygon has fewer than 4 vertices")
    void shouldThrowWhenVerticesLessThanFour() {
        String invalid = "{\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.25,-18.05]]]}";
        assertThatThrownBy(() -> service.validate(invalid))
                .isInstanceOf(InvalidPlotGeometryException.class)
                .hasMessage("plot.geometry.vertices_min");
    }

    @Test
    @DisplayName("Should throw exception when polygon ring is unclosed")
    void shouldThrowWhenRingUnclosed() {
        String unclosed = "{\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06]]]}";
        assertThatThrownBy(() -> service.validate(unclosed))
                .isInstanceOf(InvalidPlotGeometryException.class)
                .hasMessage("plot.geometry.ring_unclosed");
    }

    @Test
    @DisplayName("Should calculate net area in hectares accurately")
    void shouldCalculateNetAreaHa() {
        Double area = service.netAreaHa(validGeoJson);
        assertThat(area).isNotNull();
        assertThat(area).isGreaterThan(100.0);
    }

    @Test
    @DisplayName("Should compute complete PlotGeometry value object")
    void shouldComputeGeometryValueObject() {
        var geometry = service.computeGeometry(validGeoJson);
        assertThat(geometry).isNotNull();
        assertThat(geometry.geoJson()).isEqualTo(validGeoJson);
        assertThat(geometry.areaHa()).isGreaterThan(0.0);
    }
}
