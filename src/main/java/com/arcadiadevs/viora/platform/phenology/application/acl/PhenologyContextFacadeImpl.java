package com.arcadiadevs.viora.platform.phenology.application.acl;

import com.arcadiadevs.viora.platform.phenology.application.queryservices.HarvestRecordQueryService;
import com.arcadiadevs.viora.platform.phenology.domain.model.queries.GetHarvestRecordsByPlotIdQuery;
import com.arcadiadevs.viora.platform.phenology.interfaces.acl.PhenologyContextFacade;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Application-layer implementation of the {@link PhenologyContextFacade} ACL interface.
 */
@Service
@Transactional(readOnly = true)
public class PhenologyContextFacadeImpl implements PhenologyContextFacade {

    private final HarvestRecordQueryService harvestRecordQueryService;

    public PhenologyContextFacadeImpl(HarvestRecordQueryService harvestRecordQueryService) {
        this.harvestRecordQueryService = harvestRecordQueryService;
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
}
