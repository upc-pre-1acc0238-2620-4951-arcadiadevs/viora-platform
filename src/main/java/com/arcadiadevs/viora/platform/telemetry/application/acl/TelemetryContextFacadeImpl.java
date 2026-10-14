package com.arcadiadevs.viora.platform.telemetry.application.acl;

import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.IoTDeviceRepository;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Implementation of the Telemetry ACL facade, telling other contexts how much climate data a plot holds without
 * exposing the readings themselves. It only reads what is stored: it never triggers the virtual node simulation.
 */
@Service
@Transactional(readOnly = true)
public class TelemetryContextFacadeImpl implements TelemetryContextFacade {

    private final IoTDeviceRepository deviceRepository;
    private final TelemetrySeriesRepository seriesRepository;

    public TelemetryContextFacadeImpl(IoTDeviceRepository deviceRepository, TelemetrySeriesRepository seriesRepository) {
        this.deviceRepository = deviceRepository;
        this.seriesRepository = seriesRepository;
    }

    @Override
    public TelemetryCoverage findTelemetryCoverage(String plotId, Instant from, Instant to) {
        final PlotId id;
        try {
            id = new PlotId(plotId);
        } catch (IllegalArgumentException exception) {
            return new TelemetryCoverage(0, 0);
        }
        int nodes = deviceRepository.findActiveByPlotId(id).size();
        if (from == null || to == null || !from.isBefore(to)) {
            return new TelemetryCoverage(nodes, 0);
        }
        int days = (int) seriesRepository.findReadingsByPlotIdAndDateRange(id, from, to).stream()
                .map(reading -> reading.timestamp().timestamp().atZone(ZoneOffset.UTC).toLocalDate())
                .distinct()
                .count();
        return new TelemetryCoverage(nodes, days);
    }
}
