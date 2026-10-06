package com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.ReceiptCounter;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.ReceiptCounterRepository;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.assemblers.ReceiptCounterPersistenceAssembler;
import com.arcadiadevs.viora.platform.settlement.infrastructure.persistence.jpa.repositories.ReceiptCounterPersistenceRepository;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

/** JPA adapter for the receipt number counters, the only place that opens and moves a counter row. */
@Repository
public class ReceiptCounterRepositoryImpl implements ReceiptCounterRepository {

    private final ReceiptCounterPersistenceRepository persistenceRepository;
    private final boolean postgres;

    /**
     * Constructs the adapter, resolving once which of the two supported dialects the database speaks. The
     * connection used for the probe is returned to the pool before the constructor ends.
     *
     * @param persistenceRepository Spring Data repository of the counter rows
     * @param dataSource           the application data source, read once for its product name
     * @throws SQLException if the product name cannot be read
     */
    public ReceiptCounterRepositoryImpl(ReceiptCounterPersistenceRepository persistenceRepository, DataSource dataSource)
            throws SQLException {
        this.persistenceRepository = persistenceRepository;
        try (var connection = dataSource.getConnection()) {
            this.postgres = "postgresql".equalsIgnoreCase(connection.getMetaData().getDatabaseProductName());
        }
    }

    @Override
    public void ensureExists(UserId producerId, CampaignYear campaignYear) {
        var id = UUID.randomUUID();
        var producer = UUID.fromString(producerId.userId());
        var year = campaignYear.value();
        if (postgres) {
            persistenceRepository.insertOnConflictDoNothing(id, producer, year);
        } else {
            persistenceRepository.mergeKeepingLastSequence(id, producer, year);
        }
    }

    @Override
    public Optional<ReceiptCounter> findByProducerIdAndCampaignYearForUpdate(UserId producerId,
            CampaignYear campaignYear) {
        return persistenceRepository
                .findByProducerIdAndCampaignYearForUpdate(UUID.fromString(producerId.userId()), campaignYear.value())
                .map(ReceiptCounterPersistenceAssembler::toDomain);
    }

    @Override
    public void save(ReceiptCounter counter) {
        // The row already exists: it was opened by ensureExists and locked by the caller, so this is a move of the
        // sequence forward and never a second insert of the same producer and campaign.
        var entity = persistenceRepository
                .findByProducerIdAndCampaignYearForUpdate(UUID.fromString(counter.producerId().userId()),
                        counter.campaignYear().value())
                .orElseGet(() -> ReceiptCounterPersistenceAssembler.toEntity(counter));
        entity.setLastSequence(counter.lastSequence());
        persistenceRepository.save(entity);
    }
}
