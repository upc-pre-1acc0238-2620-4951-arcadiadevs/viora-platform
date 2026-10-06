package com.arcadiadevs.viora.platform.settlement.application.internal.queryservices;

import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetHarvestSettlementByPlotIdAndCampaignYearQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetHarvestSettlementsByPlotIdQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HarvestSettlementQueryServiceImplTest {
    private static final java.time.Clock CLOCK =
            java.time.Clock.fixed(java.time.Instant.parse("2026-10-02T12:00:00Z"), java.time.ZoneOffset.UTC);
    private final AgronomicReportRepository reports = mock(AgronomicReportRepository.class);
    private final ExternalOrchardService orchard = mock(ExternalOrchardService.class);
    private final HarvestSettlementQueryServiceImpl service = new HarvestSettlementQueryServiceImpl(reports, orchard);
    private final String plotId = UUID.randomUUID().toString();
    private final String owner = UUID.randomUUID().toString();
    private GetHarvestSettlementsByPlotIdQuery query;

    @BeforeEach
    void setUp() {
        query = new GetHarvestSettlementsByPlotIdQuery(plotId, owner);
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(owner)));
    }

    @Test
    void listsNoSettlementForAPlotThatHasNotClosedACampaignYet() {
        when(reports.findByPlotId(any())).thenReturn(Optional.empty());
        assertTrue(service.handle(query).success().orElseThrow().isEmpty());
    }

    @Test
    void listsTheSettlementsOfAReportNewestCampaignFirst() {
        when(reports.findByPlotId(any())).thenReturn(Optional.of(reportSettledIn(2024, 2025, 2026)));
        var years = service.handle(query).success().orElseThrow().stream()
                .map(settlement -> settlement.campaignYear().value())
                .toList();
        assertEquals(List.of(2026, 2025, 2024), years);
    }

    @Test
    void returnsNotFoundForAMissingOrInactivePlot() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.empty());
        assertEquals("PLOT_NOT_FOUND", service.handle(query).failure().orElseThrow().code());
        verifyNoInteractions(reports);
    }

    @Test
    void forbidsAProducerWhoDoesNotOwnThePlot() {
        when(orchard.findActivePlotOwner(any())).thenReturn(Optional.of(new UserId(UUID.randomUUID().toString())));
        assertEquals("PLOT_FORBIDDEN", service.handle(query).failure().orElseThrow().code());
        verifyNoInteractions(reports);
    }

    @Test
    void returnsTheSettlementOfTheRequestedCampaign() {
        when(reports.findByPlotId(any())).thenReturn(Optional.of(reportSettledIn(2024, 2025, 2026)));
        var detail = new GetHarvestSettlementByPlotIdAndCampaignYearQuery(plotId, 2025, owner);
        var settlement = service.handle(detail).success().orElseThrow();
        assertEquals(2025, settlement.campaignYear().value());
        assertEquals(100.0, settlement.totalHarvestWeight().kilograms());
        assertEquals(plotId, settlement.plotId().plotId());
    }

    @Test
    void returnsNotFoundForACampaignThatIsNotSettled() {
        when(reports.findByPlotId(any())).thenReturn(Optional.of(reportSettledIn(2026)));
        var detail = new GetHarvestSettlementByPlotIdAndCampaignYearQuery(plotId, 2029, owner);
        assertEquals("HARVESTSETTLEMENT_NOT_FOUND", service.handle(detail).failure().orElseThrow().code());
    }

    @Test
    void returnsNotFoundForACampaignOfAPlotThatHasNotClosedAnyYet() {
        when(reports.findByPlotId(any())).thenReturn(Optional.empty());
        var detail = new GetHarvestSettlementByPlotIdAndCampaignYearQuery(plotId, 2026, owner);
        assertEquals("HARVESTSETTLEMENT_NOT_FOUND", service.handle(detail).failure().orElseThrow().code());
    }

    @Test
    void rejectsANullQueryWithoutReadingTheReport() {
        assertEquals("VALIDATION_ERROR",
                service.handle((GetHarvestSettlementsByPlotIdQuery) null).failure().orElseThrow().code());
        assertEquals("VALIDATION_ERROR",
                service.handle((GetHarvestSettlementByPlotIdAndCampaignYearQuery) null).failure().orElseThrow().code());
        verifyNoInteractions(orchard, reports);
    }

    @Test
    void rejectsInvalidQueriesBeforeReachingTheService() {
        assertThrows(IllegalArgumentException.class, () -> new GetHarvestSettlementsByPlotIdQuery("bad", owner));
        assertThrows(IllegalArgumentException.class, () -> new GetHarvestSettlementsByPlotIdQuery(plotId, "bad"));
        assertThrows(IllegalArgumentException.class,
                () -> new GetHarvestSettlementByPlotIdAndCampaignYearQuery("bad", 2026, owner));
        assertThrows(IllegalArgumentException.class,
                () -> new GetHarvestSettlementByPlotIdAndCampaignYearQuery(plotId, 1999, owner));
        assertThrows(IllegalArgumentException.class,
                () -> new GetHarvestSettlementByPlotIdAndCampaignYearQuery(plotId, null, owner));
    }

    private AgronomicReport reportSettledIn(int... campaignYears) {
        var report = AgronomicReport.createForPlot(new PlotId(plotId), new UserId(owner));
        for (int campaignYear : campaignYears) {
            report.settleCampaign(new CampaignYear(campaignYear), new OliveWeight(100.0), new OliveWeight(0.0), null,
                    null, ThinningBalance.notRecorded(), new TreeMap<>(),
                    ReceiptNumber.of(new CampaignYear(campaignYear), campaignYear - 2000),
                    WeighingDate.of(java.time.LocalDate.of(campaignYear, 1, 15), CLOCK), null, null,
                    java.time.Clock.systemUTC());
        }
        report.clearDomainEvents();
        return report;
    }
}
