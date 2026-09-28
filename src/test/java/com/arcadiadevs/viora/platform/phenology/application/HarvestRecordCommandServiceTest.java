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
    @DisplayName("Should return not found error when rectifying harvest yield for non-existent plot in ACL")
    void shouldReturnNotFoundWhenRectifyingNonExistentPlotInAcl() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(false);

        var recordId = UUID.randomUUID().toString();
        var command = new com.arcadiadevs.viora.platform.phenology.domain.model.commands.RectifyHarvestYieldCommand(
                plotId, recordId, 13000.0, 7500.0, 5500.0, "Rectified note", 0L);
        var result = commandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure()).isPresent();
        assertThat(result.failure().get().code()).isEqualTo("PLOT_NOT_FOUND");
        verify(trackerRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Should return not found error when rectifying harvest yield and tracker is not found")
    void shouldReturnNotFoundWhenRectifyingAndTrackerNotFound() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.empty());

        var recordId = UUID.randomUUID().toString();
        var command = new com.arcadiadevs.viora.platform.phenology.domain.model.commands.RectifyHarvestYieldCommand(
                plotId, recordId, 13000.0, 7500.0, 5500.0, "Rectified note", 0L);
        var result = commandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure()).isPresent();
        assertThat(result.failure().get().code()).isEqualTo("PLOT_NOT_FOUND");
    }

    @Test
    @DisplayName("Should successfully rectify harvest yield and publish events")
    void shouldSuccessfullyRectifyHarvestYield() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        var tracker = com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker.create(
                new PlotId(plotId), new CampaignYear(2024));
        var entry = tracker.recordHarvest(new CampaignYear(2024), new com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestYield(12000.0, 7000.0, 5000.0));
        var entryId = entry.snapshot().id().harvestEntryId();

        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.of(tracker));
        when(trackerRepository.save(any())).thenReturn(tracker);

        var command = new com.arcadiadevs.viora.platform.phenology.domain.model.commands.RectifyHarvestYieldCommand(
                plotId, entryId, 13000.0, 7500.0, 5500.0, "Updated yield", 1L);
        var result = commandService.handle(command);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success()).contains(entryId);
        verify(trackerRepository).save(tracker);
        verify(eventPublisher).publishEvent(any(com.arcadiadevs.viora.platform.phenology.domain.model.events.HistoricalHarvestRectifiedEvent.class));
    }

    @Test
    @DisplayName("Should return precondition failed error on revision mismatch during rectification")
    void shouldReturnPreconditionFailedWhenRevisionMismatch() {
        when(externalOrchardService.existsActivePlot(any(PlotId.class))).thenReturn(true);
        var tracker = com.arcadiadevs.viora.platform.phenology.domain.model.aggregates.ChillAccumulationTracker.create(
                new PlotId(plotId), new CampaignYear(2024));
        var entry = tracker.recordHarvest(new CampaignYear(2024), new com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.HarvestYield(12000.0, 7000.0, 5000.0));
        var entryId = entry.snapshot().id().harvestEntryId();

        when(trackerRepository.findByPlotId(any(PlotId.class))).thenReturn(Optional.of(tracker));

        // Tracker revision is 1, but command expects revision 99
        var command = new com.arcadiadevs.viora.platform.phenology.domain.model.commands.RectifyHarvestYieldCommand(
                plotId, entryId, 13000.0, 7500.0, 5500.0, "Updated yield", 99L);
        var result = commandService.handle(command);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure()).isPresent();
        assertThat(result.failure().get().code()).isEqualTo("TRACKER_PRECONDITION_FAILED");
        assertThat(result.failure().get().details()).isEqualTo("phenology.tracker.revision.mismatch");
        verify(trackerRepository, never()).save(any());
    }
}
