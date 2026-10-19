package com.arcadiadevs.viora.platform.profiles.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UnverifiedUserAccountDiscardedEvent;
import com.arcadiadevs.viora.platform.profiles.application.commandservices.ProfileCommandService;
import com.arcadiadevs.viora.platform.profiles.domain.model.commands.DeleteProfileCommand;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@DisplayName("DeleteProfileOnUnverifiedUserAccountDiscardedHandler Unit Tests")
class DeleteProfileOnUnverifiedUserAccountDiscardedHandlerTest {

    private final ProfileCommandService profileCommandService = Mockito.mock(ProfileCommandService.class);
    private final DeleteProfileOnUnverifiedUserAccountDiscardedHandler handler =
            new DeleteProfileOnUnverifiedUserAccountDiscardedHandler(profileCommandService);

    @Test
    @DisplayName("Deletes the profile of the discarded account")
    void deletesProfileOfDiscardedAccount() {
        var userId = UUID.randomUUID().toString();

        handler.on(new UnverifiedUserAccountDiscardedEvent(userId, "productor@viora.ag", Instant.now()));

        var captor = ArgumentCaptor.forClass(DeleteProfileCommand.class);
        verify(profileCommandService).handle(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Ignores a null event")
    void ignoresNullEvent() {
        assertThatCode(() -> handler.on(null)).doesNotThrowAnyException();
        verifyNoInteractions(profileCommandService);
    }
}
