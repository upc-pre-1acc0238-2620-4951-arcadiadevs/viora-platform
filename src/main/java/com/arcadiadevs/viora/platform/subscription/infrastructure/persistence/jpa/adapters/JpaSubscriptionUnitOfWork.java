package com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.adapters;

import java.util.*;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import com.arcadiadevs.viora.platform.subscription.application.ports.SubscriptionUnitOfWork;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.*;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.entities.SubscriptionOperationEntity;
/** Database-backed serialization, also effective across multiple backend instances. */
@Component
public class JpaSubscriptionUnitOfWork implements SubscriptionUnitOfWork {
    private final SubscriptionGateRepository gates;
    private final SubscriptionOperationRepository operations;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final TransactionTemplate initialization;
    public JpaSubscriptionUnitOfWork(SubscriptionGateRepository gates, SubscriptionOperationRepository operations,
            JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.gates = gates; this.operations = operations; this.jdbc = jdbc;
        transaction = new TransactionTemplate(manager);
        initialization = new TransactionTemplate(manager);
        initialization.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }
    @Override public <T> T exclusive(String producerId, Supplier<T> work) {
        initialization.executeWithoutResult(status -> {
            var product = jdbc.execute((java.sql.Connection connection) -> connection.getMetaData().getDatabaseProductName());
            if ("PostgreSQL".equals(product)) {
                jdbc.update("INSERT INTO subscription_producer_gates (producer_id) VALUES (?) ON CONFLICT (producer_id) DO NOTHING", producerId);
            } else if ("H2".equals(product)) {
                jdbc.update("MERGE INTO subscription_producer_gates (producer_id) KEY(producer_id) VALUES (?)", producerId);
            } else { throw new IllegalStateException("subscription.database.unsupported"); }
        });
        return transaction.execute(status -> {
            gates.lock(producerId).orElseThrow();
            return work.get();
        });
    }
    @Override public Optional<Operation> findOperation(String owner, String key) {
        return operations.findByProducerIdAndOperationKey(owner, key).map(o -> new Operation(o.getKind(), o.getFingerprint(), o.getResourceId()));
    }
    @Override public void recordOperation(String owner, String key, String kind, String fingerprint, UUID resourceId) {
        var row = new SubscriptionOperationEntity();
        row.setId(UUID.randomUUID()); row.setProducerId(owner); row.setOperationKey(key); row.setKind(kind);
        row.setFingerprint(fingerprint); row.setResourceId(resourceId); operations.saveAndFlush(row);
    }
}
