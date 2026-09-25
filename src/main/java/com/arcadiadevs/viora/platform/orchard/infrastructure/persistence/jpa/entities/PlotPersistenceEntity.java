package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.converters.PlotNamePersistenceConverter;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.converters.ProducerIdPersistenceConverter;
import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * JPA entity mapping the {@code orchard.plots} relational database table.
 * Encapsulates domain Value Objects via {@link Embedded}, {@link Convert}, and {@link Enumerated}.
 */
@Entity
@Table(name = "plots", schema = "orchard")
@Getter
@Setter
@NoArgsConstructor
public class PlotPersistenceEntity extends AuditableAbstractPersistenceEntity {

    @Convert(converter = ProducerIdPersistenceConverter.class)
    @Column(name = "producer_id", nullable = false)
    private ProducerId producerId;

    @Convert(converter = PlotNamePersistenceConverter.class)
    @Column(name = "name", nullable = false, length = 100)
    private PlotName name;

    @Enumerated(EnumType.STRING)
    @Column(name = "variety", nullable = false, length = 50)
    private OliveVariety variety;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "geoJson", column = @Column(name = "polygon_geojson", nullable = false, columnDefinition = "text")),
            @AttributeOverride(name = "areaHa", column = @Column(name = "area_ha", nullable = false))
    })
    private PlotGeometry geometry;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "rowSpacingM", column = @Column(name = "row_spacing_m", nullable = false)),
            @AttributeOverride(name = "treeSpacingM", column = @Column(name = "tree_spacing_m", nullable = false))
    })
    private PlantationFrame frame;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "treesPerHectare", column = @Column(name = "tree_density", nullable = false))
    })
    private TreeDensity density;

    @Column(name = "last_pruning_date")
    private LocalDate lastPruningDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PlotStatus status;

    @Version
    @Column(name = "revision", nullable = false)
    private Long revision;
}
