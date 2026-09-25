package com.arcadiadevs.viora.platform.orchard.domain.model.aggregates;

import com.arcadiadevs.viora.platform.orchard.domain.model.events.PlotDelimited;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Aggregate Root representing an olive orchard plot (cuartel / predio).
 * Encapsulates cadastral polygon delimitation, planting density, and lifecycle status.
 *
 * <p>Invariant rules:
 * <ul>
 *   <li>Domain-assigned identity ({@link PlotId}).</li>
 *   <li>Immutable field access via {@link #snapshot()}.</li>
 *   <li>Zero setters or persistence framework annotations.</li>
 * </ul>
 * </p>
 */
public class Plot extends AbstractDomainAggregateRoot<Plot> {

    private final PlotId id;
    private final ProducerId producerId;
    private PlotName name;
    private OliveVariety variety;
    private PlotGeometry geometry;
    private PlantationFrame frame;
    private TreeDensity density;
    private LocalDate lastPruningDate;
    private PlotStatus status;
    private Long revision;

    private Plot(
            PlotId id,
            ProducerId producerId,
            PlotName name,
            OliveVariety variety,
            PlotGeometry geometry,
            PlantationFrame frame,
            TreeDensity density,
            LocalDate lastPruningDate,
            PlotStatus status,
            Long revision
    ) {
        this.id = id;
        this.producerId = producerId;
        this.name = name;
        this.variety = variety;
        this.geometry = geometry;
        this.frame = frame;
        this.density = density;
        this.lastPruningDate = lastPruningDate;
        this.status = status;
        this.revision = revision;
    }

    /**
     * Domain factory method that delimits and registers a new orchard plot.
     * Computes tree density automatically and registers the {@link PlotDelimited} domain event.
     *
     * @param producerId the owning/managing producer identifier
     * @param name       the descriptive plot name
     * @param variety    the botanical olive variety
     * @param geometry   the georeferenced cadastral geometry
     * @param frame      the planting grid frame
     * @return a new, consistently initialized Plot aggregate
     */
    public static Plot delimit(
            ProducerId producerId,
            PlotName name,
            OliveVariety variety,
            PlotGeometry geometry,
            PlantationFrame frame
    ) {
        var id = new PlotId();
        var density = TreeDensity.from(frame);
        var initialRevision = 0L;
        var plot = new Plot(
                id,
                producerId,
                name,
                variety,
                geometry,
                frame,
                density,
                null,
                PlotStatus.ACTIVE,
                initialRevision
        );

        plot.registerDomainEvent(new PlotDelimited(
                id.plotId(),
                producerId.producerId(),
                geometry.geoJson(),
                variety.name(),
                Instant.now()
        ));

        return plot;
    }

    /**
     * Factory method used exclusively by the persistence layer to reconstitute an existing Plot aggregate.
     *
     * @param snapshot the immutable snapshot of the plot
     * @return the reconstituted Plot instance
     */
    public static Plot reconstitute(PlotSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("plot.snapshot.null");
        }
        return new Plot(
                snapshot.id(),
                snapshot.producerId(),
                snapshot.name(),
                snapshot.variety(),
                snapshot.geometry(),
                snapshot.frame(),
                snapshot.density(),
                snapshot.lastPruningDate(),
                snapshot.status(),
                snapshot.revision()
        );
    }

    /**
     * Returns an immutable snapshot capturing the current state of this aggregate.
     *
     * @return a {@link PlotSnapshot} instance
     */
    public PlotSnapshot snapshot() {
        return new PlotSnapshot(
                id,
                producerId,
                name,
                variety,
                geometry,
                frame,
                density,
                lastPruningDate,
                status,
                revision
        );
    }
}
