package com.arcadiadevs.viora.platform.iam.domain.model;

import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TokenHash;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserSession;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UserSession Aggregate Root Domain Unit Tests")
class UserSessionTest {

    private final UserId userId = new UserId();

    @Test
    @DisplayName("Should start a session valid for 7 days that keeps the credentials version")
    void shouldStartSession() {
        var now = Instant.now();
        var session = UserSession.start(userId, UserAccountFixtures.tokenHashOf("hash"), 2L, now);

        assertThat(session.isActive(now.plus(6, ChronoUnit.DAYS))).isTrue();
        assertThat(session.isActive(now.plus(8, ChronoUnit.DAYS))).isFalse();
        assertThat(session.snapshot().credentialsVersion()).isEqualTo(2);
        assertThat(session.snapshot().revokedAt()).isNull();
    }

    @Test
    @DisplayName("Should stay revoked at its first revocation instant")
    void shouldRevokeOnce() {
        var now = Instant.now();
        var session = UserSession.start(userId, UserAccountFixtures.tokenHashOf("hash"), 0L, now);

        session.revoke(now.plusSeconds(10));
        session.revoke(now.plusSeconds(20));

        assertThat(session.isActive(now.plusSeconds(30))).isFalse();
        assertThat(session.snapshot().revokedAt()).isEqualTo(now.plusSeconds(10));
    }

    @Test
    @DisplayName("Should remember its successor when it is replaced, and survive being rebuilt from the snapshot")
    void shouldRememberSuccessor() {
        var now = Instant.now();
        var session = UserSession.start(userId, UserAccountFixtures.tokenHashOf("hash"), 0L, now);
        var successor = UserSession.start(userId, UserAccountFixtures.tokenHashOf("next"), 0L, now);

        session.replaceWith(successor.snapshot().id(), now.plusSeconds(10));

        assertThat(session.snapshot().revokedAt()).isEqualTo(now.plusSeconds(10));
        assertThat(UserSession.reconstitute(session.snapshot()).snapshot().successorId())
                .isEqualTo(successor.snapshot().id());
    }

    @Test
    @DisplayName("Should treat the rotation answer as lost only while the successor was never used")
    void shouldDetectLostRotationAnswer() {
        var now = Instant.now();
        var session = UserSession.start(userId, UserAccountFixtures.tokenHashOf("hash"), 0L, now);
        var successor = UserSession.start(userId, UserAccountFixtures.tokenHashOf("next"), 0L, now);
        var stranger = UserSession.start(userId, UserAccountFixtures.tokenHashOf("other"), 0L, now);
        assertThat(session.rotationAnswerWasLost(successor, now)).isFalse();

        session.replaceWith(successor.snapshot().id(), now);

        assertThat(session.rotationAnswerWasLost(successor, now.plusSeconds(60))).isTrue();
        assertThat(session.rotationAnswerWasLost(stranger, now.plusSeconds(60))).isFalse();
        assertThat(session.rotationAnswerWasLost(null, now.plusSeconds(60))).isFalse();
        assertThat(session.rotationAnswerWasLost(successor, now.plus(8, ChronoUnit.DAYS))).isFalse();
        successor.revoke(now.plusSeconds(90));
        assertThat(session.rotationAnswerWasLost(successor, now.plusSeconds(120))).isFalse();
    }

    @Test
    @DisplayName("Should reject a session without refresh token hash or with a negative version")
    void shouldValidateStart() {
        assertThatThrownBy(() -> UserSession.start(userId, null, 0L, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.session.refresh_token_hash.null");
        assertThatThrownBy(() -> UserSession.start(userId, UserAccountFixtures.tokenHashOf("hash"), -1L, Instant.now()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("user.session.credentials_version.negative");
    }
}
