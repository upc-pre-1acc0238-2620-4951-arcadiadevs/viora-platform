package com.arcadiadevs.viora.platform.subscription.domain.model;

import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InvitationCodeNotReleasableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.DataSharingConsent;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.events.CooperativeLicenseRenewedEvent;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ConsentTextVersion;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.DataSharingConsentStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeStatus;
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

@DisplayName("License renewal and member release domain unit tests")
class LicenseRenewalAndMemberReleaseDomainTest {

    private static final Instant NOW = Instant.parse("2026-11-07T15:00:00Z");
    private static final Instant LICENSE_END = Instant.parse("2027-03-31T00:00:00Z");

    private final CooperativeLicense license = CooperativeLicense.grant(new CooperativeId(), "Coop. Olivar del Sur",
            60, new HectaresQuota(new BigDecimal("200")),
            new SubscriptionPeriod(NOW.minus(Duration.ofDays(30)), LICENSE_END));
    private final ProducerId member = new ProducerId();
    private final InvitationCode code = license.issueCode(new CooperativeCode("COOP-7K2Q").fingerprint(),
            new HectaresQuota(new BigDecimal("12")), NOW.plus(Duration.ofDays(7)));
    private final RedeemedCode redeemed = license.redeem(code, member, NOW);

    @Test
    @DisplayName("Renewing extends the license and tells its members' plans, never to an earlier end")
    void renewal() {
        var newEnd = LICENSE_END.plus(Duration.ofDays(365));

        license.renew(newEnd, NOW);

        assertThat(license.snapshot().period().endsAt()).isEqualTo(newEnd);
        assertThat(license.domainEvents()).singleElement().isInstanceOfSatisfying(CooperativeLicenseRenewedEvent.class,
                event -> {
                    assertThat(event.previousEndsAt()).isEqualTo(LICENSE_END);
                    assertThat(event.newEndsAt()).isEqualTo(newEnd);
                });
        assertThatThrownBy(() -> license.renew(LICENSE_END, NOW)).hasMessage("subscription.license.renewal.not_later");
    }

    @Test
    @DisplayName("A sponsored plan and its active consent follow a renewal; a withdrawn consent does not come back")
    void plansFollowTheRenewal() {
        var plan = Subscription.activateFromCode(redeemed, NOW);
        var consent = DataSharingConsent.accept(redeemed, plan, new ManagerId(), new ConsentTextVersion("v1"), NOW);
        var withdrawn = DataSharingConsent.accept(redeemed, plan, new ManagerId(), new ConsentTextVersion("v1"), NOW);
        withdrawn.withdraw(NOW.plusSeconds(1));
        var newEnd = LICENSE_END.plus(Duration.ofDays(365));

        plan.extendTo(newEnd);

        assertThat(plan.snapshot().period().endsAt()).isEqualTo(newEnd);
        assertThat(consent.extendTo(newEnd, NOW)).isTrue();
        assertThat(consent.snapshot().endsAt()).isEqualTo(newEnd);
        assertThat(withdrawn.extendTo(newEnd, NOW)).isFalse();
    }

    @Test
    @DisplayName("Releasing a member holds the seat through the grace, then frees it with its hectares, only once")
    void releaseWithGrace() {
        var graceEnd = NOW.plus(Duration.ofDays(30));
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("12");

        license.holdMemberSeat(code, graceEnd, NOW);

        assertThat(code.isSeatOverdue(graceEnd.minusSeconds(1))).isFalse();
        assertThat(code.statusAt(graceEnd.minusSeconds(1))).isEqualTo(InvitationCodeStatus.REDEEMED);
        assertThat(code.statusAt(graceEnd)).isEqualTo(InvitationCodeStatus.RELEASED);
        assertThat(license.releaseOverdueSeat(code, graceEnd.minusSeconds(1))).isFalse();
        assertThat(license.releaseOverdueSeat(code, graceEnd)).isTrue();
        assertThat(code.snapshot().status()).isEqualTo(InvitationCodeStatus.RELEASED);
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> license.holdMemberSeat(code, graceEnd, NOW))
                .isInstanceOf(InvitationCodeNotReleasableException.class);
    }

    @Test
    @DisplayName("A deleted member's seat goes back at once; the plan ends early and the reading ends now")
    void immediateReleaseAndEarlyEnd() {
        var plan = Subscription.activateFromCode(redeemed, NOW);
        var consent = DataSharingConsent.accept(redeemed, plan, new ManagerId(), new ConsentTextVersion("v1"), NOW);

        assertThat(license.releaseMemberCode(code, NOW)).isTrue();
        assertThat(license.releaseMemberCode(code, NOW)).isFalse();
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("0");

        plan.endEarly(NOW.plus(Duration.ofDays(30)));
        assertThat(plan.snapshot().period().endsAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        plan.endEarly(LICENSE_END);
        assertThat(plan.snapshot().period().endsAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));

        assertThat(consent.endWithRelease(NOW.plusSeconds(5))).isTrue();
        assertThat(consent.statusAt(NOW.plusSeconds(5))).isEqualTo(DataSharingConsentStatus.ENDED);
        assertThat(consent.endWithRelease(NOW.plusSeconds(6))).isFalse();
    }
}
