package com.arcadiadevs.viora.platform.phenology.application.acl;

import com.arcadiadevs.viora.platform.phenology.application.internal.chill.ChillSeasonReader;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather.HourlyTemperatureProvider;
import com.arcadiadevs.viora.platform.phenology.application.queryservices.HarvestRecordQueryService;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetChillAccumulationTrackerByPlotIdQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetHarvestRecordsByPlotIdQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.BbiAlternationCategory;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ChillSeasonEvaluation;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ThermalAnomalyStatus;
import com.arcadiadevs.viora.platform.phenology.domain.services.ChillSeasonEvaluator;
import com.arcadiadevs.viora.platform.phenology.domain.services.HoblynBbiCalculatorService;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Application-layer implementation of the {@link PhenologyContextFacade} ACL interface.
 */
@Service
@Transactional(readOnly = true)
public class PhenologyContextFacadeImpl implements PhenologyContextFacade {

    /** How long the evaluation of a winter in progress is reused: its weather changes once a day. */
    static final Duration CHILL_SEASON_TTL = Duration.ofMinutes(30);

    /** How long the evaluation of a finished winter is reused; late weather corrections still get in. */
    static final Duration FINISHED_SEASON_TTL = Duration.ofHours(24);

    private static final Logger LOG = LoggerFactory.getLogger(PhenologyContextFacadeImpl.class);

    private final HarvestRecordQueryService harvestRecordQueryService;
    private final ExternalOrchardService externalOrchardService;
    private final HourlyTemperatureProvider temperatureProvider;
    private final ChillSeasonReader chillSeasonReader;
    private final Clock clock;

    /**
     * Evaluated seasons by plot and winter. Only plots with a location get an entry, and expired entries are dropped
     * on every store, so the map holds at most the real plots asked for within a day.
     */
    private final ConcurrentHashMap<SeasonKey, CachedSeason> seasons = new ConcurrentHashMap<>();

    /** One lock per season being read, so concurrent misses fetch the weather once. */
    private final ConcurrentHashMap<SeasonKey, ReentrantLock> seasonLocks = new ConcurrentHashMap<>();

    public PhenologyContextFacadeImpl(HarvestRecordQueryService harvestRecordQueryService,
            ExternalOrchardService externalOrchardService, HourlyTemperatureProvider temperatureProvider,
            Clock clock) {
        this.harvestRecordQueryService = harvestRecordQueryService;
        this.externalOrchardService = externalOrchardService;
        this.temperatureProvider = temperatureProvider;
        this.chillSeasonReader = new ChillSeasonReader(temperatureProvider);
        this.clock = clock;
    }

    @Override
    public SortedMap<Integer, Double> findHistoricalYields(String plotId) {
        if (plotId == null || plotId.isBlank()) {
            return Collections.emptySortedMap();
        }
        try {
            var result = harvestRecordQueryService.handle(new GetHarvestRecordsByPlotIdQuery(plotId, null));
            var yields = new TreeMap<Integer, Double>();
            result.success().ifPresent(entries -> entries.forEach(entry ->
                    yields.put(entry.campaignYear().value(), entry.harvestYield().totalKg())));
            return Collections.unmodifiableSortedMap(yields);
        } catch (IllegalArgumentException ex) {
            // Malformed UUID or VO validation failure means plot has no history
            return Collections.emptySortedMap();
        }
    }

    @Override
    public SortedMap<LocalDate, Double> findDailyMaxTemperatures(String plotId, LocalDate from, LocalDate to) {
        if (plotId == null || plotId.isBlank() || from == null || to == null || to.isBefore(from)) {
            return Collections.emptySortedMap();
        }
        try {
            var centroid = externalOrchardService.findPlotCentroid(new PlotId(plotId));
            if (centroid.isEmpty()) {
                return Collections.emptySortedMap();
            }
            var hours = temperatureProvider.fetchHourlyTemperatures(centroid.get()[0], centroid.get()[1], from, to);
            var maxima = new TreeMap<LocalDate, Double>();
            hours.orElse(List.of()).forEach(hour -> maxima.merge(hour.hour().toLocalDate(), hour.celsius(), Math::max));
            return Collections.unmodifiableSortedMap(maxima);
        } catch (IllegalArgumentException ex) {
            return Collections.emptySortedMap();
        }
    }

    @Override
    public int currentChillSeasonYear() {
        return ChillSeasonEvaluator.seasonYearFor(today());
    }

    /**
     * Runs outside a transaction: the weather call takes seconds and must not hold a database connection.
     */
    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Optional<PlotChillSeason> findChillSeason(String plotId, int seasonYear) {
        var today = today();
        if (plotId == null || plotId.isBlank() || seasonYear > ChillSeasonEvaluator.seasonYearFor(today)) {
            return Optional.empty();
        }
        try {
            var id = new PlotId(plotId);
            var key = new SeasonKey(id.plotId(), seasonYear);
            var cached = fresh(key);
            if (cached.isPresent()) {
                return cached;
            }
            var centroid = externalOrchardService.findPlotCentroid(id);
            if (centroid.isEmpty()) {
                return Optional.empty();
            }
            // A lock rather than synchronized, so a virtual thread waiting on the weather does not pin its carrier
            var lock = seasonLocks.computeIfAbsent(key, ignored -> new ReentrantLock());
            lock.lock();
            try {
                // Another request may have read it while this one waited
                cached = fresh(key);
                if (cached.isPresent()) {
                    return cached;
                }
                var end = ChillSeasonEvaluator.seasonEnd(seasonYear);
                boolean finished = today.isAfter(end);
                var evaluation = finished
                        ? chillSeasonReader.evaluateWholeSeason(centroid.get()[0], centroid.get()[1], seasonYear)
                        : chillSeasonReader.evaluate(centroid.get()[0], centroid.get()[1], seasonYear, today);
                if (evaluation.isEmpty()) {
                    // Not cached: the next question tries again
                    return Optional.empty();
                }
                var season = toPlotChillSeason(evaluation.get());
                store(key, season, finished ? FINISHED_SEASON_TTL : CHILL_SEASON_TTL);
                return Optional.of(season);
            } finally {
                lock.unlock();
            }
        } catch (RuntimeException ex) {
            // A malformed id, or Orchard or the weather provider failing, means no season to show
            LOG.warn("Chill season {} of plot {} could not be read: {}", seasonYear, plotId, ex.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public Optional<PlotBearingIndex> findBiennialBearingIndex(String plotId) {
        if (plotId == null || plotId.isBlank()) {
            return Optional.empty();
        }
        final GetChillAccumulationTrackerByPlotIdQuery query;
        try {
            query = new GetChillAccumulationTrackerByPlotIdQuery(plotId);
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
        // Same classification the BBI metric answers, so both read the same band
        return harvestRecordQueryService.handle(query).flatMap(tracker -> {
            var snapshot = tracker.snapshot();
            if (snapshot.calculatedBbi() == null || snapshot.calculatedBbi().value() == null) {
                return Optional.<PlotBearingIndex>empty();
            }
            var category = HoblynBbiCalculatorService.classifyAlternation(
                    snapshot.calculatedBbi(), snapshot.harvestHistory().size());
            if (category == BbiAlternationCategory.INSUFFICIENT_DATA) {
                return Optional.empty();
            }
            return Optional.of(new PlotBearingIndex(snapshot.calculatedBbi().value(), category.name()));
        });
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(HourlyTemperatureProvider.PLOT_TIME_ZONE));
    }

    private Optional<PlotChillSeason> fresh(SeasonKey key) {
        var cached = seasons.get(key);
        return cached != null && cached.expiresAt().isAfter(clock.instant())
                ? Optional.of(cached.season())
                : Optional.empty();
    }

    private void store(SeasonKey key, PlotChillSeason season, Duration ttl) {
        var now = clock.instant();
        seasons.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
        seasons.put(key, new CachedSeason(season, now.plus(ttl)));
    }

    private static PlotChillSeason toPlotChillSeason(ChillSeasonEvaluation evaluation) {
        var daily = new TreeMap<LocalDate, Double>();
        evaluation.dailyCurve().forEach(point -> daily.put(point.date(), point.accumulatedPortions()));
        return new PlotChillSeason(evaluation.seasonYear(), evaluation.accumulatedPortions(),
                evaluation.thresholdPortions(), evaluation.seasonState().name(), evaluation.projectionStatus().name(),
                evaluation.projectedCompletionDate(), evaluation.completionDate(), evaluation.seasonEnd(),
                evaluation.evaluatedThrough(), evaluation.daysAbove24Celsius(),
                ThermalAnomalyStatus.WARM_DAY_MAX_TEMPERATURE, daily);
    }

    private record SeasonKey(String plotId, int seasonYear) {
    }

    private record CachedSeason(PlotChillSeason season, Instant expiresAt) {
    }
}
