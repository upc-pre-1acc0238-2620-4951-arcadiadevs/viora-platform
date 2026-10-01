package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.entities;

import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.OliveVariety;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotName;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotStatus;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.TreeDensity;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.converters.PlotNamePersistenceConverter;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.converters.ProducerIdPersistenceConverter;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.converters.TreeDensityPersistenceConverter;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.embeddables.PlantationFramePersistenceEmbeddable;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.embeddables.PlotGeometryPersistenceEmbeddable;
import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * JPA entity mapping the {@code orchard.plots} relational database table.
 */
@Entity
@Table(name = "plots")
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
    private PlotGeometryPersistenceEmbeddable geometry;

    @Embedded
    private PlantationFramePersistenceEmbeddable frame;

    @Convert(converter = TreeDensityPersistenceConverter.class)
    @Column(name = "tree_density", nullable = false)
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
