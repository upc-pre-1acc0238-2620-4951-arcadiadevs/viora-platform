package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.subscription.application.commandservices.DataSharingConsentCommandService;
import com.arcadiadevs.viora.platform.subscription.application.ports.SubscriptionUnitOfWork;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.DataSharingConsentNotActiveException;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.DataSharingConsent;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.WithdrawDataSharingConsentCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.DataSharingConsentId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.DataSharingConsentRepository;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.util.Map;

/**
 * Application service withdrawing the data sharing consents of producers. The consent is locked while it changes,
 * under the producer's lock.
 */
@Service
public class DataSharingConsentCommandServiceImpl implements DataSharingConsentCommandService {

    private final DataSharingConsentRepository dataSharingConsentRepository;
    private final Clock clock;
    private final SubscriptionUnitOfWork work;

    /**
     * Creates the service.
     *
     * @param dataSharingConsentRepository the data sharing consents
     * @param clock                        the application clock
     * @param work                         the per producer lock and transaction
     * @throws IllegalArgumentException if any dependency is null
     */
    public DataSharingConsentCommandServiceImpl(DataSharingConsentRepository dataSharingConsentRepository, Clock clock,
                                                SubscriptionUnitOfWork work) {
        if (dataSharingConsentRepository == null || clock == null || work == null) {
            throw new IllegalArgumentException("subscription.consent_service.dependency.null");
        }
        this.dataSharingConsentRepository = dataSharingConsentRepository;
        this.clock = clock;
        this.work = work;
    }

    @Override
    public Result<DataSharingConsent, ApplicationError> handle(WithdrawDataSharingConsentCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "subscription.consent.command.null"));
        }
        try {
            var producerId = new ProducerId(command.producerId());
            var consentId = new DataSharingConsentId(command.consentId());
            return work.exclusive(producerId.producerId(), () -> {
                var found = dataSharingConsentRepository.findByIdForUpdate(consentId)
                        .filter(consent -> consent.snapshot().producerId().equals(producerId));
                // A consent of another producer answers the same as one that does not exist.
                if (found.isEmpty()) {
                    return Result.failure(ApplicationError.notFound("consent", consentId.dataSharingConsentId()));
                }
                try {
                    found.get().withdraw(clock.instant());
                } catch (DataSharingConsentNotActiveException ex) {
                    return Result.failure(ApplicationError.conflict("consent", ex.getMessage(),
                            Map.of("reason", ex.getMessage())));
                }
                return Result.success(dataSharingConsentRepository.save(found.get()));
            });
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }
}
