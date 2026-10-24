package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import java.util.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PaymentAttempt;
/** Durable attempts used for checkout recovery and authoritative payment matching. */
public interface PaymentAttemptRepository {
    PaymentAttempt save(PaymentAttempt attempt);
    Optional<PaymentAttempt> find(UUID id);
    /** @param subscriptionId plan @return latest attempt that pays the plan itself, never one of its extensions */
    Optional<PaymentAttempt> latest(UUID subscriptionId);
    /** @param extensionId pending extension @return latest attempt that pays that extension */
    Optional<PaymentAttempt> latestForExtension(UUID extensionId);
}
