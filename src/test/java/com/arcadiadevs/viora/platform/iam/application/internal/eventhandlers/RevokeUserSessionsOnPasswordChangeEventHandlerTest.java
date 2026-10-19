package com.arcadiadevs.viora.platform.iam.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.domain.model.UserAccountFixtures;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserSession;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordChangedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetCompletedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserSessionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("RevokeUserSessionsOnPasswordChangeEventHandler Unit Tests")
class RevokeUserSessionsOnPasswordChangeEventHandlerTest {

    private final UserSessionRepository repository = Mockito.mock(UserSessionRepository.class);
    private final RevokeUserSessionsOnPasswordChangeEventHandler handler =
            new RevokeUserSessionsOnPasswordChangeEventHandler(repository);

    @Test
    @DisplayName("Revokes every open session when the password changes or is recovered")
    void revokesSessionsOnPasswordChange() {
        var userId = new UserId();
        var now = Instant.now();
        var phone = UserSession.start(userId, UserAccountFixtures.tokenHashOf("hash-1"), 0L, now);
        var tablet = UserSession.start(userId, UserAccountFixtures.tokenHashOf("hash-2"), 0L, now);
        when(repository.findNotRevokedByUserId(any(UserId.class))).thenReturn(List.of(phone, tablet));

        handler.on(new PasswordChangedEvent(userId.userId(), now));

        assertThat(phone.isActive(now)).isFalse();
        assertThat(tablet.isActive(now)).isFalse();
        verify(repository, times(2)).save(any(UserSession.class));

        handler.on(new PasswordResetCompletedEvent(userId.userId(), now));
        verify(repository, times(2)).findNotRevokedByUserId(userId);
    }
}
