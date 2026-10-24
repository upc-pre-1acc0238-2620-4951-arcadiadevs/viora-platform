package com.arcadiadevs.viora.platform.subscription.infrastructure.seeding;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicenseSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.SequencedMap;

/**
 * Test codes for the team: a license of its own ("Viora · Pruebas") with five single-use codes for each plan size, so
 * new accounts can be activated while testing without Mercado Pago. Unlike {@link SubscriptionDemoDataSeeder} it does
 * not depend on other licenses: it runs once, while its own license does not exist yet, under the same
 * {@code viora.subscription.seed.enabled} switch (on only in the demo profile).
 */
@Component
@Order(4)
public class SubscriptionTestCodesSeeder implements ApplicationRunner {

    /** Fixed so a restart finds the license and seeds nothing twice. */
    public static final CooperativeLicenseId LICENSE_ID = new CooperativeLicenseId("7e57c0de-0000-4000-8000-000000000001");
    /** Cooperative name the producer sees on P06 after redeeming one of these codes. */
    public static final String COOPERATIVE_NAME = "Viora · Pruebas";
    /** Each code with the hectares it covers: COOP-T{hectares}{A..E}. */
    public static final SequencedMap<String, BigDecimal> CODES = codes();

    private static final Logger log = LoggerFactory.getLogger(SubscriptionTestCodesSeeder.class);

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
    public SubscriptionTestCodesSeeder(
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
            if (licenseRepository.findById(LICENSE_ID).isPresent()) {
                log.info("The test codes license already exists. Skipping test codes seeding.");
                return;
            }
            var now = clock.instant();
            var area = CODES.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            var license = CooperativeLicense.reconstitute(new CooperativeLicenseSnapshot(LICENSE_ID, new CooperativeId(),
                    COOPERATIVE_NAME, CODES.size(), new HectaresQuota(area), BigDecimal.ZERO,
                    new SubscriptionPeriod(now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(730))), 0L));
            var codesExpireAt = now.plus(Duration.ofDays(365));
            var codes = new ArrayList<InvitationCode>();
            CODES.forEach((code, hectares) -> codes.add(license.issueCode(
                    new CooperativeCode(code).fingerprint(), new HectaresQuota(hectares), codesExpireAt)));
            licenseRepository.save(license);
            codes.forEach(invitationCodeRepository::save);
            log.info("SubscriptionTestCodesSeeder granted the test license with {} codes.", codes.size());
        } catch (Exception ex) {
            log.error("Unexpected error during SubscriptionTestCodesSeeder execution: {}", ex.getMessage(), ex);
        }
    }

    private static SequencedMap<String, BigDecimal> codes() {
        var codes = new LinkedHashMap<String, BigDecimal>();
        for (var hectares : new int[] {2, 5, 12, 20}) {
            for (var letter : "ABCDE".toCharArray()) {
                codes.put("COOP-T%02d%s".formatted(hectares, letter), BigDecimal.valueOf(hectares));
            }
        }
        return Collections.unmodifiableSequencedMap(codes);
    }
}
