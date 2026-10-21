package com.arcadiadevs.viora.platform.subscription.application.commandservices;

import java.util.UUID;
import com.arcadiadevs.viora.platform.shared.application.result.*;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.CreateSubscriptionCommand;
/** Initial subscription command boundary. */
public interface SubscriptionCommandService {
    Result<CreatedSubscription, ApplicationError> handle(CreateSubscriptionCommand command);
    record CreatedSubscription(UUID id, boolean replayed) { }
}
