package com.arcadiadevs.viora.platform.orchard.application;

import com.arcadiadevs.viora.platform.orchard.application.internal.commandservices.PlotCommandServiceImpl;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.Plot;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.RemovePlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.RestorePlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.UpdatePlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.*;
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
    @SuppressWarnings({"all", "ConstantConditions"})
    void shouldReturnFailureWhenCommandIsNull() {
        DelimitPlotCommand nullCommand = null;
        var result = plotCommandService.handle(nullCommand);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().orElseThrow().code()).isEqualTo("UNEXPECTED_ERROR");
    }

    @Test
    @DisplayName("Should enforce constructor dependency null guards")
    @SuppressWarnings({"DataFlowIssue", "ConstantConditions"})
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

    @Test
    @DisplayName("Should successfully handle UpdatePlotCommand and update boundaries")
    void shouldSuccessfullyUpdatePlot() {
        var prodId = new ProducerId(producerId.toString());
        var plot = Plot.delimit(
                prodId,
                new PlotName("Cuartel Antiguo"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );
        var plotId = plot.snapshot().id();

        when(plotRepository.findById(plotId)).thenReturn(java.util.Optional.of(plot));
        when(plotRepository.existsByNameAndProducerId(any(PlotName.class), eq(prodId))).thenReturn(false);
        when(plotRepository.save(any(Plot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var updateCommand = new UpdatePlotCommand(
                plotId.plotId(),
                prodId.producerId(),
                "Cuartel Rectificado",
                6.5,
                4.5,
                java.time.LocalDate.of(2026, 9, 10),
                validGeoJson,
                0L
        );

        var result = plotCommandService.handle(updateCommand);

        assertThat(result.isSuccess()).isTrue();
        var updated = result.success().orElseThrow();
        assertThat(updated.snapshot().name().value()).isEqualTo("Cuartel Rectificado");
        assertThat(updated.snapshot().revision()).isEqualTo(1L);
        verify(plotRepository, times(1)).save(plot);
        verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Should return Precondition Failed when revision does not match expected revision")
    void shouldReturnPreconditionFailedWhenRevisionMismatch() {
        var prodId = new ProducerId(producerId.toString());
        var plot = Plot.delimit(
                prodId,
                new PlotName("Cuartel Antiguo"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );
        var plotId = plot.snapshot().id();

        when(plotRepository.findById(plotId)).thenReturn(java.util.Optional.of(plot));

        var updateCommand = new UpdatePlotCommand(
                plotId.plotId(),
                prodId.producerId(),
                "Cuartel Rectificado",
                6.5,
                4.5,
                null,
                validGeoJson,
                5L // current is 0L
        );

        var result = plotCommandService.handle(updateCommand);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().orElseThrow().code()).isEqualTo("PLOT_PRECONDITION_FAILED");
        verify(plotRepository, never()).save(any(Plot.class));
    }

    @Test
    @DisplayName("Should return Not Found when updating a plot that does not exist")
    void shouldReturnNotFoundWhenUpdatingNonExistentPlot() {
        var nonExistentPlotId = new PlotId();
        when(plotRepository.findById(nonExistentPlotId)).thenReturn(java.util.Optional.empty());

        var updateCommand = new UpdatePlotCommand(
                nonExistentPlotId.plotId(),
                producerId.toString(),
                "Cuartel Nuevo",
                6.5,
                4.5,
                null,
                validGeoJson,
                0L
        );

        var result = plotCommandService.handle(updateCommand);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().orElseThrow().code()).isEqualTo("PLOT_NOT_FOUND");
    }

    @Test
    @DisplayName("Should successfully soft delete plot and publish PlotRemovedEvent event")
    void shouldRemovePlotSuccessfully() {
        var plot = Plot.delimit(
                new ProducerId(producerId.toString()),
                new PlotName("Cuartel A Eliminar"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );
        var plotId = plot.snapshot().id();

        when(plotRepository.findById(plotId)).thenReturn(java.util.Optional.of(plot));
        when(plotRepository.save(any(Plot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var command = new RemovePlotCommand(
                plotId.plotId(),
                producerId.toString(),
                "Cierre de cuartel"
        );

        var result = plotCommandService.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success().orElseThrow()).isEqualTo(plotId.plotId());
        verify(plotRepository).save(plot);
        verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Should return Not Found when removing a plot that does not exist")
    void shouldReturnNotFoundWhenRemovingNonExistentPlot() {
        var nonExistentPlotId = new PlotId();
        when(plotRepository.findById(nonExistentPlotId)).thenReturn(java.util.Optional.empty());

        var command = new RemovePlotCommand(
                nonExistentPlotId.plotId(),
                producerId.toString(),
                "Remocion"
        );

        var result = plotCommandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().orElseThrow().code()).isEqualTo("PLOT_NOT_FOUND");
        verify(plotRepository, never()).save(any(Plot.class));
    }

    @Test
    @DisplayName("Should return Conflict when removing a plot that is already removed")
    void shouldReturnConflictWhenPlotIsAlreadyRemoved() {
        var plot = Plot.delimit(
                new ProducerId(producerId.toString()),
                new PlotName("Cuartel Ya Desactivado"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );
        plot.remove("Primera remocion");

        var plotId = plot.snapshot().id();
        when(plotRepository.findById(plotId)).thenReturn(java.util.Optional.of(plot));

        var command = new RemovePlotCommand(
                plotId.plotId(),
                producerId.toString(),
                "Segunda remocion"
        );

        var result = plotCommandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().orElseThrow().code()).isEqualTo("PLOT_CONFLICT");
    }

    @Test
    @DisplayName("Should correct the variety of a plot when the update command carries one")
    void shouldCorrectVarietyWhenUpdatingPlot() {
        var prodId = new ProducerId(producerId.toString());
        var plot = Plot.delimit(
                prodId,
                new PlotName("Cuartel Antiguo"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );
        var plotId = plot.snapshot().id();
        when(plotRepository.findById(plotId)).thenReturn(java.util.Optional.of(plot));
        when(plotRepository.save(any(Plot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = plotCommandService.handle(new UpdatePlotCommand(
                plotId.plotId(), prodId.producerId(), "Cuartel Antiguo", 7.0, 5.0, null, validGeoJson, "sevillana", 0L));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success().orElseThrow().snapshot().variety()).isEqualTo(OliveVariety.SEVILLANA);
    }

    @Test
    @DisplayName("Should return a validation error when the corrected variety is unknown")
    void shouldRejectUnknownVarietyWhenUpdatingPlot() {
        var prodId = new ProducerId(producerId.toString());
        var plot = Plot.delimit(
                prodId,
                new PlotName("Cuartel Antiguo"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );
        var plotId = plot.snapshot().id();
        when(plotRepository.findById(plotId)).thenReturn(java.util.Optional.of(plot));

        var result = plotCommandService.handle(new UpdatePlotCommand(
                plotId.plotId(), prodId.producerId(), "Cuartel Antiguo", 7.0, 5.0, null, validGeoJson, "PICUAL", 0L));

        assertThat(result.isFailure()).isTrue();
        verify(plotRepository, never()).save(any(Plot.class));
    }

    private Plot archivedPlot(ProducerId prodId) {
        var plot = Plot.delimit(
                prodId,
                new PlotName("Cuartel Archivado"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );
        plot.remove("No longer farmed");
        plot.clearDomainEvents();
        return plot;
    }

    @Test
    @DisplayName("Should restore an archived plot and publish PlotRestoredEvent")
    void shouldRestoreArchivedPlot() {
        var prodId = new ProducerId(producerId.toString());
        var plot = archivedPlot(prodId);
        var plotId = plot.snapshot().id();
        when(plotRepository.findById(plotId)).thenReturn(java.util.Optional.of(plot));
        when(plotRepository.save(any(Plot.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = plotCommandService.handle(new RestorePlotCommand(plotId.plotId(), prodId.producerId()));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success().orElseThrow().snapshot().status()).isEqualTo(PlotStatus.ACTIVE);
        verify(plotRepository, times(1)).save(plot);
        verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Should return Not Found when restoring a plot that does not exist or belongs to another producer")
    void shouldReturnNotFoundWhenRestoringUnknownOrForeignPlot() {
        var foreign = archivedPlot(new ProducerId(UUID.randomUUID().toString()));
        var foreignId = foreign.snapshot().id();
        when(plotRepository.findById(foreignId)).thenReturn(java.util.Optional.of(foreign));
        var missingId = new PlotId();
        when(plotRepository.findById(missingId)).thenReturn(java.util.Optional.empty());

        var foreignResult = plotCommandService.handle(new RestorePlotCommand(foreignId.plotId(), producerId.toString()));
        var missingResult = plotCommandService.handle(new RestorePlotCommand(missingId.plotId(), producerId.toString()));

        assertThat(foreignResult.failure().orElseThrow().code()).isEqualTo("PLOT_NOT_FOUND");
        assertThat(missingResult.failure().orElseThrow().code()).isEqualTo("PLOT_NOT_FOUND");
        verify(plotRepository, never()).save(any(Plot.class));
    }

    @Test
    @DisplayName("Should return Conflict when restoring a plot that is still active")
    void shouldReturnConflictWhenRestoringActivePlot() {
        var prodId = new ProducerId(producerId.toString());
        var plot = Plot.delimit(
                prodId,
                new PlotName("Cuartel Activo"),
                OliveVariety.CRIOLLA,
                new PlotGeometry(validGeoJson, 1.25),
                new PlantationFrame(7.0, 5.0)
        );
        var plotId = plot.snapshot().id();
        when(plotRepository.findById(plotId)).thenReturn(java.util.Optional.of(plot));

        var result = plotCommandService.handle(new RestorePlotCommand(plotId.plotId(), prodId.producerId()));

        assertThat(result.isFailure()).isTrue();
        verify(plotRepository, never()).save(any(Plot.class));
    }
}
