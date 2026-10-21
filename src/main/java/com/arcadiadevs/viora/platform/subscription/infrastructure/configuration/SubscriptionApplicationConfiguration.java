package com.arcadiadevs.viora.platform.subscription.infrastructure.configuration;

import java.time.Clock;
import org.springframework.context.annotation.*;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.SubscriptionCommandService;
import com.arcadiadevs.viora.platform.subscription.application.internal.commandservices.SubscriptionCommandServiceImpl;
import com.arcadiadevs.viora.platform.subscription.application.internal.queryservices.SubscriptionQueryService;
import com.arcadiadevs.viora.platform.subscription.application.ports.*;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.SubscriptionCatalog;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
/** Composition of application ports with infrastructure adapters. */
@Configuration
public class SubscriptionApplicationConfiguration {
    @Bean public SubscriptionCommandService subscriptionCommands(SubscriptionRepository repository,
            SubscriptionUnitOfWork work, SubscriptionCatalog catalog, ProducerProfilePort profiles, Clock clock) {
        return new SubscriptionCommandServiceImpl(repository, work, catalog, profiles, clock);
    }
    @Bean public SubscriptionQueryService subscriptionQueries(SubscriptionRepository repository, Clock clock) {
        return new SubscriptionQueryService(repository, clock);
    }
}
