package com.arcadiadevs.viora.platform.subscription.infrastructure;

import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.ManagerActivationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.CooperativeLicensePersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.persistence.jpa.repositories.ManagerActivationCodePersistenceRepository;
import com.arcadiadevs.viora.platform.subscription.infrastructure.seeding.ManagerCodesSeeder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Manager codes seeded from configuration: a test license of its own for each well formed code, once.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:managercodesseeder;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "viora.subscription.manager-seed-codes= gest-tst1 ,GEST-TST2,not-a-code,GEST-TST1"
})
@DisplayName("Manager codes seeder")
class ManagerCodesSeederIntegrationTest {

    @Autowired ManagerCodesSeeder seeder;
    @Autowired ManagerActivationCodeRepository codes;
    @Autowired ManagerActivationCodePersistenceRepository codeRows;
    @Autowired CooperativeLicensePersistenceRepository licenseRows;

    @Test
    @DisplayName("each well formed code gets an available code on a test license of its own, and a rerun adds nothing")
    void seedsEachCodeOnce() {
        for (var code : new String[]{"GEST-TST1", "GEST-TST2"}) {
            assertThat(codes.existsByFingerprint(new ManagerCode(code).fingerprint())).as(code).isTrue();
        }
        var rows = codeRows.findAll();
        assertThat(rows).hasSize(2).allSatisfy(row -> {
            assertThat(row.getStatus()).isEqualTo(ManagerCodeStatus.AVAILABLE);
            var license = licenseRows.findById(row.getLicenseId()).orElseThrow();
            assertThat(license.getCooperativeName()).isEqualTo(ManagerCodesSeeder.ORGANIZATION_NAME);
            assertThat(license.getSeats()).isEqualTo(ManagerCodesSeeder.SEATS);
            assertThat(license.getContractedAreaHa()).isEqualByComparingTo(ManagerCodesSeeder.CONTRACTED_AREA_HA);
            assertThat(license.getManagerUserId()).isNull();
            assertThat(row.getExpiresAt()).isEqualTo(license.getEndsAt());
        });
        assertThat(rows.stream().map(row -> row.getLicenseId()).distinct().map(UUID::toString)).hasSize(2);

        var licensesBefore = licenseRows.count();
        seeder.run(null);

        assertThat(codeRows.count()).isEqualTo(2);
        assertThat(licenseRows.count()).isEqualTo(licensesBefore);
    }
}
