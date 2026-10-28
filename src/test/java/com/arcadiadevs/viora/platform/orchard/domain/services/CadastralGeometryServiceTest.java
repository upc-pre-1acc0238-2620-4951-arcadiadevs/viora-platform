package com.arcadiadevs.viora.platform.orchard.domain.services;

import com.arcadiadevs.viora.platform.orchard.domain.exceptions.InvalidPlotGeometryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        String invalid = "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.25,-18.05]]]}";
        assertThatThrownBy(() -> service.validate(invalid))
                .isInstanceOf(InvalidPlotGeometryException.class)
                .hasMessage("plot.geometry.vertices_min");
    }

    @Test
    @DisplayName("Should throw exception when polygon ring is unclosed")
    void shouldThrowWhenRingUnclosed() {
        String unclosed = "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06]]]}";
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

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
            "<img src=x onerror=alert(1)> coordinates [1,1] [2,2] [3,3] [1,1] | plot.geometry.invalid",
            "{\"type\":\"LineString\",\"coordinates\":[[-70.25,-18.05],[-70.24,-18.05]]} | plot.geometry.invalid",
            "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.05]],[[0,0],[0,1],[1,1],[0,0]]]} | plot.geometry.invalid",
            "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,1000],[-70.24,-18.05],[-70.24,-18.06],[-70.25,1000]]]} | plot.geometry.coordinates_out_of_range",
            "{\"type\":\"Polygon\",\"coordinates\":[[[-71,-18],[-70,-18],[-70,-19],[-71,-19],[-71,-18]]]} | plot.geometry.area_too_large"
    })
    @DisplayName("Rejects text that is not a GeoJSON Polygon, impossible coordinates and implausible areas")
    void rejectsWhatIsNotARealPlotOutline(String geoJson, String code) {
        assertThatThrownBy(() -> service.netAreaHa(geoJson))
                .isInstanceOf(InvalidPlotGeometryException.class)
                .hasMessage(code);
    }

    @Test
    @DisplayName("Rejects an outline with more vertices than any drawn plot has")
    void rejectsTooManyVertices() {
        var ring = new StringBuilder();
        for (int i = 0; i < 600; i++) {
            ring.append("[-70.").append(1000 + i).append(",-18.05],");
        }
        ring.append("[-70.1000,-18.05]");
        assertThatThrownBy(() -> service.validate("{\"type\":\"Polygon\",\"coordinates\":[[" + ring + "]]}"))
                .isInstanceOf(InvalidPlotGeometryException.class)
                .hasMessage("plot.geometry.vertices_max");
    }
}
