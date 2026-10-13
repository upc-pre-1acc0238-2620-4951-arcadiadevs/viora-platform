package com.arcadiadevs.viora.platform.iam.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Event handler listening to {@link PasswordResetRequestedEvent} to trigger asynchronous email delivery.
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

    @EventListener
    @Async
    public void on(PasswordResetRequestedEvent event) {
        if (event != null) {
            emailService.sendPasswordResetEmail(event.email(), event.resetToken());
        }
    }
}
