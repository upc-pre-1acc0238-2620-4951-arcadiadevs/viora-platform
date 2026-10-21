package com.arcadiadevs.viora.platform.subscription.infrastructure.seeding;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCodeSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Demo data seeder for the Subscription context.
 * <p>There is no screen to issue codes yet, so it grants one cooperative license and issues known codes the
 * mobile app can redeem: three available ones, one expired and one revoked. It runs only while no license exists,
 * and can be turned off with {@code viora.subscription.seed.enabled=false}.</p>
 */
@Component
@Order(3)
public class SubscriptionDemoDataSeeder implements ApplicationRunner {

    /** Codes that can be redeemed once each. */
    public static final List<String> AVAILABLE_CODES = List.of("COOP-7K2Q", "COOP-4F9T", "COOP-9HX3");
    /** A code whose redemption deadline has passed. */
    public static final String EXPIRED_CODE = "COOP-2MKD";
    /** A code the cooperative revoked. */
    public static final String REVOKED_CODE = "COOP-7PZA";

    private static final Logger log = LoggerFactory.getLogger(SubscriptionDemoDataSeeder.class);

    private final CooperativeLicenseRepository licenseRepository;
    private final InvitationCodeRepository invitationCodeRepository;
    private final Clock clock;
    private final boolean enabled;

    /**
     * Creates the seeder.
     *
     * @param licenseRepository        the cooperative licenses
     * @param invitationCodeRepository the invitation codes
     * @param clock                    the application clock
     * @param enabled                  whether the seed runs
     */
    public SubscriptionDemoDataSeeder(
            CooperativeLicenseRepository licenseRepository,
            InvitationCodeRepository invitationCodeRepository,
            Clock clock,
            @Value("${viora.subscription.seed.enabled:true}") boolean enabled
    ) {
        this.licenseRepository = licenseRepository;
        this.invitationCodeRepository = invitationCodeRepository;
        this.clock = clock;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try {
            if (licenseRepository.existsAny()) {
                log.info("A cooperative license already exists. Skipping Subscription demo seeding.");
                return;
            }
            var now = clock.instant();
            var license = licenseRepository.save(CooperativeLicense.grant(new CooperativeId(),
                    "Coop. Olivarera La Yarada", 60, new HectaresQuota(new BigDecimal("200")),
                    new SubscriptionPeriod(now.minus(Duration.ofDays(30)), now.plus(Duration.ofDays(365)))));
            var licenseId = license.snapshot().id();
            var codesExpireAt = now.plus(Duration.ofDays(30));

            for (var code : AVAILABLE_CODES) {
                invitationCodeRepository.save(
                        InvitationCode.issue(licenseId, new CooperativeCode(code).fingerprint(), codesExpireAt));
            }
            invitationCodeRepository.save(closedCode(licenseId, EXPIRED_CODE, InvitationCodeStatus.EXPIRED,
                    now.minus(Duration.ofDays(1))));
            invitationCodeRepository.save(closedCode(licenseId, REVOKED_CODE, InvitationCodeStatus.REVOKED,
                    codesExpireAt));

            log.info("SubscriptionDemoDataSeeder granted license {} with demo invitation codes.", licenseId.cooperativeLicenseId());
        } catch (Exception ex) {
            log.error("Unexpected error during SubscriptionDemoDataSeeder execution: {}", ex.getMessage(), ex);
        }
    }

    private static InvitationCode closedCode(
            CooperativeLicenseId licenseId, String code, InvitationCodeStatus status, Instant expiresAt) {
        var issued = InvitationCode.issue(licenseId, new CooperativeCode(code).fingerprint(), expiresAt).snapshot();
        return InvitationCode.reconstitute(new InvitationCodeSnapshot(issued.id(), issued.licenseId(), null,
                issued.fingerprint(), status, issued.expiresAt(), null, null, issued.revision()));
    }
}
