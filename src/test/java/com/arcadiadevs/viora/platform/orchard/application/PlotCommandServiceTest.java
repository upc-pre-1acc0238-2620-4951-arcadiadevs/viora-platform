package com.arcadiadevs.viora.platform.orchard.application;

import com.arcadiadevs.viora.platform.orchard.application.internal.commandservices.PlotCommandServiceImpl;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotName;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import com.arcadiadevs.viora.platform.orchard.domain.services.CadastralGeometryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlotCommandService Application Unit Tests")
class PlotCommandServiceTest {

    @Mock
    private PlotRepository plotRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private PlotCommandServiceImpl plotCommandService;
    private final UUID producerId = UUID.randomUUID();
    private final String validGeoJson = "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}";

    @BeforeEach
    void setUp() {
        var geometryService = new CadastralGeometryService();
        plotCommandService = new PlotCommandServiceImpl(plotRepository, geometryService, eventPublisher);
    }

    @Test
    @DisplayName("Should successfully create and save plot when name is unique")
    void shouldCreatePlotSuccessfully() {
        var command = new DelimitPlotCommand(
                producerId.toString(),
                "Cuartel El Olivo",
                "CRIOLLA",
                validGeoJson,
                7.0,
                5.0
        );

        when(plotRepository.existsByNameAndProducerId(any(PlotName.class), eq(new ProducerId(producerId.toString())))).thenReturn(false);
        when(plotRepository.save(any(Plot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = plotCommandService.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success()).isPresent();
        verify(plotRepository).save(any(Plot.class));
        verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Should return Failure result when plot name already exists for producer")
    void shouldReturnFailureWhenPlotNameIsDuplicate() {
        var command = new DelimitPlotCommand(
                producerId.toString(),
                "Cuartel Existente",
                "CRIOLLA",
                validGeoJson,
                7.0,
                5.0
        );

        when(plotRepository.existsByNameAndProducerId(any(PlotName.class), eq(new ProducerId(producerId.toString())))).thenReturn(true);

        var result = plotCommandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().orElseThrow().code()).isEqualTo("PLOT_CONFLICT");

        verify(plotRepository, never()).save(any(Plot.class));
    }

    @Test
    @DisplayName("Should return Failure result when variety is invalid")
    void shouldReturnFailureWhenVarietyIsInvalid() {
        var command = new DelimitPlotCommand(
                producerId.toString(),
                "Cuartel Variedad Invalida",
                "INVALID_VARIETY",
                validGeoJson,
                7.0,
                5.0
        );

        when(plotRepository.existsByNameAndProducerId(any(PlotName.class), eq(new ProducerId(producerId.toString())))).thenReturn(false);

        var result = plotCommandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().orElseThrow().code()).isEqualTo("VALIDATION_ERROR");

        verify(plotRepository, never()).save(any(Plot.class));
    }

    @Test
    @DisplayName("Should return Failure result when command is null")
    void shouldReturnFailureWhenCommandIsNull() {
        var result = plotCommandService.handle(null);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().orElseThrow().code()).isEqualTo("UNEXPECTED_ERROR");
    }

    @Test
    @DisplayName("Should enforce constructor dependency null guards")
    void shouldEnforceConstructorNullGuards() {
        var geometryService = new CadastralGeometryService();

        assertThatThrownBy(() -> new PlotCommandServiceImpl(null, geometryService, eventPublisher))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.repository.null");

        assertThatThrownBy(() -> new PlotCommandServiceImpl(plotRepository, null, eventPublisher))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.cadastral_geometry_service.null");

        assertThatThrownBy(() -> new PlotCommandServiceImpl(plotRepository, geometryService, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.event_publisher.null");
    }

    @Test
    @DisplayName("Should validate DelimitPlotCommand non-null constraints")
    void shouldValidateDelimitPlotCommandConstraints() {
        var prodIdStr = producerId.toString();

        assertThatThrownBy(() -> new DelimitPlotCommand(null, "Name", "CRIOLLA", validGeoJson, 7.0, 5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("producer.id.null_or_empty");

        assertThatThrownBy(() -> new DelimitPlotCommand(prodIdStr, null, "CRIOLLA", validGeoJson, 7.0, 5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.name.blank");

        assertThatThrownBy(() -> new DelimitPlotCommand(prodIdStr, "Name", null, validGeoJson, 7.0, 5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.variety.blank");

        assertThatThrownBy(() -> new DelimitPlotCommand(prodIdStr, "Name", "CRIOLLA", null, 7.0, 5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.geometry.empty");

        assertThatThrownBy(() -> new DelimitPlotCommand(prodIdStr, "Name", "CRIOLLA", validGeoJson, null, 5.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.spacing.positive");

        assertThatThrownBy(() -> new DelimitPlotCommand(prodIdStr, "Name", "CRIOLLA", validGeoJson, 7.0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("plot.spacing.positive");
    }
}
