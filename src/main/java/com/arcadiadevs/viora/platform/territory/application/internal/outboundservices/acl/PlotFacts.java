package com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.GeoPoint;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;

/**
 * What Orchard tells about an active plot.
 *
 * @param plotId  the plot
 * @param name    its name
 * @param variety its olive variety
 * @param areaHa  its area in hectares
 * @param center  its centroid; null if it cannot be computed
 * @param geoJson its polygon as the producer drew it
 */
public record PlotFacts(PlotId plotId, String name, String variety, Double areaHa, GeoPoint center, String geoJson) {
}
