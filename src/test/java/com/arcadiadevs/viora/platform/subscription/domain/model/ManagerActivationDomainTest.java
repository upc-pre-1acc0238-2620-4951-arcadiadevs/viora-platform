package com.arcadiadevs.viora.platform.subscription.domain.model;

import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.ManagerCodeNotRedeemableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.ManagerActivationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Manager activation domain unit tests")
class ManagerActivationDomainTest {

    private static final Instant NOW = Instant.parse("2026-11-05T15:00:00Z");
    private static final Instant LICENSE_END = Instant.parse("2027-12-31T05:00:00Z");

    private final CooperativeLicense license = newLicense(NOW.minus(Duration.ofDays(1)), LICENSE_END);
    private final ManagerActivationCode code = license.issueManagerCode(
            new ManagerCode("GEST-7K2Q").fingerprint(), NOW.plus(Duration.ofDays(30)));
    private final ManagerId ruben = new ManagerId();

    @Test
    @DisplayName("A typed code is trimmed and uppercased, and differs from a cooperative code with the same body")
    void codeIsNormalized() {
        var typed = new ManagerCode("  gest-7k2q ");

        assertThat(typed.value()).isEqualTo("GEST-7K2Q");
        assertThat(typed.fingerprint()).isEqualTo(new ManagerCode("GEST-7K2Q").fingerprint());
        assertThat(typed.fingerprint()).isNotEqualTo(new CooperativeCode("COOP-7K2Q").fingerprint());
    }

    @Test
    @DisplayName("A code that is not GEST- followed by four letters or digits is rejected")
    void codeFormatIsEnforced() {
        for (var invalid : new String[]{"COOP-7K2Q", "GEST-12345", "GEST-7K2", "GEST7K2Q", "OLIV-7K2Q", " "}) {
            assertThatThrownBy(() -> new ManagerCode(invalid)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new ManagerCode(null)).hasMessage("subscription.manager_code.blank");
        assertThatThrownBy(() -> new ManagerCode("GEST-7K"))
                .hasMessage("subscription.manager_code.invalid_format");
    }

    @Test
    @DisplayName("A new license has no manager and a new code is available")
    void issuedCodeIsAvailable() {
        assertThat(license.snapshot().managerId()).isNull();
        assertThat(code.snapshot().status()).isEqualTo(ManagerCodeStatus.AVAILABLE);
        assertThat(code.snapshot().licenseId()).isEqualTo(license.snapshot().id());
        assertThat(code.isRedeemable(NOW)).isTrue();
    }

    @Test
    @DisplayName("A code never outlives its license")
    void codeCannotOutliveTheLicense() {
        assertThatThrownBy(() -> license.issueManagerCode(new ManagerCode("GEST-AAAA").fingerprint(),
                LICENSE_END.plusSeconds(1)))
                .hasMessage("subscription.manager_code.expires_after_license");
        assertThat(license.issueManagerCode(new ManagerCode("GEST-AAAA").fingerprint(), LICENSE_END)
                .snapshot().expiresAt()).isEqualTo(LICENSE_END);
    }

    @Test
    @DisplayName("Redeeming a code puts the manager in charge and spends the code")
    void redeemingPutsTheManagerInCharge() {
        license.assignManager(code, ruben, NOW);

        assertThat(license.snapshot().managerId()).isEqualTo(ruben);
        assertThat(code.snapshot().status()).isEqualTo(ManagerCodeStatus.REDEEMED);
        assertThat(code.snapshot().redeemedBy()).isEqualTo(ruben);
        assertThat(code.snapshot().redeemedAt()).isEqualTo(NOW);
        assertThat(code.isRedeemable(NOW)).isFalse();
    }

    @Test
    @DisplayName("A used, expired or revoked code, or a license out of force, cannot be redeemed and changes nothing")
    void codesThatCannotBeRedeemed() {
        license.assignManager(code, ruben, NOW);
        assertThatThrownBy(() -> license.assignManager(code, new ManagerId(), NOW))
                .isInstanceOf(ManagerCodeNotRedeemableException.class);

        var expiring = license.issueManagerCode(new ManagerCode("GEST-BBBB").fingerprint(), NOW.plusSeconds(60));
        assertThatThrownBy(() -> license.assignManager(expiring, new ManagerId(), NOW.plusSeconds(60)))
                .isInstanceOf(ManagerCodeNotRedeemableException.class);

        var revoked = license.issueManagerCode(new ManagerCode("GEST-CCCC").fingerprint(), LICENSE_END);
        revoked.revoke();
        assertThatThrownBy(() -> license.assignManager(revoked, new ManagerId(), NOW))
                .isInstanceOf(ManagerCodeNotRedeemableException.class);

        var ended = newLicense(NOW.minus(Duration.ofDays(400)), NOW);
        var late = ended.issueManagerCode(new ManagerCode("GEST-DDDD").fingerprint(), NOW);
        assertThatThrownBy(() -> ended.assignManager(late, new ManagerId(), NOW))
                .isInstanceOf(ManagerCodeNotRedeemableException.class);
        assertThat(late.snapshot().status()).isEqualTo(ManagerCodeStatus.AVAILABLE);
        assertThat(ended.snapshot().managerId()).isNull();
        assertThat(license.snapshot().managerId()).isEqualTo(ruben);
    }

    @Test
    @DisplayName("A new code replaces the manager: the previous one is no longer in charge")
    void aNewCodeReplacesTheManager() {
        license.assignManager(code, ruben, NOW);
        var replacement = license.issueManagerCode(new ManagerCode("GEST-EEEE").fingerprint(), LICENSE_END);
        var rosa = new ManagerId();

        license.assignManager(replacement, rosa, NOW.plusSeconds(10));

        assertThat(license.snapshot().managerId()).isEqualTo(rosa);
        assertThat(license.releaseManager(ruben)).isFalse();
        assertThat(license.snapshot().managerId()).isEqualTo(rosa);
    }

    @Test
    @DisplayName("Releasing the manager in charge leaves the license without one")
    void releasingTheManager() {
        license.assignManager(code, ruben, NOW);

        assertThat(license.releaseManager(ruben)).isTrue();
        assertThat(license.snapshot().managerId()).isNull();
    }

    @Test
    @DisplayName("Only an available code can be revoked")
    void revoking() {
        license.assignManager(code, ruben, NOW);

        assertThatThrownBy(code::revoke).isInstanceOf(BusinessRuleException.class)
                .hasMessage("subscription.manager_code.not_revocable");
    }

    @Test
    @DisplayName("A code of another license is refused")
    void codeOfAnotherLicense() {
        var other = newLicense(NOW.minus(Duration.ofDays(1)), LICENSE_END);

        assertThatThrownBy(() -> other.assignManager(code, ruben, NOW))
                .hasMessage("subscription.license.code_of_another_license");
    }

    private static CooperativeLicense newLicense(Instant start, Instant end) {
        return CooperativeLicense.grant(new CooperativeId(), "Cooperativa Olivar del Sur", 60,
                new HectaresQuota(new BigDecimal("200")), new SubscriptionPeriod(start, end));
    }
}
