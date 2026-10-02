package com.arcadiadevs.viora.platform.settlement.domain.repositories;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.AgronomicReport;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ReportId;

import java.util.Optional;

/** Domain port for agronomic reports and their settlements. */
public interface AgronomicReportRepository {

    Optional<AgronomicReport> findById(ReportId id);

    Optional<AgronomicReport> findByPlotId(PlotId plotId);

    /** Loads and locks the report of a plot until the caller's transaction completes. */
    Optional<AgronomicReport> findByPlotIdForUpdate(PlotId plotId);

    /** Persists the report and its new settlements atomically. */
    AgronomicReport save(AgronomicReport report);
}
