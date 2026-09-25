package com.arcadiadevs.viora.platform.orchard.application;

import com.arcadiadevs.viora.platform.orchard.application.internal.queryservices.PlotQueryServiceImpl;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetAllActivePlotsByProducerIdQuery;
import com.arcadiadevs.viora.platform.orchard.domain.model.queries.GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlotQueryService Unit Tests")
class PlotQueryServiceTest {

    @Mock
    private PlotRepository plotRepository;

    private PlotQueryServiceImpl plotQueryService;

    private final ProducerId producerId = new ProducerId();
    private final String validGeoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}";

    @BeforeEach
    void setUp() {
        plotQueryService = new PlotQueryServiceImpl(plotRepository);
    }

    @Test
    @DisplayName("Should retrieve active plots when executing GetAllActivePlotsByProducerIdQuery")
    void shouldRetrieveActivePlots() {
        var plot = Plot.delimit(
                producerId,
                new PlotName("Cuartel San Jerónimo"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );

        when(plotRepository.findActiveByProducerId(producerId))
                .thenReturn(List.of(plot));

        var query = new GetAllActivePlotsByProducerIdQuery(producerId);
        var result = plotQueryService.handle(query);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().snapshot().name().value()).isEqualTo("Cuartel San Jerónimo");
        verify(plotRepository, times(1)).findActiveByProducerId(producerId);
        verify(plotRepository, never()).findByProducerIdAndUpdatedSince(any(), any());
    }

    @Test
    @DisplayName("Should retrieve delta plots when executing GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery")
    void shouldRetrieveDeltaPlots() {
        var plot = Plot.delimit(
                producerId,
                new PlotName("Cuartel Delta"),
                OliveVariety.ARBEQUINA,
                new PlotGeometry(validGeoJson, 2.50),
                new PlantationFrame(6.0, 4.0)
        );
        var timestamp = Instant.parse("2026-09-01T00:00:00Z");

        when(plotRepository.findByProducerIdAndUpdatedSince(producerId, timestamp))
                .thenReturn(List.of(plot));

        var query = new GetPlotsDeltaSyncByProducerIdAndUpdatedSinceQuery(producerId, timestamp);
        var result = plotQueryService.handle(query);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().snapshot().name().value()).isEqualTo("Cuartel Delta");
        verify(plotRepository, times(1)).findByProducerIdAndUpdatedSince(producerId, timestamp);
        verify(plotRepository, never()).findActiveByProducerId(any());
    }

    @Test
    @DisplayName("Should return empty list when no active plots match")
    void shouldReturnEmptyListWhenNoPlotsMatch() {
        when(plotRepository.findActiveByProducerId(producerId))
                .thenReturn(Collections.emptyList());

        var query = new GetAllActivePlotsByProducerIdQuery(producerId);
        var result = plotQueryService.handle(query);

        assertThat(result).isEmpty();
        verify(plotRepository, times(1)).findActiveByProducerId(producerId);
    }
}
