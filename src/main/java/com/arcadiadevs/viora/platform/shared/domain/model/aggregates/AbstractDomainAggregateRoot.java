package com.arcadiadevs.viora.platform.shared.domain.model.aggregates;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Framework-free event collection for aggregate roots.
 * Application services publish and clear events after coordinating persistence.
 * @param <T> concrete aggregate root type
 */
public abstract class AbstractDomainAggregateRoot<T extends AbstractDomainAggregateRoot<T>> {
    private final List<Object> events = new ArrayList<>();

    /**
     * Records a domain fact without publishing it or depending on a framework.
     * @param event domain event
     * @throws IllegalArgumentException if the event is missing
     */
    protected void registerDomainEvent(Object event) {
        if (event == null) {
            throw new IllegalArgumentException("shared.domain.event.null");
        }
        events.add(event);
    }

    /** @return immutable snapshot of events awaiting publication */
    public Collection<Object> domainEvents() {
        return List.copyOf(events);
    }

    /** Clears already processed events; publication remains an application responsibility. */
    public void clearDomainEvents() {
        events.clear();
    }
}
