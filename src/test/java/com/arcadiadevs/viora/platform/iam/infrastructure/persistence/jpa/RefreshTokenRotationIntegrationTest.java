package com.arcadiadevs.viora.platform.iam.infrastructure.persistence.jpa;

import com.arcadiadevs.viora.platform.iam.application.commandservices.AuthenticatedSession;
import com.arcadiadevs.viora.platform.iam.application.commandservices.UserSessionCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.FakePasswordHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.UserAccountFixtures;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.OpenUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RefreshUserSessionCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The refresh token rotation against the real database: the successor column and the row locks.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:refreshrotation;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Refresh token rotation integration")
class RefreshTokenRotationIntegrationTest {

    @Autowired UserSessionCommandService sessions;
    @Autowired UserAccountRepository accounts;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Recovers a lost answer once the successor is stored, and raises the alarm once that chain is shared")
    void recoversThenRaisesTheAlarm() {
        var userId = newAccount("perdida@viora.ag");
        var first = open(userId);
        var lost = refresh(first.refreshToken());

        var recovered = refresh(first.refreshToken());

        assertThat(recovered.refreshToken()).isNotEqualTo(lost.refreshToken());
        assertThat(activeSessionsOf(userId)).isEqualTo(1);

        assertThat(sessions.handle(new RefreshUserSessionCommand(lost.refreshToken())))
                .isInstanceOf(Result.Failure.class);
        assertThat(activeSessionsOf(userId)).isZero();
    }

    @Test
    @DisplayName("Serves several refreshes of one token at once and leaves exactly one session open")
    void concurrentRefreshesLeaveOneSession() throws Exception {
        var userId = newAccount("simultanea@viora.ag");
        var token = open(userId).refreshToken();
        int requests = 4;
        var start = new CountDownLatch(1);
        var pool = Executors.newFixedThreadPool(requests);
        var calls = new ArrayList<Future<Result<AuthenticatedSession, ApplicationError>>>();
        try {
            for (int i = 0; i < requests; i++) {
                Callable<Result<AuthenticatedSession, ApplicationError>> call = () -> {
                    start.await();
                    return sessions.handle(new RefreshUserSessionCommand(token));
                };
                calls.add(pool.submit(call));
            }
            start.countDown();
            for (var call : calls) {
                assertThat(call.get(30, TimeUnit.SECONDS)).isInstanceOf(Result.Success.class);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(activeSessionsOf(userId)).isEqualTo(1);
    }

    private String newAccount(String email) {
        var account = UserAccountFixtures.activeAccount(email, FakePasswordHasher.hashOf("Password123").value(),
                UserRole.ROLE_PRODUCER);
        accounts.save(account);
        return account.snapshot().id().userId();
    }

    private AuthenticatedSession open(String userId) {
        return value(sessions.handle(new OpenUserSessionCommand(userId)));
    }

    private AuthenticatedSession refresh(String refreshToken) {
        return value(sessions.handle(new RefreshUserSessionCommand(refreshToken)));
    }

    private int activeSessionsOf(String userId) {
        var count = jdbcTemplate.queryForObject(
                "select count(*) from user_sessions where user_id = ? and revoked_at is null", Integer.class,
                UUID.fromString(userId));
        return count == null ? 0 : count;
    }

    private static AuthenticatedSession value(Result<AuthenticatedSession, ApplicationError> result) {
        assertThat(result).isInstanceOf(Result.Success.class);
        return ((Result.Success<AuthenticatedSession, ApplicationError>) result).value();
    }
}
