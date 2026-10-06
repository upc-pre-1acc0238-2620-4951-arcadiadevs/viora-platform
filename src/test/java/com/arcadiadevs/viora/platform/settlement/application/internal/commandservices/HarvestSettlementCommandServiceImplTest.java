package com.arcadiadevs.viora.platform.settlement.application.internal.commandservices;

import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.ReceiptCounterInitializer;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.ReceiptCounter;
import com.arcadiadevs.viora.platform.settlement.domain.model.commands.SettleCampaignHarvestCommand;
import com.arcadiadevs.viora.platform.settlement.domain.model.events.CampaignHarvestSettledEvent;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.ReceiptCounterRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.SettledHarvestRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.ThinningExecutionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HarvestSettlementCommandServiceImplTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-02T12:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate WEIGHED_ON = LocalDate.of(2026, 9, 20);
    private final AgronomicReportRepository reports = mock(AgronomicReportRepository.class);
    private final ThinningExecutionRecordRepository thinningRecords = mock(ThinningExecutionRecordRepository.class);
    private final ReceiptCounterRepository counters = mock(ReceiptCounterRepository.class);
    private final SettledHarvestRepository settledHarvests = mock(SettledHarvestRepository.class);
    private final ReceiptCounterInitializer counterInitializer = mock(ReceiptCounterInitializer.class);
    private final ExternalOrchardService orchard = mock(ExternalOrchardService.class);
    private final ExternalPhenologyService phenology = mock(ExternalPhenologyService.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final HarvestSettlementCommandServiceImpl service =
            new HarvestSettlementCommandServiceImpl(reports, thinningRecords, counters, settledHarvests,
                    counterInitializer, orchard, phenology, publisher, CLOCK);
    private final String plotId = UUID.randomUUID().toString();
    private final String owner = UUID.randomUUID().toString();
    /** One counter row per producer and campaign, the way the database keeps them. */
    private final Map<String, ReceiptCounter> counterRows = new HashMap<>();
    private final Map<String, AgronomicReport> reportRows = new HashMap<>();
    private SettleCampaignHarvestCommand command;

    @BeforeEach
    void setUp() {
        command = new SettleCampaignHarvestCommand(plotId, owner, 2026, 8200.0, 6050.0, null, null,
                WEIGHED_ON, null, null);
        when(phenology.findHistoricalYields(any())).thenReturn(new TreeMap<>());
        when(reports.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        // A real row: the same mutable counter instance comes back for the same producer and campaign, so a
        // second settlement of that pair continues the numbering instead of restarting it.
        when(counters.findByProducerIdAndCampaignYearForUpdate(any(), any())).thenAnswer(invocation -> {
            UserId producer = invocation.getArgument(0);
            CampaignYear campaignYear = invocation.getArgument(1);
            return Optional.of(counterRows.computeIfAbsent(producer.userId() + "/" + campaignYear.value(),
                    key -> ReceiptCounter.start(producer, campaignYear)));
        });
        doAnswer(invocation -> null).when(counters).save(any());
        // One report per plot, the way the database keeps them, so a second settlement finds the first one.
        when(reports.findByPlotIdForUpdate(any())).thenAnswer(invocation -> {
            PlotId plot = invocation.getArgument(0);
            return Optional.ofNullable(reportRows.get(plot.plotId()));
        });
        doAnswer(invocation -> {
            AgronomicReport report = invocation.getArgument(0);
            reportRows.put(report.snapshot().plotId().plotId(), report);
            return report;
        }).when(reports).save(any());
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
        var settlement = service.handle(command).success().orElseThrow().settlement();
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
        when(thinningRecords.findByPlotIdAndCampaignYear(new PlotId(plotId), 2026)).thenReturn(Optional.of(
                new ThinningExecutionRecord("e", UUID.randomUUID().toString(), new PlotId(plotId), 2026,
                        LocalDate.of(2026, 1, 10), 25.0, 25.0, true)));
        var balance = service.handle(command).success().orElseThrow().settlement().thinningBalance();
        assertEquals(ThinningComplianceStatus.EXECUTED_ON_TIME, balance.status());
        assertEquals(0.0, balance.deviationPercentagePoints());
    }

    @Test
    void rejectsAnAlreadySettledCampaignWithoutSavingOrPublishing() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));
        var report = AgronomicReport.createForPlot(new PlotId(plotId), new UserId(owner));
        report.settleCampaign(new CampaignYear(2026), new OliveWeight(1.0), new OliveWeight(1.0), null, null,
                ThinningBalance.notRecorded(), new TreeMap<>(), ReceiptNumber.of(new CampaignYear(2026), 1),
                WeighingDate.of(WEIGHED_ON, CLOCK), null, null, CLOCK);
        report.clearDomainEvents();
        reportRows.put(plotId, report);
        assertEquals("HARVESTSETTLEMENT_CONFLICT", service.handle(command).failure().orElseThrow().code());
        verify(reports, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsInvalidCommandsBeforeReachingTheService() {
        assertThrows(IllegalArgumentException.class,
                () -> new SettleCampaignHarvestCommand(plotId, owner, 2026, 0.0, 0.0, null, null,
                        WEIGHED_ON, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new SettleCampaignHarvestCommand("bad", owner, 2026, 1.0, 0.0, null, null,
                        WEIGHED_ON, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new SettleCampaignHarvestCommand(plotId, owner, 1999, 1.0, 0.0, null, null,
                        WEIGHED_ON, null, null));
    }

    // --- receipt numbering ---

    @Test
    void numbersTwoPlotsOfTheSameProducerInTheSameCampaignConsecutively() {
        var otherPlot = UUID.randomUUID().toString();
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));

        var first = service.handle(command).success().orElseThrow().settlement();
        var second = service.handle(commandOf(otherPlot, owner, 2026, null)).success().orElseThrow().settlement();

        assertEquals("VR-26-0001", first.receiptNumber().value());
        assertEquals("VR-26-0002", second.receiptNumber().value());
        assertEquals(1, first.receiptNumber().sequence());
        assertEquals(2, second.receiptNumber().sequence());
        // One counter for the producer and campaign, whatever the plot: it advanced once per settlement.
        assertEquals(2, counterOf(owner, 2026).lastSequence());
        verify(counters, times(2)).save(any());
        verify(counterInitializer, times(2)).ensureExists(new UserId(owner), new CampaignYear(2026));
    }

    @Test
    void numbersEachProducerAndCampaignWithItsOwnCounter() {
        var otherProducer = UUID.randomUUID().toString();
        var otherPlot = UUID.randomUUID().toString();
        when(orchard.findActivePlotOwner(new PlotId(plotId))).thenReturn(Optional.of(new UserId(owner)));
        when(orchard.findActivePlotOwner(new PlotId(otherPlot))).thenReturn(Optional.of(new UserId(otherProducer)));

        var mine = service.handle(command).success().orElseThrow().settlement();
        var ofAnotherProducer = service.handle(commandOf(otherPlot, otherProducer, 2026, null))
                .success().orElseThrow().settlement();
        var ofAnotherCampaign = service.handle(commandOf(plotId, owner, 2027, null))
                .success().orElseThrow().settlement();

        assertEquals("VR-26-0001", mine.receiptNumber().value());
        assertEquals("VR-26-0001", ofAnotherProducer.receiptNumber().value());
        assertEquals("VR-27-0001", ofAnotherCampaign.receiptNumber().value());
        assertEquals(1, counterOf(owner, 2026).lastSequence());
        assertEquals(1, counterOf(otherProducer, 2026).lastSequence());
        assertEquals(1, counterOf(owner, 2027).lastSequence());
        // The counter is asked for by producer and campaign, never by plot alone.
        verify(counters).findByProducerIdAndCampaignYearForUpdate(new UserId(otherProducer), new CampaignYear(2026));
        verify(counters).findByProducerIdAndCampaignYearForUpdate(new UserId(owner), new CampaignYear(2027));
    }

    @Test
    void opensTheCounterBeforeLockingIt() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));
        service.handle(command).success().orElseThrow();
        var order = inOrder(counterInitializer, counters);
        order.verify(counterInitializer).ensureExists(new UserId(owner), new CampaignYear(2026));
        order.verify(counters).findByProducerIdAndCampaignYearForUpdate(new UserId(owner), new CampaignYear(2026));
        order.verify(counters).save(any());
    }

    // --- idempotent replay ---

    @Test
    void replaysTheStoredSettlementOfTheSameKeyWithoutTouchingAnything() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));
        var keyed = commandOf(plotId, owner, 2026, "key-a");
        var stored = settlementOf(plotId, 2026, 1);
        when(settledHarvests.findByProducerIdAndIdempotencyKey(new UserId(owner), IdempotencyKey.of("key-a")))
                .thenReturn(Optional.of(stored));

        var outcome = service.handle(keyed).success().orElseThrow();

        assertFalse(outcome.created());
        assertEquals(stored.id(), outcome.settlement().id());
        assertEquals("VR-26-0001", outcome.settlement().receiptNumber().value());
        assertEquals(stored.totalHarvestWeight().kilograms(), outcome.settlement().totalHarvestWeight().kilograms());
        verifyNoInteractions(publisher, reports, counterInitializer);
        verifyNoInteractions(counters);
    }

    @Test
    void rejectsAKeyReusedForAnotherPlotOfTheSameProducer() {
        var otherPlot = UUID.randomUUID().toString();
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));
        when(settledHarvests.findByProducerIdAndIdempotencyKey(any(), any()))
                .thenReturn(Optional.of(settlementOf(otherPlot, 2026, 1)));

        var error = service.handle(commandOf(plotId, owner, 2026, "key-a")).failure().orElseThrow();

        assertEquals("BUSINESS_RULE_VIOLATION", error.code());
        assertEquals("settlement.idempotency_key.reused", error.details());
        verifyNoInteractions(publisher, reports, counterInitializer);
        verifyNoInteractions(counters);
    }

    @Test
    void rejectsAKeyReusedForAnotherCampaignOfTheSamePlot() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));
        when(settledHarvests.findByProducerIdAndIdempotencyKey(any(), any()))
                .thenReturn(Optional.of(settlementOf(plotId, 2027, 1)));

        var error = service.handle(commandOf(plotId, owner, 2026, "key-a")).failure().orElseThrow();

        assertEquals("BUSINESS_RULE_VIOLATION", error.code());
        assertEquals("settlement.idempotency_key.reused", error.details());
        verifyNoInteractions(publisher, reports, counterInitializer);
        verifyNoInteractions(counters);
    }

    // --- the conflict of an already settled campaign ---

    @Test
    void theConflictOfAnAlreadySettledCampaignCarriesTheExistingSettlement() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));
        var first = service.handle(commandOf(plotId, owner, 2026, "key-a")).success().orElseThrow().settlement();

        var error = service.handle(commandOf(plotId, owner, 2026, "key-b")).failure().orElseThrow();

        assertEquals("HARVESTSETTLEMENT_CONFLICT", error.code());
        assertEquals("settlement.campaign.already_settled", error.details());
        var expected = new LinkedHashMap<String, Object>();
        expected.put("campaignYear", 2026);
        expected.put("totalYieldKg", 14250.0);
        expected.put("receiptNumber", "VR-26-0001");
        expected.put("weighedOn", WEIGHED_ON);
        assertEquals(expected, error.properties().get("existingSettlement"));
        assertEquals(first.receiptNumber().value(), ((Map<?, ?>) error.properties().get("existingSettlement"))
                .get("receiptNumber"));
        // The second call neither saved nor published again, and did not burn a receipt number.
        verify(reports, times(1)).save(any());
        verify(publisher, times(1)).publishEvent(any(CampaignHarvestSettledEvent.class));
        assertEquals(1, counterOf(owner, 2026).lastSequence());
    }

    @Test
    void theConflictOfAnAlreadySettledCampaignWithoutAKeyCarriesItToo() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));
        service.handle(command).success().orElseThrow();

        var error = service.handle(command).failure().orElseThrow();

        assertEquals("HARVESTSETTLEMENT_CONFLICT", error.code());
        var existing = (Map<?, ?>) error.properties().get("existingSettlement");
        assertNotNull(existing);
        assertEquals(Set.of("campaignYear", "totalYieldKg", "receiptNumber", "weighedOn"), existing.keySet());
        assertEquals("VR-26-0001", existing.get("receiptNumber"));
    }

    private ReceiptCounter counterOf(String producer, int campaignYear) {
        return counterRows.get(producer + "/" + campaignYear);
    }

    private SettleCampaignHarvestCommand commandOf(String plot, String actor, int campaignYear, String idempotencyKey) {
        return new SettleCampaignHarvestCommand(plot, actor, campaignYear, 8200.0, 6050.0, null, null,
                WEIGHED_ON, null, idempotencyKey);
    }

    /** A settlement as the replay lookup would find it in the database, frozen with its own receipt number. */
    private HarvestSettlementSnapshot settlementOf(String plot, int campaignYear, int sequence) {
        var report = AgronomicReport.createForPlot(new PlotId(plot), new UserId(owner));
        return report.settleCampaign(new CampaignYear(campaignYear), new OliveWeight(8200.0), new OliveWeight(6050.0),
                null, null, ThinningBalance.notRecorded(), new TreeMap<>(),
                ReceiptNumber.of(new CampaignYear(campaignYear), sequence), WeighingDate.of(WEIGHED_ON, CLOCK), null,
                IdempotencyKey.of("key-a"), CLOCK);
    }
}
