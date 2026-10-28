package com.arcadiadevs.viora.platform.telemetry.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.telemetry.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.telemetry.application.queryservices.WeatherForecastQueryService;
import com.arcadiadevs.viora.platform.telemetry.domain.model.queries.GetWeatherForecastByPlotIdQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

/**
 * Reads the forecast of every active plot, so a frost warning is raised even when nobody opens the climate screen.
 *
 * <p>Reading the forecast publishes it, and the listener of that event raises the frost incident when the minimum
 * reaches the threshold. Before this job existed, the warning only appeared after a producer opened the forecast.</p>
 */
@Service
public class FrostForecastMonitor {

    private static final Logger LOG = LoggerFactory.getLogger(FrostForecastMonitor.class);

    private final ExternalOrchardService orchard;
    private final WeatherForecastQueryService forecastQueryService;

    public FrostForecastMonitor(@Qualifier("telemetryExternalOrchardService") ExternalOrchardService orchard,
            WeatherForecastQueryService forecastQueryService) {
        this.orchard = orchard;
        this.forecastQueryService = forecastQueryService;
    }

    /**
     * Reads the forecast of every active plot.
     *
     * @return the plots whose forecast could be read
     */
    public int checkAllPlots() {
        int evaluated = 0;
        for (var plotId : orchard.findAllActivePlotIds()) {
            try {
                if (forecastQueryService.handle(new GetWeatherForecastByPlotIdQuery(plotId))
                        instanceof Result.Success<?, ?>) {
                    evaluated++;
                }
            } catch (RuntimeException exception) {
                // One plot without weather must not stop the others
                LOG.warn("Frost forecast of plot {} could not be evaluated: {}", plotId.plotId(),
                        exception.getMessage());
            }
        }
        return evaluated;
    }
}
