package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.assemblers;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.ReceiptCounter;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.entities.ReceiptCounterPersistenceEntity;

import java.util.UUID;

/** Maps receipt counters between the domain and JPA, in both directions. */
public final class ReceiptCounterPersistenceAssembler {

    private ReceiptCounterPersistenceAssembler() {
    }

    /**
     * Maps a counter to its entity.
     *
     * @param counter counter to persist
     * @return a new entity carrying the counter state
     */
    public static ReceiptCounterPersistenceEntity toEntity(ReceiptCounter counter) {
        var entity = new ReceiptCounterPersistenceEntity();
        entity.setId(UUID.randomUUID());
        entity.setProducerId(UUID.fromString(counter.producerId().userId()));
        entity.setCampaignYear(counter.campaignYear().value());
        entity.setLastSequence(counter.lastSequence());
        return entity;
    }

    /**
     * Maps a counter entity to the domain.
     *
     * @param entity the persisted counter
     * @return the reconstituted counter
     */
    public static ReceiptCounter toDomain(ReceiptCounterPersistenceEntity entity) {
        return ReceiptCounter.reconstitute(new UserId(entity.getProducerId().toString()),
                new CampaignYear(entity.getCampaignYear()), entity.getLastSequence());
    }
}
