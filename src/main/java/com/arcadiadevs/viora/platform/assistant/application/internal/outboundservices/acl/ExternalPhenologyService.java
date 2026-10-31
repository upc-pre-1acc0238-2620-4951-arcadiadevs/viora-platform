package com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ChillFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.SortedMap;

/**
 * Outbound ACL service used by the assistant to read the harvest history, alternate bearing and winter chill of a
 * plot.
 */
@Service("assistantExternalPhenologyService")
public class ExternalPhenologyService {

    private final PhenologyContextFacade phenologyContextFacade;

    public ExternalPhenologyService(PhenologyContextFacade phenologyContextFacade) {
        this.phenologyContextFacade = phenologyContextFacade;
    }

    /**
     * Resolves the latest Biennial Bearing Index of the plot.
     *
     * @param plotId the assistant PlotId value object
     * @return the index, or empty while there are too few campaigns to calculate it
     */
    public Optional<Double> findBiennialBearingIndex(PlotId plotId) {
        return phenologyContextFacade.findBiennialBearingIndex(plotId.plotId())
                .map(PhenologyContextFacade.PlotBearingIndex::value);
    }

    /**
     * Reads the kilograms harvested per campaign.
     *
     * @param plotId the assistant PlotId value object
     * @return kilograms per campaign year, ascending; empty without a history
     */
    public SortedMap<Integer, Double> findHistoricalYields(PlotId plotId) {
        return phenologyContextFacade.findHistoricalYields(plotId.plotId());
    }

    /**
     * Resolves the latest Biennial Bearing Index with its band.
     *
     * @param plotId the assistant PlotId value object
     * @return the index and band, or empty while there are too few campaigns to classify it
     */
    public Optional<PhenologyContextFacade.PlotBearingIndex> findBearing(PlotId plotId) {
        return phenologyContextFacade.findBiennialBearingIndex(plotId.plotId());
    }

    /**
     * Reads the winter chill of the plot: the season the chill metric describes today and the one before it.
     *
     * @param plotId the assistant PlotId value object
     * @return the chill figures; a season Phenology cannot evaluate (no location, weather unavailable) is null
     */
    public ChillFacts findChillSeasons(PlotId plotId) {
        int seasonYear = phenologyContextFacade.currentChillSeasonYear();
        var current = phenologyContextFacade.findChillSeason(plotId.plotId(), seasonYear).map(this::toSeason);
        if (current.isEmpty()) {
            // Without the current winter the screen has nothing to compare, so last winter is not read
            return new ChillFacts(null, null);
        }
        var lastWinter = phenologyContextFacade.findChillSeason(plotId.plotId(), seasonYear - 1).map(this::toSeason);
        return new ChillFacts(current.get(), lastWinter.orElse(null));
    }

    private ChillFacts.Season toSeason(PhenologyContextFacade.PlotChillSeason season) {
        return new ChillFacts.Season(season.seasonYear(), season.accumulatedPortions(), season.thresholdPortions(),
                season.seasonState(), season.projectionStatus(), season.projectedCompletionDate(),
                season.completionDate(), season.seasonEnd(), season.evaluatedThrough(), season.daysAbove24Celsius(),
                season.warmDayCelsius(), season.dailyPortions());
    }
}
