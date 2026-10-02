package com.arcadiadevs.viora.platform.orchard.application.acl;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.PlotSnapshot;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrchardContextFacadeImpl ACL Unit Tests")
class OrchardContextFacadeImplTest {

    @Mock
    private PlotRepository plotRepository;

    private OrchardContextFacadeImpl facade;

    private final UUID plotId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        facade = new OrchardContextFacadeImpl(plotRepository);
    }

    @Test
    @DisplayName("Should return true when plot exists and status is ACTIVE")
    void shouldReturnTrueWhenPlotExistsAndIsActive() {
        var plotMock = mock(Plot.class);
        var snapshot = new PlotSnapshot(
                new PlotId(plotId.toString()),
                new ProducerId(UUID.randomUUID().toString()),
                new PlotName("Cuartel A"),
                OliveVariety.ARBEQUINA,
                new PlotGeometry("{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}", 1.0),
                new PlantationFrame(7.0, 5.0),
                new TreeDensity(286),
                null,
                PlotStatus.ACTIVE,
                0L
        );
        when(plotMock.snapshot()).thenReturn(snapshot);
        when(plotRepository.findById(new PlotId(plotId.toString()))).thenReturn(Optional.of(plotMock));

        var exists = facade.existsActivePlot(plotId.toString());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("Should return false when plot exists but status is REMOVED_SOFT_DELETE")
    void shouldReturnFalseWhenPlotExistsButIsRemoved() {
        var plotMock = mock(Plot.class);
        var snapshot = new PlotSnapshot(
                new PlotId(plotId.toString()),
                new ProducerId(UUID.randomUUID().toString()),
                new PlotName("Cuartel Baja"),
                OliveVariety.ARBEQUINA,
                new PlotGeometry("{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}", 1.0),
                new PlantationFrame(7.0, 5.0),
                new TreeDensity(286),
                null,
                PlotStatus.REMOVED_SOFT_DELETE,
                1L
        );
        when(plotMock.snapshot()).thenReturn(snapshot);
        when(plotRepository.findById(new PlotId(plotId.toString()))).thenReturn(Optional.of(plotMock));

        var exists = facade.existsActivePlot(plotId.toString());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return false when plot does not exist in repository")
    void shouldReturnFalseWhenPlotNotFound() {
        when(plotRepository.findById(any(PlotId.class))).thenReturn(Optional.empty());

        var exists = facade.existsActivePlot(UUID.randomUUID().toString());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("Should return false when plotId is null, blank, or malformed UUID")
    void shouldReturnFalseWhenPlotIdInvalid() {
        assertThat(facade.existsActivePlot(null)).isFalse();
        assertThat(facade.existsActivePlot("")).isFalse();
        assertThat(facade.existsActivePlot("   ")).isFalse();
        assertThat(facade.existsActivePlot("not-a-valid-uuid")).isFalse();
    }

    @Test
    @DisplayName("Should expose the variety name of an active plot only")
    void shouldExposeVarietyOfActivePlotOnly() {
        var activePlot = mock(Plot.class);
        when(activePlot.snapshot()).thenReturn(snapshot(OliveVariety.SEVILLANA, PlotStatus.ACTIVE));
        when(plotRepository.findById(new PlotId(plotId.toString()))).thenReturn(Optional.of(activePlot));
        assertThat(facade.findPlotVariety(plotId.toString())).contains("SEVILLANA");

        var removedPlot = mock(Plot.class);
        when(removedPlot.snapshot()).thenReturn(snapshot(OliveVariety.SEVILLANA, PlotStatus.REMOVED_SOFT_DELETE));
        when(plotRepository.findById(new PlotId(plotId.toString()))).thenReturn(Optional.of(removedPlot));
        assertThat(facade.findPlotVariety(plotId.toString())).isEmpty();
    }

    @Test
    @DisplayName("Should expose the owner producer of an active plot only")
    void shouldExposeOwnerOfActivePlotOnly() {
        var activePlot = mock(Plot.class);
        var snapshot = snapshot(OliveVariety.CRIOLLA, PlotStatus.ACTIVE);
        when(activePlot.snapshot()).thenReturn(snapshot);
        when(plotRepository.findById(new PlotId(plotId.toString()))).thenReturn(Optional.of(activePlot));
        assertThat(facade.findPlotProducerId(plotId.toString())).contains(snapshot.producerId().producerId());

        var removedPlot = mock(Plot.class);
        when(removedPlot.snapshot()).thenReturn(snapshot(OliveVariety.CRIOLLA, PlotStatus.REMOVED_SOFT_DELETE));
        when(plotRepository.findById(new PlotId(plotId.toString()))).thenReturn(Optional.of(removedPlot));
        assertThat(facade.findPlotProducerId(plotId.toString())).isEmpty();
        assertThat(facade.findPlotProducerId("not-a-valid-uuid")).isEmpty();
    }

    @Test
    @DisplayName("Should return no variety when plotId is null, blank, or malformed UUID")
    void shouldReturnNoVarietyWhenPlotIdInvalid() {
        assertThat(facade.findPlotVariety(null)).isEmpty();
        assertThat(facade.findPlotVariety(" ")).isEmpty();
        assertThat(facade.findPlotVariety("not-a-valid-uuid")).isEmpty();
    }

    private PlotSnapshot snapshot(OliveVariety variety, PlotStatus status) {
        return new PlotSnapshot(
                new PlotId(plotId.toString()),
                new ProducerId(UUID.randomUUID().toString()),
                new PlotName("Cuartel Variedad"),
                variety,
                new PlotGeometry("{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}", 1.0),
                new PlantationFrame(7.0, 5.0),
                new TreeDensity(286),
                null,
                status,
                0L
        );
    }
}
