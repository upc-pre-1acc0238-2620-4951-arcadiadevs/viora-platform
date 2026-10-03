package com.arcadiadevs.viora.platform.settlement.application.internal.commandservices;

import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SettleCampaignHarvestCommand;
import com.arcadiadevs.viora.platform.settlement.domain.model.events.CampaignHarvestSettledEvent;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.ThinningExecutionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HarvestSettlementCommandServiceImplTest {
    private final AgronomicReportRepository reports = mock(AgronomicReportRepository.class);
    private final ThinningExecutionRecordRepository thinningRecords = mock(ThinningExecutionRecordRepository.class);
    private final ExternalOrchardService orchard = mock(ExternalOrchardService.class);
    private final ExternalPhenologyService phenology = mock(ExternalPhenologyService.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final HarvestSettlementCommandServiceImpl service =
            new HarvestSettlementCommandServiceImpl(reports, thinningRecords, orchard, phenology, publisher,
                    java.time.Clock.fixed(java.time.Instant.parse("2026-10-02T12:00:00Z"), java.time.ZoneOffset.UTC));
    private final String plotId = UUID.randomUUID().toString();
    private final String owner = UUID.randomUUID().toString();
    private SettleCampaignHarvestCommand command;

    @BeforeEach
    void setUp() {
        command = new SettleCampaignHarvestCommand(plotId, owner, 2026, 8200.0, 6050.0, null, null);
        when(phenology.findHistoricalYields(any())).thenReturn(new TreeMap<>());
        when(reports.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void returnsNotFoundForAMissingOrInactivePlot() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.empty());
        assertEquals("PLOT_NOT_FOUND", service.handle(command).failure().orElseThrow().code());
        verifyNoInteractions(reports, publisher);
    }

    @Test
    void forbidsAProducerWhoDoesNotOwnThePlot() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(UUID.randomUUID().toString())));
        assertEquals("PLOT_FORBIDDEN", service.handle(command).failure().orElseThrow().code());
        verifyNoInteractions(reports, publisher);
    }

    @Test
    void opensTheReportOnTheFirstSettlementAndPublishesAfterSaving() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));
        when(reports.findByPlotIdForUpdate(any())).thenReturn(Optional.empty());
        var settlement = service.handle(command).success().orElseThrow();
        assertEquals(14250.0, settlement.totalHarvestWeight().kilograms());
        assertEquals(java.time.Instant.parse("2026-10-02T12:00:00Z"), settlement.settledAt());
        assertEquals(ThinningComplianceStatus.NOT_RECORDED, settlement.thinningBalance().status());
        var order = inOrder(reports, publisher);
        order.verify(reports).save(any());
        order.verify(publisher).publishEvent(any(CampaignHarvestSettledEvent.class));
    }

    @Test
    void freezesTheThinningExecutionOfTheSameCampaign() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));
        when(reports.findByPlotIdForUpdate(any())).thenReturn(Optional.empty());
        when(thinningRecords.findByPlotIdAndCampaignYear(new PlotId(plotId), 2026)).thenReturn(Optional.of(
                new ThinningExecutionRecord("e", UUID.randomUUID().toString(), new PlotId(plotId), 2026,
                        LocalDate.of(2026, 1, 10), 25.0, 25.0, true)));
        var balance = service.handle(command).success().orElseThrow().thinningBalance();
        assertEquals(ThinningComplianceStatus.EXECUTED_ON_TIME, balance.status());
        assertEquals(0.0, balance.deviationPercentagePoints());
    }

    @Test
    void rejectsAnAlreadySettledCampaignWithoutSavingOrPublishing() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));
        var report = AgronomicReport.createForPlot(new PlotId(plotId), new UserId(owner));
        report.settleCampaign(new CampaignYear(2026), new OliveWeight(1.0), new OliveWeight(1.0), null, null,
                ThinningBalance.notRecorded(), new TreeMap<>(), java.time.Clock.systemUTC());
        report.clearDomainEvents();
        when(reports.findByPlotIdForUpdate(any())).thenReturn(Optional.of(report));
        assertEquals("HARVESTSETTLEMENT_CONFLICT", service.handle(command).failure().orElseThrow().code());
        verify(reports, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsInvalidCommandsBeforeReachingTheService() {
        assertThrows(IllegalArgumentException.class,
                () -> new SettleCampaignHarvestCommand(plotId, owner, 2026, 0.0, 0.0, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new SettleCampaignHarvestCommand("bad", owner, 2026, 1.0, 0.0, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new SettleCampaignHarvestCommand(plotId, owner, 1999, 1.0, 0.0, null, null));
    }
}
