package com.arcadiadevs.viora.platform.phenology.application;

import com.arcadiadevs.viora.platform.phenology.application.internal.commandservices.HarvestRecordCommandServiceImpl;
import com.arcadiadevs.viora.platform.phenology.domain.model.commands.RecordHarvestYieldCommand;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.repositories.ChillAccumulationTrackerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HarvestRecordCommandService Unit Tests")
class HarvestRecordCommandServiceTest {

    @Mock
    private ChillAccumulationTrackerRepository trackerRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService externalOrchardService;

    private HarvestRecordCommandServiceImpl commandService;

    private final String plotId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        commandService = new HarvestRecordCommandServiceImpl(trackerRepository, eventPublisher, externalOrchardService);
    }

    @Test
    @DisplayName("Should return not found error when plot does not exist in orchard")
    void shouldReturnNotFoundWhenPlotDoesNotExist() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(false);

        var command = new RecordHarvestYieldCommand(plotId, 2024, 12000.0, 7000.0, 5000.0, "Great harvest");
        var result = commandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure()).isPresent();
        assertThat(result.failure().get().code()).isEqualTo("PLOT_NOT_FOUND");
        assertThat(result.failure().get().message()).contains(plotId);

        verify(trackerRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should successfully record harvest yield when valid command")
    void shouldRecordHarvestYieldSuccessfully() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        when(trackerRepository.existsByPlotIdAndCampaignYear(any(PlotId.class), any(CampaignYear.class)))
                .thenReturn(false);
        when(trackerRepository.findByPlotId(any(PlotId.class)))
                .thenReturn(Optional.empty());

        var command = new RecordHarvestYieldCommand(plotId, 2024, 12000.0, 7000.0, 5000.0, "Great harvest");
        var result = commandService.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success()).isPresent();

        verify(trackerRepository, times(1)).save(any());
        verify(eventPublisher).publishEvent(any(com.arcadiadevs.viora.platform.phenology.domain.model.events.HarvestYieldRecordedEvent.class));
    }

    @Test
    @DisplayName("Should return conflict when campaign year is duplicate for plot")
    void shouldReturnConflictWhenDuplicateCampaignYear() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        when(trackerRepository.existsByPlotIdAndCampaignYear(any(PlotId.class), any(CampaignYear.class)))
                .thenReturn(true);

        var command = new RecordHarvestYieldCommand(plotId, 2024, 12000.0, 7000.0, 5000.0, "Great harvest");
        var result = commandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure()).isPresent();
        assertThat(result.failure().get().code()).isEqualTo("HARVEST_CONFLICT");
        assertThat(result.failure().get().details()).isEqualTo("phenology.harvest_yield.duplicate_campaign");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException when constructor dependencies are null")
    @SuppressWarnings("DataFlowIssue")
    void shouldThrowWhenConstructorDependenciesNull() {
        assertThatThrownBy(() -> new HarvestRecordCommandServiceImpl(null, eventPublisher, externalOrchardService))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("harvest.repository.null");

        assertThatThrownBy(() -> new HarvestRecordCommandServiceImpl(trackerRepository, null, externalOrchardService))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("harvest.event_publisher.null");

        assertThatThrownBy(() -> new HarvestRecordCommandServiceImpl(trackerRepository, eventPublisher, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("harvest.external_orchard_service.null");
    }
}
