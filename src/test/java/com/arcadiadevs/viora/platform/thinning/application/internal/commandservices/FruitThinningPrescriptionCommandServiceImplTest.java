package com.arcadiadevs.viora.platform.thinning.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.IngestFieldSamplingsBatchCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.TreeSampleItem;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FruitThinningPrescriptionCommandServiceImplTest {

    @Mock
    private FruitThinningPrescriptionRepository prescriptionRepository;

    @Mock
    private ExternalOrchardService externalOrchardService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private FruitThinningPrescriptionCommandServiceImpl commandService;

    @BeforeEach
    void setUp() {
        commandService = new FruitThinningPrescriptionCommandServiceImpl(
                prescriptionRepository,
                externalOrchardService,
                eventPublisher
        );
    }

    @Test
    @DisplayName("Should successfully ingest sampling batch when plot is active")
    void shouldSuccessfullyIngestSamplingBatch() {
        String plotIdStr = UUID.randomUUID().toString();
        String actorIdStr = UUID.randomUUID().toString();
        var plotId = new PlotId(plotIdStr);
        var campaignYear = new CampaignYear(2026);

        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(true);
        when(prescriptionRepository.findByPlotIdAndCampaignYear(plotId, campaignYear))
                .thenReturn(Optional.empty());
        when(prescriptionRepository.save(any(FruitThinningPrescription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var command = new IngestFieldSamplingsBatchCommand(
                plotIdStr,
                actorIdStr,
                2026,
                "batch-101",
                List.of(
                        new TreeSampleItem("T-01", 10, 100, 150.0, LocalDate.now()),
                        new TreeSampleItem("T-02", 12, 120, 160.0, LocalDate.now()),
                        new TreeSampleItem("T-03", 8, 80, 140.0, LocalDate.now()),
                        new TreeSampleItem("T-04", 10, 100, 150.0, LocalDate.now()),
                        new TreeSampleItem("T-05", 10, 100, 150.0, LocalDate.now())
                )
        );

        var result = commandService.handle(command);

        assertThat(result).isInstanceOf(Result.Success.class);
        var summary = ((Result.Success<?, ?>) result).value();
        assertThat(summary).isNotNull();

        verify(prescriptionRepository, times(1)).save(any(FruitThinningPrescription.class));
        verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Should return failure if plot does not exist in orchard context")
    void shouldReturnFailureWhenPlotNotFound() {
        String plotIdStr = UUID.randomUUID().toString();
        String actorIdStr = UUID.randomUUID().toString();
        var plotId = new PlotId(plotIdStr);

        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(false);

        var command = new IngestFieldSamplingsBatchCommand(
                plotIdStr,
                actorIdStr,
                2026,
                "batch-102",
                List.of(new TreeSampleItem("T-01", 10, 100, 150.0, LocalDate.now()))
        );

        var result = commandService.handle(command);

        assertThat(result).isInstanceOf(Result.Failure.class);
        verify(prescriptionRepository, never()).save(any());
    }
}
