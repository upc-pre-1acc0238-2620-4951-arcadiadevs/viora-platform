package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * JPA Embeddable mapping cadastral geometry and surface area.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class PlotGeometryPersistenceEmbeddable {

    @Column(name = "polygon_geojson", nullable = false, columnDefinition = "text")
    private String polygonGeoJson;

    @Column(name = "area_ha", nullable = false)
    private Double areaHa;

    public PlotGeometryPersistenceEmbeddable(String polygonGeoJson, Double areaHa) {
        this.polygonGeoJson = polygonGeoJson;
        this.areaHa = areaHa;
    }
}
