package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.HarvestSettlementSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.IdempotencyKey;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ReportId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.SettledHarvestRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.assemblers.AgronomicReportPersistenceAssembler;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.SettledHarvestPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/** JPA adapter that reads a settlement on its own, by producer and idempotency key, and the highest receipt sequence of a producer. */
@Repository
public class SettledHarvestRepositoryImpl implements SettledHarvestRepository {

    private final SettledHarvestPersistenceRepository persistenceRepository;

    /**
     * Constructs the adapter.
     *
     * @param persistenceRepository Spring Data repository of the settlements
     */
    public SettledHarvestRepositoryImpl(SettledHarvestPersistenceRepository persistenceRepository) {
        this.persistenceRepository = persistenceRepository;
    }

    @Override
    public Optional<HarvestSettlementSnapshot> findByProducerIdAndIdempotencyKey(UserId producerId,
            IdempotencyKey idempotencyKey) {
        if (producerId == null || idempotencyKey == null || !idempotencyKey.isPresent()) {
            return Optional.empty();
        }
        return persistenceRepository
                .findByProducerIdAndIdempotencyKey(UUID.fromString(producerId.userId()), idempotencyKey.value())
                .map(entity -> AgronomicReportPersistenceAssembler.toSnapshot(
                        new ReportId(entity.getReport().getId().toString()),
                        new PlotId(entity.getReport().getPlotId().toString()), entity));
    }

    @Override
    public int findHighestReceiptSequence(UserId producerId, CampaignYear campaignYear) {
        var highest = persistenceRepository.findHighestReceiptSequence(UUID.fromString(producerId.userId()),
                campaignYear.value());
        return highest == null ? 0 : highest;
    }
}
