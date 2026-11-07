package com.arcadiadevs.viora.platform.territory.infrastructure.scheduling;

import com.arcadiadevs.viora.platform.territory.application.commandservices.SectorAlertCommandService;
import com.arcadiadevs.viora.platform.territory.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.territory.domain.model.commands.EvaluateSectorAlertsCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Brings the sector alerts of every managed license in force up to date one minute after the platform starts and
 * then every fifteen minutes.
 * <p>The first run matters on a host that sleeps when idle (Render free tier). A license that fails is logged and the
 * rest still run. Disable it with {@code viora.territory.sector-alerts.enabled=false} (the tests do, and call the
 * command service themselves).</p>
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "viora.territory.sector-alerts.enabled", havingValue = "true", matchIfMissing = true)
public class SectorAlertScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(SectorAlertScheduler.class);

    private final SectorAlertCommandService sectorAlertCommandService;
    private final ExternalSubscriptionService externalSubscriptionService;

    /**
     * Creates the scheduler.
     *
     * @param sectorAlertCommandService   the evaluation of a license's alerts
     * @param externalSubscriptionService the managed licenses in force
     */
    public SectorAlertScheduler(SectorAlertCommandService sectorAlertCommandService,
                                ExternalSubscriptionService externalSubscriptionService) {
        this.sectorAlertCommandService = sectorAlertCommandService;
        this.externalSubscriptionService = externalSubscriptionService;
    }

    /**
     * Evaluates the alerts of every managed license in force.
     */
    @Scheduled(initialDelayString = "PT1M", fixedDelayString = "PT15M")
    public void run() {
        for (var licenseId : externalSubscriptionService.findManagedLicensesInForce()) {
            try {
                var result = sectorAlertCommandService.handle(new EvaluateSectorAlertsCommand(licenseId.licenseId()));
                result.failure().ifPresent(error -> LOG.warn("Sector alerts of license {} not evaluated: {}",
                        licenseId.licenseId(), error.details()));
            } catch (RuntimeException exception) {
                LOG.warn("Sector alerts of license {} failed: {}", licenseId.licenseId(), exception.getMessage());
            }
        }
    }
}
