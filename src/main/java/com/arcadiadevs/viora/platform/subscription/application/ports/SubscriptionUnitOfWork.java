package com.arcadiadevs.viora.platform.subscription.application.ports;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
/** Durable owner serialization and idempotency port, implemented transactionally by infrastructure. */
public interface SubscriptionUnitOfWork {
    <T> T exclusive(String producerId, Supplier<T> work);
    Optional<Operation> findOperation(String producerId, String key);
    void recordOperation(String producerId, String key, String kind, String fingerprint, UUID resourceId);
    record Operation(String kind, String fingerprint, UUID resourceId) { }
}
