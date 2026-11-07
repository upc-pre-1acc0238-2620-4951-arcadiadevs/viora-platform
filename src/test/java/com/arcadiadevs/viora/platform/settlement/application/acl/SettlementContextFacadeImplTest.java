package com.arcadiadevs.viora.platform.settlement.application.acl;

import com.arcadiadevs.viora.platform.settlement.application.queryservices.ProvisionalDossierQueryService;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReportSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.interfaces.acl.SettlementContextFacade.SettledCaliber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SettlementContextFacade: the settled caliber of each campaign of a plot")
class SettlementContextFacadeImplTest {

    private static final String PLOT = "550e8400-e29b-41d4-a716-446655440001";

    private final AgronomicReportRepository reports = mock(AgronomicReportRepository.class);
    private final CommercialSizeGradeQueryService grades = mock(CommercialSizeGradeQueryService.class);
    private final SettlementContextFacadeImpl facade = new SettlementContextFacadeImpl(reports, grades,
            mock(ProvisionalDossierQueryService.class));

    @Test
    @DisplayName("one caliber per campaign, oldest first, named with its grade; the latest settlement of a year wins")
    void calibersByCampaign() {
        when(grades.gradeOf(180.4)).thenReturn(Optional.of("171/190"));
        when(grades.gradeOf(205.6)).thenReturn(Optional.of("201/220"));
        when(grades.gradeOf(400.0)).thenReturn(Optional.empty());
        givenSettlements(
                settlement(2025, 150.0, "2026-01-10T10:00:00Z"),
                settlement(2024, 205.6, "2025-01-10T10:00:00Z"),
                // A second settlement of 2025, written later, is the one that counts
                settlement(2025, 180.4, "2026-02-10T10:00:00Z"),
                settlement(2023, 400.0, "2024-01-10T10:00:00Z"),
                // Settled without a caliber: not listed
                settlement(2022, null, "2023-01-10T10:00:00Z"));

        assertThat(facade.findSettledCalibers(PLOT)).containsExactly(
                new SettledCaliber(2023, 400.0, null),
                new SettledCaliber(2024, 205.6, "201/220"),
                new SettledCaliber(2025, 180.4, "171/190"));
    }

    @Test
    @DisplayName("a plot without report, or a malformed identifier, has no calibers")
    void nothingToList() {
        when(reports.findByPlotId(new PlotId(PLOT))).thenReturn(Optional.empty());

        assertThat(facade.findSettledCalibers(PLOT)).isEmpty();
        assertThat(facade.findSettledCalibers("not-a-uuid")).isEmpty();
        assertThat(facade.findSettledCalibers(null)).isEmpty();
        verify(grades, never()).gradeOf(anyDouble());
        // Only the well-formed identifier reaches the repository
        verify(reports, times(1)).findByPlotId(any(PlotId.class));
    }

    private void givenSettlements(HarvestSettlementSnapshot... settlements) {
        var report = mock(AgronomicReport.class);
        var snapshot = mock(AgronomicReportSnapshot.class);
        when(snapshot.settlements()).thenReturn(List.of(settlements));
        when(report.snapshot()).thenReturn(snapshot);
        when(reports.findByPlotId(new PlotId(PLOT))).thenReturn(Optional.of(report));
    }

    private static HarvestSettlementSnapshot settlement(int year, Double fruitsPerKg, String settledAt) {
        var settlement = mock(HarvestSettlementSnapshot.class);
        when(settlement.campaignYear()).thenReturn(new CampaignYear(year));
        when(settlement.commercialFruitsPerKg()).thenReturn(fruitsPerKg);
        when(settlement.settledAt()).thenReturn(Instant.parse(settledAt));
        return settlement;
    }
}
