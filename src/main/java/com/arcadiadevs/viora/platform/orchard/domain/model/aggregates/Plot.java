package com.arcadiadevs.viora.platform.orchard.domain.model.aggregates;

import com.arcadiadevs.viora.platform.orchard.domain.exceptions.PlotAlreadyRemovedException;
import com.arcadiadevs.viora.platform.orchard.domain.exceptions.PlotRevisionMismatchException;
import com.arcadiadevs.viora.platform.orchard.domain.model.events.PlotDelimitedEvent;
import com.arcadiadevs.viora.platform.orchard.domain.model.events.PlotRemovedEvent;
import com.arcadiadevs.viora.platform.orchard.domain.model.events.PlotUpdatedEvent;
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

        plot.registerDomainEvent(new PlotDelimitedEvent(
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
     * Integrally updates the plot agronomic and cadastral boundaries while enforcing optimistic locking.
     * Recalculates planting tree density and dispatches {@link PlotUpdatedEvent}.
     *
     * @param newName          the new plot name
     * @param newGeometry      the updated cadastral geometry
     * @param newFrame         the updated plantation frame
     * @param newPruningDate   the updated date of last pruning
     * @param expectedRevision the revision expected by the caller from If-Match header
     * @throws PlotRevisionMismatchException if expectedRevision does not match current aggregate revision
     */
    public void update(
            PlotName newName,
            PlotGeometry newGeometry,
            PlantationFrame newFrame,
            LocalDate newPruningDate,
            long expectedRevision
    ) {
        if (this.revision == null || this.revision != expectedRevision) {
            long currentRev = this.revision == null ? 0L : this.revision;
            throw new PlotRevisionMismatchException(this.id, currentRev, expectedRevision);
        }
        this.name = newName;
        this.geometry = newGeometry;
        this.frame = newFrame;
        this.density = TreeDensity.from(newFrame);
        this.lastPruningDate = newPruningDate;
        this.revision = this.revision + 1L;

        registerDomainEvent(new PlotUpdatedEvent(
                this.id.plotId(),
                this.producerId.producerId(),
                this.name.value(),
                this.geometry.geoJson(),
                this.geometry.areaHa(),
                this.revision,
                Instant.now()
        ));
    }

    /**
     * Executes sovereign soft deletion of the plot, changing status to REMOVED_SOFT_DELETE
     * and dispatching a {@link PlotRemovedEvent} domain event.
     *
     * @param reason optional justification or cause for removing the plot
     * @throws PlotAlreadyRemovedException if the plot is already in soft-deleted state
     */
    public void remove(String reason) {
        if (this.status == PlotStatus.REMOVED_SOFT_DELETE) {
            throw new PlotAlreadyRemovedException(this.id);
        }
        this.status = PlotStatus.REMOVED_SOFT_DELETE;
        this.revision = (this.revision == null ? 0L : this.revision) + 1L;

        var effectiveReason = (reason != null && !reason.isBlank()) ? reason : "Manual plot removal";

        registerDomainEvent(new PlotRemovedEvent(
                this.id.plotId(),
                this.producerId.producerId(),
                effectiveReason,
                Instant.now()
        ));
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
