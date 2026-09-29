package com.arcadiadevs.viora.platform.telemetry.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.TelemetryQueryService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetTelemetrySeriesByPlotIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementation of {@link TelemetryQueryService} orchestrating telemetry readings queries.
 */
@Service
@Transactional(readOnly = true)
public class TelemetryQueryServiceImpl implements TelemetryQueryService {

    private final TelemetrySeriesRepository telemetrySeriesRepository;
    private final ExternalOrchardService externalOrchardService;

    /**
     * Constructs the TelemetryQueryServiceImpl injecting required dependencies.
     *
     * @param telemetrySeriesRepository the domain repository port for telemetry series
     * @param externalOrchardService    the outbound ACL service for orchard validations
     */
    public TelemetryQueryServiceImpl(
            TelemetrySeriesRepository telemetrySeriesRepository,
            @Qualifier("telemetryExternalOrchardService") ExternalOrchardService externalOrchardService
    ) {
        if (telemetrySeriesRepository == null) {
            throw new IllegalArgumentException("telemetry.series_repository.null");
        }
        if (externalOrchardService == null) {
            throw new IllegalArgumentException("device.external_orchard_service.null");
        }
        this.telemetrySeriesRepository = telemetrySeriesRepository;
        this.externalOrchardService = externalOrchardService;
    }

    @Override
    public Result<List<HourlyTelemetryReadingSnapshot>, ApplicationError> handle(GetTelemetrySeriesByPlotIdQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query.null");
        }

        var plotId = query.plotId();
        if (!externalOrchardService.existsActivePlot(plotId)) {
            return Result.failure(ApplicationError.notFound("Plot", plotId.plotId()));
        }

        var readings = telemetrySeriesRepository.findReadingsByPlotIdAndDateRange(
                plotId,
                query.startDate(),
                query.endDate()
        );

        return Result.success(readings);
    }
}
