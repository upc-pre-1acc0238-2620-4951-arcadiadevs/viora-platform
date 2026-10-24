package com.arcadiadevs.viora.platform.subscription.infrastructure.seeding;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.QuotaExtensionCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/**
 * Extension codes for the demo and the team, while there is no Gestor screen to share them.
 * <p>The license of the demo code {@code COOP-7K2Q} (the one the demo account redeemed) gets {@link #DEMO_CODE}, which
 * adds {@link #DEMO_HECTARES} hectares, so the demo account has room for its plots. The test license of
 * {@link SubscriptionTestCodesSeeder} gets small codes for trying the flow. Each license first grows its contracted
 * area so the codes have hectares to commit. A code is seeded once, while its fingerprint is unknown, under the same
 * {@code viora.subscription.seed.enabled} switch (on only in the demo profile).</p>
 */
@Component
@Order(5)
public class SubscriptionExtensionCodesSeeder implements ApplicationRunner {

    /** The extension code the demo account redeems. */
    public static final String DEMO_CODE = "AMPL-D500";
    /** The hectares {@link #DEMO_CODE} adds to a plan. */
    public static final BigDecimal DEMO_HECTARES = new BigDecimal("500");
    /** Small test codes on the test license, with the hectares each adds. */
    public static final String TEST_CODE_SMALL = "AMPL-T005";
    public static final String TEST_CODE_LARGE = "AMPL-T020";

    private static final String DEMO_LICENSE_CODE = "COOP-7K2Q";
    private static final Logger log = LoggerFactory.getLogger(SubscriptionExtensionCodesSeeder.class);

    private final CooperativeLicenseRepository licenses;
    private final InvitationCodeRepository invitationCodes;
    private final QuotaExtensionCodeRepository extensionCodes;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final boolean enabled;

    /**
     * Creates the seeder.
     *
     * @param licenses           the cooperative licenses
     * @param invitationCodes    the invitation codes, to find the demo license
     * @param extensionCodes     the extension codes
     * @param transactionManager the transaction manager; the license lookups lock rows
     * @param clock              the application clock
     * @param enabled            whether the seed runs
     */
    public SubscriptionExtensionCodesSeeder(
            CooperativeLicenseRepository licenses,
            InvitationCodeRepository invitationCodes,
            QuotaExtensionCodeRepository extensionCodes,
            PlatformTransactionManager transactionManager,
            Clock clock,
            @Value("${viora.subscription.seed.enabled:true}") boolean enabled
    ) {
        this.licenses = licenses;
        this.invitationCodes = invitationCodes;
        this.extensionCodes = extensionCodes;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            return;
        }
        try {
            transaction.executeWithoutResult(status -> {
                demoLicense().ifPresent(license -> seed(license, DEMO_CODE, DEMO_HECTARES, 2));
                licenses.findByIdForRedemption(SubscriptionTestCodesSeeder.LICENSE_ID).ifPresent(license -> {
                    seed(license, TEST_CODE_SMALL, new BigDecimal("5"), 10);
                    seed(license, TEST_CODE_LARGE, new BigDecimal("20"), 10);
                });
            });
        } catch (Exception ex) {
            log.error("Unexpected error during SubscriptionExtensionCodesSeeder execution: {}", ex.getMessage(), ex);
        }
    }

    private Optional<CooperativeLicense> demoLicense() {
        return invitationCodes.findByFingerprintForRedemption(new CooperativeCode(DEMO_LICENSE_CODE).fingerprint())
                .flatMap(code -> licenses.findByIdForRedemption(code.snapshot().licenseId()));
    }

    /**
     * Grows the license for {@code redemptions} uses of the code and issues it, unless it already exists.
     */
    private void seed(CooperativeLicense license, String code, BigDecimal hectares, int redemptions) {
        var fingerprint = new ExtensionCode(code).fingerprint();
        if (extensionCodes.findByFingerprint(fingerprint).isPresent()) {
            return;
        }
        license.addContractedArea(new HectaresQuota(hectares.multiply(BigDecimal.valueOf(redemptions))));
        var issued = license.issueExtensionCode(fingerprint, new HectaresQuota(hectares),
                clock.instant().plus(Duration.ofDays(365)));
        licenses.save(license);
        extensionCodes.save(issued);
        log.info("SubscriptionExtensionCodesSeeder issued {} on license {}.", code,
                license.snapshot().id().cooperativeLicenseId());
    }
}
