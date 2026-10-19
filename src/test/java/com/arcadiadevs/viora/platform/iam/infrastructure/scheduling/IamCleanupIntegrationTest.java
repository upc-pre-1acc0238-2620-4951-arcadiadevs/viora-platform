package com.arcadiadevs.viora.platform.iam.infrastructure.scheduling;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.application.commandservices.UserSessionCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.UserAccountFixtures;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserSession;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.DiscardUnverifiedUserAccountsCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.PurgeEndedUserSessionsCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserSessionRepository;
import com.arcadiadevs.viora.platform.profiles.domain.repositories.ProfileRepository;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The IAM cleanup against the real database: the JPA queries, and the profile removed with its account.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:iamcleanup;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("IAM cleanup integration")
class IamCleanupIntegrationTest {

    @Autowired UserAccountCommandService userAccountCommandService;
    @Autowired UserSessionCommandService userSessionCommandService;
    @Autowired UserAccountRepository userAccountRepository;
    @Autowired UserSessionRepository userSessionRepository;
    @Autowired ProfileRepository profileRepository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Discards a pending account abandoned for 8 days together with its profile, and keeps a recent one")
    void discardsAbandonedAccountWithProfile() {
        var abandonedId = signUp("abandonada@viora.ag");
        var recentId = signUp("reciente@viora.ag");
        // The auditing listener stamps updated_at on every save, so the age is set straight in the table
        jdbcTemplate.update("update user_accounts set updated_at = ? where id = ?",
                Timestamp.from(Instant.now().minus(8, ChronoUnit.DAYS)), UUID.fromString(abandonedId));

        var result = userAccountCommandService.handle(new DiscardUnverifiedUserAccountsCommand());

        assertThat(((Result.Success<Integer, ?>) result).value()).isEqualTo(1);
        assertThat(userAccountRepository.findById(new UserId(abandonedId))).isEmpty();
        assertThat(profileRepository.findByUserId(
                new com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.UserId(abandonedId))).isEmpty();
        assertThat(userAccountRepository.findById(new UserId(recentId))).isPresent();
        assertThat(profileRepository.findByUserId(
                new com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.UserId(recentId))).isPresent();
    }

    @Test
    @DisplayName("Removes the sessions that ended over 30 days ago and keeps the rest")
    void purgesEndedSessions() {
        var userId = new UserId();
        var longAgo = Instant.now().minus(UserSession.LIFESPAN).minus(UserSession.RETENTION_AFTER_END).minus(1, ChronoUnit.DAYS);
        userSessionRepository.save(UserSession.start(userId, UserAccountFixtures.tokenHashOf("expired"), 0L, longAgo));
        var revoked = UserSession.start(userId, UserAccountFixtures.tokenHashOf("revoked"), 0L, Instant.now());
        revoked.revoke(Instant.now().minus(UserSession.RETENTION_AFTER_END).minus(1, ChronoUnit.DAYS));
        userSessionRepository.save(revoked);
        userSessionRepository.save(UserSession.start(userId, UserAccountFixtures.tokenHashOf("active"), 0L, Instant.now()));

        var result = userSessionCommandService.handle(new PurgeEndedUserSessionsCommand());

        assertThat(((Result.Success<Integer, ?>) result).value()).isEqualTo(2);
        assertThat(userSessionRepository.findByRefreshTokenHash(UserAccountFixtures.tokenHashOf("expired"))).isEmpty();
        assertThat(userSessionRepository.findByRefreshTokenHash(UserAccountFixtures.tokenHashOf("revoked"))).isEmpty();
        assertThat(userSessionRepository.findByRefreshTokenHash(UserAccountFixtures.tokenHashOf("active"))).isPresent();
    }

    private String signUp(String email) {
        var result = userAccountCommandService.handle(new RegisterUserAccountCommand(email, "Password123",
                "ROLE_PRODUCER", true, "Juan Perez", "PE", "+51987654321"));
        assertThat(result).isInstanceOf(Result.Success.class);
        return ((Result.Success<String, ?>) result).value();
    }
}
