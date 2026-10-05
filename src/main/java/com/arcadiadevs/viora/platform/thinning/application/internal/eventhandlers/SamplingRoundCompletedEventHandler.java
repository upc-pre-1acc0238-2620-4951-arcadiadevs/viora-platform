package com.arcadiadevs.viora.platform.thinning.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.ThinningPrescriptionIssuer;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.SamplingRoundCompletedEvent;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionId;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Issues the thinning prescription as soon as the field sampling becomes representative, if the rest
 * of its inputs (technical profile and full bloom) are already known. When they are not, the
 * prescription is issued later, when the missing input arrives.
 *
 * <p>A failure here never breaks the sampling that triggered it. Runs inside the sampling transaction.</p>
 */
@Component
public class SamplingRoundCompletedEventHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SamplingRoundCompletedEventHandler.class);

    private final FruitThinningPrescriptionRepository prescriptionRepository;
    private final ThinningPrescriptionIssuer issuer;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs the handler.
     *
     * @param prescriptionRepository prescription repository
     * @param issuer                 issues the prescription when it is ready
     * @param eventPublisher         publisher for the events the prescription registers
     */
    public SamplingRoundCompletedEventHandler(
            FruitThinningPrescriptionRepository prescriptionRepository,
            ThinningPrescriptionIssuer issuer,
            ApplicationEventPublisher eventPublisher
    ) {
        this.prescriptionRepository = prescriptionRepository;
        this.issuer = issuer;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Reacts to a sampling that just reached representative coverage.
     *
     * @param event the completed-sampling event
     */
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(SamplingRoundCompletedEvent event) {
        var found = prescriptionRepository.findByIdForUpdate(new PrescriptionId(event.prescriptionId()));
        if (found.isEmpty()) {
            return;
        }
        var prescription = found.get();
        try {
            if (!issuer.issueIfReady(prescription, false)) {
                return;
            }
        } catch (IllegalStateException | IllegalArgumentException exception) {
            LOG.warn("Thinning prescription not issued for plot {}: {}", event.plotId(), exception.getMessage());
            return;
        }
        prescriptionRepository.save(prescription);
        prescription.domainEvents().forEach(eventPublisher::publishEvent);
        prescription.clearDomainEvents();
    }
}
