package com.arcadiadevs.viora.platform.telemetry.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.application.internal.commandservices.VirtualNodeTelemetrySimulator;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.TelemetryQueryService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetTelemetrySeriesByPlotIdQuery;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of {@link TelemetryQueryService} orchestrating telemetry readings queries.
 *
 * <p>Before reading, the {@link VirtualNodeTelemetrySimulator} fills the hours of the last week that
 * have no reading. It is not wrapped in one read-only transaction so the call to Open-Meteo does not
 * hold a database connection; the simulator writes in its own transaction.</p>
 */
@Service
public class TelemetryQueryServiceImpl implements TelemetryQueryService {

    private final TelemetrySeriesRepository telemetrySeriesRepository;
    private final ExternalOrchardService externalOrchardService;
    private final VirtualNodeTelemetrySimulator virtualNodeTelemetrySimulator;

    /**
     * Constructs the TelemetryQueryServiceImpl injecting required dependencies.
     *
     * @param telemetrySeriesRepository the domain repository port for telemetry series
     * @param externalOrchardService    the outbound ACL service for orchard validations
     * @param virtualNodeTelemetrySimulator fills the missing hours with the observed weather
     */
    public TelemetryQueryServiceImpl(
            TelemetrySeriesRepository telemetrySeriesRepository,
            @Qualifier("telemetryExternalOrchardService") ExternalOrchardService externalOrchardService,
            VirtualNodeTelemetrySimulator virtualNodeTelemetrySimulator
    ) {
        if (telemetrySeriesRepository == null) {
            throw new IllegalArgumentException("telemetry.series_repository.null");
        }
        if (externalOrchardService == null) {
            throw new IllegalArgumentException("device.external_orchard_service.null");
        }
        this.telemetrySeriesRepository = telemetrySeriesRepository;
        this.externalOrchardService = externalOrchardService;
        this.virtualNodeTelemetrySimulator = virtualNodeTelemetrySimulator;
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

        virtualNodeTelemetrySimulator.fillMissingHours(plotId);

        var readings = telemetrySeriesRepository.findReadingsByPlotIdAndDateRange(
                plotId,
                query.startDate(),
                query.endDate()
        );

        return Result.success(readings);
    }
}
