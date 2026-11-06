package com.arcadiadevs.viora.platform.subscription.domain.model;

import com.arcadiadevs.viora.platform.subscription.domain.exceptions.DataSharingConsentNotActiveException;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.DataSharingConsent;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ConsentTextVersion;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.DataSharingConsentStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.RedeemedCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Data sharing consent domain unit tests")
class DataSharingConsentDomainTest {

    private static final Instant NOW = Instant.parse("2026-11-06T15:00:00Z");
    private static final Instant LICENSE_END = Instant.parse("2027-03-31T00:00:00Z");

    private final ProducerId producer = new ProducerId();
    private final ManagerId manager = new ManagerId();
    private final ConsentTextVersion version = new ConsentTextVersion("2026-11-v1");

    @Test
    @DisplayName("Accepting records who, with which license and plan, the text version and an end equal to the plan's")
    void acceptRecordsTheConsent() {
        var redeemed = redeem();
        var plan = Subscription.activateFromCode(redeemed, NOW);

        var consent = DataSharingConsent.accept(redeemed, plan, manager, version, NOW);

        var snapshot = consent.snapshot();
        assertThat(snapshot.producerId()).isEqualTo(producer);
        assertThat(snapshot.licenseId()).isEqualTo(redeemed.licenseId());
        assertThat(snapshot.subscriptionId()).isEqualTo(plan.snapshot().id());
        assertThat(snapshot.managerId()).isEqualTo(manager);
        assertThat(snapshot.textVersion()).isEqualTo(version);
        assertThat(snapshot.acceptedAt()).isEqualTo(NOW);
        assertThat(snapshot.endsAt()).isEqualTo(LICENSE_END);
        assertThat(snapshot.withdrawnAt()).isNull();
        assertThat(consent.statusAt(NOW)).isEqualTo(DataSharingConsentStatus.ACTIVE);
    }

    @Test
    @DisplayName("A consent ends with its plan")
    void endsWithThePlan() {
        var redeemed = redeem();
        var consent = DataSharingConsent.accept(redeemed, Subscription.activateFromCode(redeemed, NOW), manager,
                version, NOW);

        assertThat(consent.statusAt(LICENSE_END.minusSeconds(1))).isEqualTo(DataSharingConsentStatus.ACTIVE);
        assertThat(consent.statusAt(LICENSE_END)).isEqualTo(DataSharingConsentStatus.ENDED);
        assertThatThrownBy(() -> consent.withdraw(LICENSE_END))
                .isInstanceOf(DataSharingConsentNotActiveException.class);
    }

    @Test
    @DisplayName("Withdrawing is for good: a second withdrawal is refused and the first instant stays")
    void withdrawIsForGood() {
        var redeemed = redeem();
        var consent = DataSharingConsent.accept(redeemed, Subscription.activateFromCode(redeemed, NOW), manager,
                version, NOW);
        var later = NOW.plus(Duration.ofDays(3));

        consent.withdraw(later);

        assertThat(consent.snapshot().withdrawnAt()).isEqualTo(later);
        assertThat(consent.statusAt(later)).isEqualTo(DataSharingConsentStatus.WITHDRAWN);
        assertThat(consent.statusAt(LICENSE_END.plusSeconds(1))).isEqualTo(DataSharingConsentStatus.WITHDRAWN);
        assertThatThrownBy(() -> consent.withdraw(later.plusSeconds(1)))
                .isInstanceOf(DataSharingConsentNotActiveException.class)
                .hasMessage("subscription.consent.not_active");
        assertThat(consent.snapshot().withdrawnAt()).isEqualTo(later);
    }

    @Test
    @DisplayName("A consent cannot be tied to the plan of another redemption")
    void planMustBeTheOneRedeemed() {
        var redeemed = redeem();
        var otherPlan = Subscription.activateFromCode(redeem(), NOW);

        assertThatThrownBy(() -> DataSharingConsent.accept(redeemed, otherPlan, manager, version, NOW))
                .hasMessage("subscription.consent.plan_of_another_redemption");
    }

    @Test
    @DisplayName("A license without a manager still records the consent, with no manager")
    void licenseWithoutManager() {
        var redeemed = redeem();

        var consent = DataSharingConsent.accept(redeemed, Subscription.activateFromCode(redeemed, NOW), null,
                version, NOW);

        assertThat(consent.snapshot().managerId()).isNull();
    }

    @Test
    @DisplayName("The text version is letters, digits, dots and hyphens, up to 40")
    void textVersionFormat() {
        assertThat(new ConsentTextVersion(" 2026-11-v1 ").value()).isEqualTo("2026-11-v1");
        assertThatThrownBy(() -> new ConsentTextVersion(" "))
                .hasMessage("subscription.consent.text_version.blank");
        assertThatThrownBy(() -> new ConsentTextVersion("v1; drop"))
                .hasMessage("subscription.consent.text_version.invalid");
        assertThatThrownBy(() -> new ConsentTextVersion("v".repeat(41)))
                .hasMessage("subscription.consent.text_version.invalid");
    }

    private RedeemedCode redeem() {
        var license = CooperativeLicense.grant(new CooperativeId(), "Coop. Olivarera La Yarada", 60,
                new HectaresQuota(new BigDecimal("200")),
                new SubscriptionPeriod(NOW.minus(Duration.ofDays(30)), LICENSE_END));
        var code = license.issueCode(new CooperativeCode("COOP-7K2Q").fingerprint(),
                new HectaresQuota(new BigDecimal("12")), NOW.plus(Duration.ofDays(30)));
        return license.redeem(code, producer, NOW);
    }
}
