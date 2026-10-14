package com.arcadiadevs.viora.platform.iam.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event handler listening to {@link PasswordResetRequestedEvent} to trigger asynchronous email delivery.
 *
 * <p>The email is sent only after the transaction that stored the new code commits, so a rolled back request
 * never delivers a code that does not exist.</p>
 */
@Component
public class PasswordResetRequestedEventHandler {

    private final TransactionalEmailService emailService;

    public PasswordResetRequestedEventHandler(TransactionalEmailService emailService) {
        if (emailService == null) {
            throw new IllegalArgumentException("email.service.null");
        }
        this.emailService = emailService;
    }

    /**
     * Sends the recovery code to the account email once the reset request is committed.
     *
     * @param event the committed password reset request
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Async
    public void on(PasswordResetRequestedEvent event) {
        if (event != null) {
            emailService.sendPasswordResetEmail(event.email(), event.resetToken());
        }
    }
}
