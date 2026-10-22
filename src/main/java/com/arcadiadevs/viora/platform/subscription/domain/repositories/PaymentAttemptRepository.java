package com.arcadiadevs.viora.platform.subscription.domain.repositories;

import java.util.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PaymentAttempt;
/** Durable attempts used for checkout recovery and authoritative payment matching. */
public interface PaymentAttemptRepository {
    PaymentAttempt save(PaymentAttempt attempt);
    Optional<PaymentAttempt> find(UUID id);
    Optional<PaymentAttempt> latest(UUID subscriptionId);
}
