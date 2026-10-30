package com.arcadiadevs.viora.platform.phenology.application.acl;

import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.phenology.application.internal.outboundservices.weather.HourlyTemperatureProvider;
import com.arcadiadevs.viora.platform.phenology.application.queryservices.HarvestRecordQueryService;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetChillAccumulationTrackerByPlotIdQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetHarvestRecordsByPlotIdQuery;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.BbiAlternationCategory;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.phenology.domain.services.HoblynBbiCalculatorService;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Application-layer implementation of the {@link PhenologyContextFacade} ACL interface.
 */
@Service
@Transactional(readOnly = true)
public class PhenologyContextFacadeImpl implements PhenologyContextFacade {

    private final HarvestRecordQueryService harvestRecordQueryService;
    private final ExternalOrchardService externalOrchardService;
    private final HourlyTemperatureProvider temperatureProvider;

    public PhenologyContextFacadeImpl(HarvestRecordQueryService harvestRecordQueryService,
            ExternalOrchardService externalOrchardService, HourlyTemperatureProvider temperatureProvider) {
        this.harvestRecordQueryService = harvestRecordQueryService;
        this.externalOrchardService = externalOrchardService;
        this.temperatureProvider = temperatureProvider;
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
            var category = HoblynBbiCalculatorService.classifyAlternation(
                    snapshot.calculatedBbi(), snapshot.harvestHistory().size());
            if (category == BbiAlternationCategory.INSUFFICIENT_DATA) {
                return Optional.empty();
            }
            return Optional.of(new PlotBearingIndex(snapshot.calculatedBbi().value(), category.name()));
        });
    }
}
