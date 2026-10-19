package com.arcadiadevs.viora.platform.iam.infrastructure.scheduling;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.application.commandservices.UserSessionCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.DiscardUnverifiedUserAccountsCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.PurgeEndedUserSessionsCommand;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Runs the IAM cleanup two minutes after the platform starts and then once a day: it discards the pending accounts
 * abandoned before verifying their email, with their profiles, and removes the sessions that ended long ago.
 *
 * <p>It runs relative to the startup instead of at a fixed hour because the host sleeps when idle (Render free tier),
 * so a fixed hour might never be reached. Disable it with {@code viora.iam.cleanup.enabled=false} (the tests do).</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "viora.iam.cleanup.enabled", havingValue = "true", matchIfMissing = true)
public class IamCleanupScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(IamCleanupScheduler.class);

    private final UserAccountCommandService userAccountCommandService;
    private final UserSessionCommandService userSessionCommandService;

    /**
     * Creates the scheduler.
     *
     * @param userAccountCommandService discards the abandoned pending accounts
     * @param userSessionCommandService removes the ended sessions
     */
    public IamCleanupScheduler(UserAccountCommandService userAccountCommandService,
                               UserSessionCommandService userSessionCommandService) {
        this.userAccountCommandService = userAccountCommandService;
        this.userSessionCommandService = userSessionCommandService;
    }

    /**
     * Runs both cleanups. A failure of one does not stop the other.
     */
    @Scheduled(initialDelayString = "PT2M", fixedDelayString = "P1D")
    public void run() {
        try {
            if (userAccountCommandService.handle(new DiscardUnverifiedUserAccountsCommand())
                    instanceof Result.Success<Integer, ?> discarded && discarded.value() > 0) {
                LOG.info("Discarded {} unverified accounts", discarded.value());
            }
        } catch (RuntimeException exception) {
            LOG.warn("Unverified accounts cleanup failed: {}", exception.getMessage());
        }
        try {
            if (userSessionCommandService.handle(new PurgeEndedUserSessionsCommand())
                    instanceof Result.Success<Integer, ?> purged && purged.value() > 0) {
                LOG.info("Removed {} ended sessions", purged.value());
            }
        } catch (RuntimeException exception) {
            LOG.warn("Ended sessions cleanup failed: {}", exception.getMessage());
        }
    }
}
