package com.arcadiadevs.viora.platform.subscription.infrastructure.seeding;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.ManagerActivationCodeRepository;
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
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Manager codes for testing the technical manager app before there is a script to grant real licenses: each code set in
 * {@code viora.subscription.manager-seed-codes} gets a test license of its own, of 60 seats and 200 hectares for a year,
 * and activates it once.
 * <p>The codes come from the environment and are never committed, because the repository is public and a code hands
 * over a license. Only their fingerprints are stored, and a code already issued is skipped, so a restart seeds nothing
 * twice and a used code is never brought back.</p>
 */
@Component
@Order(6)
public class ManagerCodesSeeder implements ApplicationRunner {

    /** Name the manager sees on the license after redeeming one of these codes. */
    public static final String ORGANIZATION_NAME = "Viora · Pruebas";
    /** Producers each test license covers. */
    public static final int SEATS = 60;
    /** Hectares each test license covers. */
    public static final BigDecimal CONTRACTED_AREA_HA = new BigDecimal("200");

    private static final Duration TERM = Duration.ofDays(365);
    private static final Logger log = LoggerFactory.getLogger(ManagerCodesSeeder.class);

    private final CooperativeLicenseRepository licenses;
    private final ManagerActivationCodeRepository codes;
    private final Clock clock;
    private final List<String> configuredCodes;

    /**
     * Creates the seeder.
     *
     * @param licenses        the cooperative licenses
     * @param codes           the manager activation codes
     * @param clock           the application clock
     * @param configuredCodes the codes to seed, comma separated; empty seeds nothing
     */
    public ManagerCodesSeeder(
            CooperativeLicenseRepository licenses,
            ManagerActivationCodeRepository codes,
            Clock clock,
            @Value("${viora.subscription.manager-seed-codes:}") String configuredCodes
    ) {
        this.licenses = licenses;
        this.codes = codes;
        this.clock = clock;
        this.configuredCodes = Arrays.stream(configuredCodes == null ? new String[0] : configuredCodes.split(","))
                .map(String::trim)
                .filter(code -> !code.isEmpty())
                .collect(LinkedHashSet<String>::new, LinkedHashSet::add, LinkedHashSet::addAll)
                .stream().toList();
    }

    @Override
    public void run(ApplicationArguments args) {
        var seeded = 0;
        for (var configured : configuredCodes) {
            try {
                var fingerprint = new ManagerCode(configured).fingerprint();
                if (codes.existsByFingerprint(fingerprint)) {
                    continue;
                }
                var now = clock.instant();
                var license = CooperativeLicense.grant(new CooperativeId(), ORGANIZATION_NAME, SEATS,
                        new HectaresQuota(CONTRACTED_AREA_HA),
                        new SubscriptionPeriod(now.minus(Duration.ofDays(1)), now.plus(TERM)));
                var code = license.issueManagerCode(fingerprint, license.snapshot().period().endsAt());
                licenses.save(license);
                codes.save(code);
                seeded++;
            } catch (IllegalArgumentException ex) {
                // The code itself is never logged: it would hand over a license to whoever reads the logs.
                log.warn("ManagerCodesSeeder skipped a configured code: {}", ex.getMessage());
            }
        }
        if (seeded > 0) {
            log.info("ManagerCodesSeeder granted {} test licenses with a manager code each.", seeded);
        }
    }
}
