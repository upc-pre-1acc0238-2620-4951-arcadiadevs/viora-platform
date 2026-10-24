package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.purge;

import com.arcadiadevs.viora.platform.orchard.domain.model.events.ProducerPlotsPurgedEvent;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.repositories.AgroclimaticIncidentPersistenceRepository;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.repositories.HourlyTelemetryReadingPersistenceRepository;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.repositories.IoTDevicePersistenceRepository;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.repositories.TelemetrySeriesPersistenceRepository;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Removes the readings, series, alerts (with their mitigation steps) and sensors of the plots of a deleted account.
 */
@Component
public class TelemetryAccountDataPurger {

    private final HourlyTelemetryReadingPersistenceRepository readings;
    private final TelemetrySeriesPersistenceRepository series;
    private final AgroclimaticIncidentPersistenceRepository incidents;
    private final IoTDevicePersistenceRepository devices;

    /**
     * Creates the purger.
     *
     * @param readings  the hourly readings table
     * @param series    the telemetry series table
     * @param incidents the agroclimatic incidents table
     * @param devices   the IoT devices table
     */
    public TelemetryAccountDataPurger(HourlyTelemetryReadingPersistenceRepository readings,
                                      TelemetrySeriesPersistenceRepository series,
                                      AgroclimaticIncidentPersistenceRepository incidents,
                                      IoTDevicePersistenceRepository devices) {
        this.readings = readings;
        this.series = series;
        this.incidents = incidents;
        this.devices = devices;
    }

    /**
     * Removes the telemetry data of the plots.
     *
     * @param event the plots of the deleted account
     */
    @EventListener
    public void on(ProducerPlotsPurgedEvent event) {
        if (event.plotIds().isEmpty()) {
            return;
        }
        var uuids = event.plotIds().stream().map(UUID::fromString).toList();
        var plotIds = event.plotIds().stream().map(PlotId::new).toList();
        readings.deleteAll(readings.findAllByPlotIdIn(uuids));
        series.deleteAll(series.findAllByPlotIdIn(uuids));
        incidents.deleteAll(incidents.findAllByPlotIdIn(plotIds));
        devices.deleteAll(devices.findAllByPlotIdIn(plotIds));
    }
}
