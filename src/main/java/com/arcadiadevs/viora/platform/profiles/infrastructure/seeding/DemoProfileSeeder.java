package com.arcadiadevs.viora.platform.profiles.infrastructure.seeding;

import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.CountryCode;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.FullName;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.PhoneNumber;
import com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.profiles.domain.repositories.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Creates the profile of the demo account (see the IAM {@code DemoAccountSeeder}), so the app greets it by name like
 * any producer. Runs under the same condition: the demo account email and password are configured.
 */
@Component
@Order(0)
public class DemoProfileSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoProfileSeeder.class);

    private final ProfileRepository profileRepository;
    private final String producerId;
    private final boolean enabled;
    private final String fullName;
    private final String country;
    private final String phoneNumber;

    /**
     * Creates the seeder.
     *
     * @param profileRepository the profiles
     * @param producerId        the seeded demo producer, identity of the demo account
     * @param email             the demo account email; blank disables the seeder
     * @param password          the demo account password; blank disables the seeder
     * @param fullName          the name shown for the demo account
     * @param country           the country of the demo account (ISO-3166-1 alpha-2)
     * @param phoneNumber       the mobile number of the demo account (E.164)
     */
    public DemoProfileSeeder(
            ProfileRepository profileRepository,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String producerId,
            @Value("${viora.demo-account.email:}") String email,
            @Value("${viora.demo-account.password:}") String password,
            @Value("${viora.demo-account.full-name:Productor Demo}") String fullName,
            @Value("${viora.demo-account.country:PE}") String country,
            @Value("${viora.demo-account.phone-number:+51987654321}") String phoneNumber
    ) {
        this.profileRepository = profileRepository;
        this.producerId = producerId == null ? "" : producerId.trim();
        this.enabled = email != null && !email.isBlank() && password != null && !password.isBlank();
        this.fullName = fullName;
        this.country = country;
        this.phoneNumber = phoneNumber;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled || producerId.isEmpty()) {
            return;
        }
        try {
            var userId = new UserId(producerId);
            if (profileRepository.existsByUserId(userId)) {
                log.info("The demo account already has a profile. Skipping demo profile seeding.");
                return;
            }
            profileRepository.save(Profile.create(userId, new FullName(fullName), new CountryCode(country),
                    new PhoneNumber(phoneNumber)));
            log.info("DemoProfileSeeder created the profile of the demo account.");
        } catch (Exception ex) {
            log.error("Unexpected error during DemoProfileSeeder execution: {}", ex.getMessage(), ex);
        }
    }
}
