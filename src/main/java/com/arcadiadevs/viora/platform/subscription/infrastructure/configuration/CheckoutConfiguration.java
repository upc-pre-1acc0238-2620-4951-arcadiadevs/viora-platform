package com.arcadiadevs.viora.platform.subscription.infrastructure.configuration;

import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.*;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.CheckoutService;
import com.arcadiadevs.viora.platform.subscription.application.ports.*;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.*;
/** Wires the payment use cases to provider and persistence ports. */
@Configuration
public class CheckoutConfiguration {
    @Bean public CheckoutService checkoutService(SubscriptionRepository subscriptions, PaymentAttemptRepository attempts,
            SubscriptionUnitOfWork work, PaymentGateway gateway, ApplicationEventPublisher events, Clock clock,
            @Value("${viora.payments.checkout-ttl:PT24H}") Duration ttl) {
        return new CheckoutService(subscriptions, attempts, work, gateway, events, clock, ttl);
    }
}
