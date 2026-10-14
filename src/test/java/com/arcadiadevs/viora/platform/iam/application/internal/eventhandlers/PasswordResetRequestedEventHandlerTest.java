package com.arcadiadevs.viora.platform.iam.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("PasswordResetRequestedEventHandler Unit Tests")
class PasswordResetRequestedEventHandlerTest {

    private final TransactionalEmailService emailService = Mockito.mock(TransactionalEmailService.class);
    private final PasswordResetRequestedEventHandler handler = new PasswordResetRequestedEventHandler(emailService);

    @Test
    @DisplayName("Should reject null email service dependency in constructor")
    void shouldRejectNullEmailService() {
        assertThatThrownBy(() -> new PasswordResetRequestedEventHandler(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("email.service.null");
    }

    @Test
    @DisplayName("Should dispatch email when receiving valid PasswordResetRequestedEvent")
    void shouldDispatchEmailOnEvent() {
        var event = new PasswordResetRequestedEvent(
                "user-123",
                "producer@viora.ag",
                "token-abc",
                Instant.now().plusSeconds(900),
                Instant.now()
        );

        handler.on(event);

        verify(emailService).sendPasswordResetEmail("producer@viora.ag", "token-abc");
    }

    @Test
    @DisplayName("Should handle null event without throwing")
    void shouldHandleNullEvent() {
        assertThatCode(() -> handler.on(null)).doesNotThrowAnyException();
        verifyNoInteractions(emailService);
    }
}
