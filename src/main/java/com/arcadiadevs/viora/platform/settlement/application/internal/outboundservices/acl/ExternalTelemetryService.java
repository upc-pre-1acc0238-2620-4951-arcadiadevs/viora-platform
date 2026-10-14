package com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.interfaces.acl.TelemetryContextFacade;
import org.springframework.stereotype.Service;

import java.time.Instant;

/** Outbound ACL used by Settlement to state how much climate data backs a campaign in its dossier. */
@Service("settlementExternalTelemetryService")
public class ExternalTelemetryService {

    private final TelemetryContextFacade telemetryContextFacade;

    public ExternalTelemetryService(TelemetryContextFacade telemetryContextFacade) {
        this.telemetryContextFacade = telemetryContextFacade;
    }

    /**
     * Summarizes the climate data of a plot over a period.
     *
     * @param plotId plot to read
     * @param from   start of the period
     * @param to     end of the period
     * @return linked nodes and days with readings
     */
    public ClimateCoverage findClimateCoverage(PlotId plotId, Instant from, Instant to) {
        if (plotId == null) {
            return new ClimateCoverage(0, 0);
        }
        var coverage = telemetryContextFacade.findTelemetryCoverage(plotId.plotId(), from, to);
        return new ClimateCoverage(coverage.linkedNodes(), coverage.daysWithReadings());
    }

    /**
     * Climate data behind a campaign.
     *
     * @param linkedNodes      nodes linked to the plot
     * @param daysWithReadings days with at least one reading
     */
    public record ClimateCoverage(int linkedNodes, int daysWithReadings) {
    }
}
