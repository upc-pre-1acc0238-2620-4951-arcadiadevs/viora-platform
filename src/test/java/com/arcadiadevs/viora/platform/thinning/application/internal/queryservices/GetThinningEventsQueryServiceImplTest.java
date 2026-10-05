package com.arcadiadevs.viora.platform.thinning.application.internal.queryservices;

import com.arcadiadevs.viora.platform.thinning.PrescriptionTestData;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.ExecutionConfirmationSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.SamplingRoundSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.TreeSamplingRecordSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetThinningEventsQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GetThinningEventsQueryService Unit Tests")
class GetThinningEventsQueryServiceImplTest {

    @Mock
    private FruitThinningPrescriptionRepository prescriptionRepository;

    @Mock
    private ExternalOrchardService externalOrchardService;

    private GetThinningEventsQueryServiceImpl queryService;

    private final PlotId plotId = new PlotId("550e8400-e29b-41d4-a716-446655440001");
    private final CampaignYear campaignYear = new CampaignYear(2026);
    private final String actorId = "550e8400-e29b-41d4-a716-446655440000";

    @BeforeEach
    void setUp() {
        queryService = new GetThinningEventsQueryServiceImpl(prescriptionRepository, externalOrchardService);
    }

    @Test
    @DisplayName("Should return NOT_FOUND error when plot does not exist in orchard context")
    void shouldReturnNotFoundWhenPlotDoesNotExist() {
        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(false);

        var query = new GetThinningEventsQuery(actorId, campaignYear, plotId);
        var result = queryService.handle(query);

        assertThat(result.isFailure()).isTrue();
        assertThat(result.failure().orElseThrow().code()).isEqualTo("PLOT_NOT_FOUND");
        verify(prescriptionRepository, never()).findByPlotIdInAndCampaignYear(any(), any());
    }

    @Test
    @DisplayName("Should return empty list when plot exists but no prescription is found")
    void shouldReturnEmptyListWhenNoPrescriptionFound() {
        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(true);
        when(prescriptionRepository.findByPlotIdInAndCampaignYear(List.of(plotId), campaignYear)).thenReturn(List.of());

        var query = new GetThinningEventsQuery(actorId, campaignYear, plotId);
        var result = queryService.handle(query);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.success().orElseThrow()).isEmpty();
    }

    @Test
    @DisplayName("Should return SAMPLING_COMPLETED event when prescription has representative sampling round")
    void shouldReturnSamplingCompletedEvent() {
        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(true);
        when(externalOrchardService.findPlotName(plotId)).thenReturn(Optional.of("Plot 101"));

        var prescription = PrescriptionTestData.representative(plotId);
        when(prescriptionRepository.findByPlotIdInAndCampaignYear(List.of(plotId), campaignYear))
                .thenReturn(List.of(prescription));

        var query = new GetThinningEventsQuery(actorId, campaignYear, plotId);
        var result = queryService.handle(query);

        assertThat(result.isSuccess()).isTrue();
        var events = result.success().orElseThrow();
        assertThat(events).hasSize(1);

        var event = events.getFirst();
        assertThat(event.eventType()).isEqualTo("SAMPLING_COMPLETED");
        assertThat(event.plotId()).isEqualTo(plotId);
        assertThat(event.plotName()).isEqualTo("Plot 101");
        assertThat(event.campaignYear()).isEqualTo(campaignYear);
        assertThat(event.evaluatedTreesCount()).isEqualTo(5);
        assertThat(event.totalFruitsCount()).isEqualTo(60); // 5 trees * 12 fruits
        assertThat(event.totalShootsCount()).isEqualTo(100); // 5 trees * 20 shoots
        assertThat(event.meanFruitsPerShoot()).isEqualTo(0.6);
        assertThat(event.isRepresentative()).isTrue();
    }

    @Test
    @DisplayName("Should return both SAMPLING_COMPLETED and THINNING_EXECUTED sorted descending by occurredAt")
    void shouldReturnEventsSortedDescending() {
        when(externalOrchardService.existsActivePlot(plotId)).thenReturn(true);
        when(externalOrchardService.findPlotName(plotId)).thenReturn(Optional.of("Plot 101"));

        var samplingInstant = Instant.parse("2026-11-10T09:00:00Z");
        var round = new SamplingRoundSnapshot(
                new RoundId(),
                new UserId(UUID.randomUUID().toString()),
                new SamplingBatchId(UUID.randomUUID().toString()),
                true,
                List.of(
                        new TreeSamplingRecordSnapshot(new SamplingRecordId(), new TreeTag("T-1"), new ShootFruitCount(10, 60), null, LocalDate.now()),
                        new TreeSamplingRecordSnapshot(new SamplingRecordId(), new TreeTag("T-2"), new ShootFruitCount(10, 60), null, LocalDate.now()),
                        new TreeSamplingRecordSnapshot(new SamplingRecordId(), new TreeTag("T-3"), new ShootFruitCount(10, 60), null, LocalDate.now()),
                        new TreeSamplingRecordSnapshot(new SamplingRecordId(), new TreeTag("T-4"), new ShootFruitCount(10, 60), null, LocalDate.now()),
                        new TreeSamplingRecordSnapshot(new SamplingRecordId(), new TreeTag("T-5"), new ShootFruitCount(10, 60), null, LocalDate.now())
                ),
                samplingInstant
        );

        var confirmationSnapshot = new ExecutionConfirmationSnapshot(
                new ConfirmationId(),
                LocalDate.parse("2026-11-15"),
                30.0,
                4,
                ExecutionTimeliness.OPTIMAL,
                Instant.parse("2026-11-15T16:00:00Z"),
                420.0,
                null,
                null,
                null
        );

        var snapshot = new FruitThinningPrescriptionSnapshot(
                new PrescriptionId(),
                plotId,
                campaignYear,
                1L,
                PrescriptionStatus.EXECUTED,
                new SustainableCropLoad(6.0, 25.0, null, LocalDate.parse("2026-11-20"), null, null),
                Instant.parse("2026-11-01T00:00:00Z"),
                List.of(round),
                confirmationSnapshot,
                1L
        );
        var prescription = FruitThinningPrescription.reconstitute(snapshot);

        when(prescriptionRepository.findByPlotIdInAndCampaignYear(List.of(plotId), campaignYear))
                .thenReturn(List.of(prescription));

        var query = new GetThinningEventsQuery(actorId, campaignYear, plotId);
        var result = queryService.handle(query);

        assertThat(result.isSuccess()).isTrue();
        var events = result.success().orElseThrow();
        assertThat(events).hasSize(2);

        // First should be the execution event (occurred on Nov 15)
        assertThat(events.get(0).eventType()).isEqualTo("THINNING_EXECUTED");
        assertThat(events.get(0).removalPercentage()).isEqualTo(30.0);
        assertThat(events.get(0).timeliness()).isEqualTo("OPTIMAL");
        assertThat(events.get(0).laborCrewSize()).isEqualTo(4);
        assertThat(events.get(0).removedKg()).isEqualTo(420.0);

        // Second should be the sampling event (occurred on Nov 10)
        assertThat(events.get(1).eventType()).isEqualTo("SAMPLING_COMPLETED");
        assertThat(events.get(1).occurredAt()).isEqualTo(samplingInstant);
    }
}
