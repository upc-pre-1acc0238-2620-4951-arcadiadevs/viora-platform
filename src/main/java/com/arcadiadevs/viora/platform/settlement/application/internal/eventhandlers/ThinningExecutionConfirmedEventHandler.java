package com.arcadiadevs.viora.platform.settlement.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.ThinningExecutionRecord;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.ThinningExecutionRecordRepository;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.ThinningExecutionConfirmedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Projects confirmed thinning executions into Settlement, so a settlement can freeze its balance against the
 * prescription without reading Thinning repositories.
 *
 * <p>Runs before the confirmation commits, inside the same transaction: the confirmation and its projection
 * are stored together or not at all. Repeated events are ignored.</p>
 */
@Component
public class ThinningExecutionConfirmedEventHandler {

    private final ThinningExecutionRecordRepository repository;

    public ThinningExecutionConfirmedEventHandler(ThinningExecutionRecordRepository repository) {
        this.repository = repository;
    }

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(ThinningExecutionConfirmedEvent event) {
        repository.saveIfAbsent(new ThinningExecutionRecord(event.eventId(), event.confirmationId(),
                new PlotId(event.plotId()), event.campaignYear(), event.executedDate(),
                event.prescribedRemovalPercentage(), event.removalPercentage(),
                "OPTIMAL".equals(event.timeliness())));
    }
}
