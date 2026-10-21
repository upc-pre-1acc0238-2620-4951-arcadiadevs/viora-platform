package com.arcadiadevs.viora.platform.shared.domain;

import org.junit.jupiter.api.Test;
import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import static org.junit.jupiter.api.Assertions.*;

class DomainAggregateEventsTest {
    private static class Aggregate extends AbstractDomainAggregateRoot<Aggregate> {
        void record(Object event) { registerDomainEvent(event); }
    }

    @Test
    void exposesAnImmutableSnapshotAndClearsOnlyThePendingCollection() {
        var aggregate = new Aggregate();
        var event = new Object();
        aggregate.record(event);
        var snapshot = aggregate.domainEvents();
        assertEquals(1, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
        aggregate.clearDomainEvents();
        assertTrue(aggregate.domainEvents().isEmpty());
        assertEquals(1, snapshot.size());
    }

    @Test
    void rejectsMissingEventWithAnInternationalizedKey() {
        var error = assertThrows(IllegalArgumentException.class, () -> new Aggregate().record(null));
        assertEquals("shared.domain.event.null", error.getMessage());
    }
}
