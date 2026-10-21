package com.arcadiadevs.viora.platform.iam.infrastructure.seeding;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccountSnapshot;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHistory;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TermsAcceptance;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserAccountStatus;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Creates the demo account: a real, verified producer account whose identity is the seeded demo producer, so the demo
 * data (plots, harvests, alerts...) can be shown by signing in with it.
 *
 * <p>It only runs when both {@code VIORA_DEMO_ACCOUNT_EMAIL} and {@code VIORA_DEMO_ACCOUNT_PASSWORD} are set, so no
 * credentials live in the code. An existing account is never changed: delete it to seed it again with other
 * credentials. Its profile is created by the Profiles context ({@code DemoProfileSeeder}).</p>
 */
@Component
@Order(0)
public class DemoAccountSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoAccountSeeder.class);

    private final UserAccountRepository userAccountRepository;
    private final HashingService hashingService;
    private final Clock clock;
    private final String producerId;
    private final String email;
    private final String password;
    private final String termsVersion;

    /**
     * Creates the seeder.
     *
     * @param userAccountRepository the user accounts
     * @param hashingService        hashes the demo password
     * @param clock                 the application clock
     * @param producerId            the seeded demo producer, which becomes the account identity
     * @param email                 the demo account email; blank disables the seeder
     * @param password              the demo account password; blank disables the seeder
     * @param termsVersion          the terms version recorded as accepted
     */
    public DemoAccountSeeder(
            UserAccountRepository userAccountRepository,
            HashingService hashingService,
            Clock clock,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String producerId,
            @Value("${viora.demo-account.email:}") String email,
            @Value("${viora.demo-account.password:}") String password,
            @Value("${viora.iam.terms-version}") String termsVersion
    ) {
        this.userAccountRepository = userAccountRepository;
        this.hashingService = hashingService;
        this.clock = clock;
        this.producerId = producerId == null ? "" : producerId.trim();
        this.email = email == null ? "" : email.trim().toLowerCase();
        this.password = password == null ? "" : password;
        this.termsVersion = termsVersion;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isEmpty() || password.isBlank() || producerId.isEmpty()) {
            return;
        }
        try {
            var userId = new UserId(producerId);
            var emailAddress = new EmailAddress(email);
            if (userAccountRepository.findById(userId).isPresent()) {
                log.info("The demo account already exists. Skipping demo account seeding.");
                return;
            }
            if (userAccountRepository.existsByEmail(emailAddress)) {
                log.warn("Another account already uses the demo account email. Skipping demo account seeding.");
                return;
            }
            var now = clock.instant();
            var account = UserAccount.reconstitute(new UserAccountSnapshot(
                    userId,
                    emailAddress,
                    new PasswordHash(hashingService.encode(password)),
                    PasswordHistory.empty(),
                    0L,
                    UserRole.ROLE_PRODUCER,
                    UserAccountStatus.ACTIVE,
                    null,
                    new TermsAcceptance(termsVersion, now),
                    null,
                    now,
                    now));
            userAccountRepository.save(account);
            log.info("DemoAccountSeeder created the demo account on producer {}.", producerId);
        } catch (Exception ex) {
            log.error("Unexpected error during DemoAccountSeeder execution: {}", ex.getMessage(), ex);
        }
    }
}
