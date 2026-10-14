package com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.SortedMap;

/**
 * Outbound ACL used by Telemetry to read the daily maxima the warm winter rule was evaluated on, so the alert shows
 * the same days that raised it.
 */
@Service("telemetryExternalPhenologyService")
public class ExternalPhenologyService {

    private final PhenologyContextFacade phenologyContextFacade;

    public ExternalPhenologyService(PhenologyContextFacade phenologyContextFacade) {
        this.phenologyContextFacade = phenologyContextFacade;
    }

    /**
     * Daily maximum temperature at a plot.
     *
     * @param plotId the plot
     * @param from   first day, inclusive
     * @param to     last day, inclusive
     * @return maximum per local day, ascending; empty without location or weather
     */
    public SortedMap<LocalDate, Double> findDailyMaxTemperatures(PlotId plotId, LocalDate from, LocalDate to) {
        if (plotId == null) {
            return Collections.emptySortedMap();
        }
        return phenologyContextFacade.findDailyMaxTemperatures(plotId.plotId(), from, to);
    }
}
