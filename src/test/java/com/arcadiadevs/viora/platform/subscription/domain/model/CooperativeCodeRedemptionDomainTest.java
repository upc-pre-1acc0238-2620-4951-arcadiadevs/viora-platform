package com.arcadiadevs.viora.platform.subscription.domain.model;

import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InsufficientLicenseAreaException;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InvitationCodeNotRedeemableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCodeSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.events.CooperativeCodeRedeemedEvent;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CodeFingerprint;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.InvitationCodeStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PlanMode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Cooperative code redemption domain unit tests")
class CooperativeCodeRedemptionDomainTest {

    private static final Instant NOW = Instant.parse("2026-10-20T15:00:00Z");
    private static final Instant LICENSE_END = Instant.parse("2027-03-31T00:00:00Z");

    private final ProducerId producer = new ProducerId();
    private final CooperativeId cooperative = new CooperativeId();
    private final CooperativeLicense license = CooperativeLicense.grant(cooperative, "Coop. Olivarera La Yarada", 60,
            new HectaresQuota(new BigDecimal("200")), new SubscriptionPeriod(NOW.minus(Duration.ofDays(30)), LICENSE_END));
    private final InvitationCode code = license.issueCode(new CooperativeCode("COOP-7K2Q").fingerprint(),
            new HectaresQuota(new BigDecimal("12")), NOW.plus(Duration.ofDays(30)));

    @Test
    @DisplayName("A typed code is trimmed and uppercased, and its fingerprint is the SHA-256 of the normalized code")
    void codeIsNormalized() {
        var typed = new CooperativeCode("  coop-7k2q ");

        assertThat(typed.value()).isEqualTo("COOP-7K2Q");
        assertThat(typed.fingerprint().value())
                .isEqualTo("e39d8912d77b48b6c6183f47b1f9d7d2287f6a76cf66525078a062dabfa77d67");
        assertThat(typed.fingerprint()).isEqualTo(new CooperativeCode("COOP-7K2Q").fingerprint());
    }

    @Test
    @DisplayName("A code that is not COOP- followed by four letters or digits is rejected")
    void codeFormatIsEnforced() {
        for (var invalid : new String[]{"OLIV-4F9T", "COOP-12345", "COOP-7K2", "COOP7K2Q", "COOP-7K-Q", " "}) {
            assertThatThrownBy(() -> new CooperativeCode(invalid))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> new CooperativeCode(null))
                .hasMessage("subscription.invitation_code.blank");
    }

    @Test
    @DisplayName("Hectares are rounded to two decimals and must be at least 0.1")
    void quotaBounds() {
        assertThat(new HectaresQuota(new BigDecimal("12.345")).hectares()).isEqualByComparingTo("12.35");
        assertThat(new HectaresQuota(new BigDecimal("0.1")).hectares()).isEqualByComparingTo("0.10");
        assertThatThrownBy(() -> new HectaresQuota(new BigDecimal("0.09")))
                .hasMessage("subscription.quota.below_minimum");
        assertThatThrownBy(() -> new HectaresQuota(null)).hasMessage("subscription.quota.null");
    }

    @Test
    @DisplayName("Issuing a code commits its hectares on the license; the code keeps that quota")
    void issuingACodeCommitsItsHectares() {
        assertThat(code.snapshot().quota().hectares()).isEqualByComparingTo("12");
        assertThat(code.snapshot().status()).isEqualTo(InvitationCodeStatus.AVAILABLE);
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("12");
        assertThat(license.remainingAreaHa()).isEqualByComparingTo("188");
    }

    @Test
    @DisplayName("Redeeming an available code consumes it, keeps the license area and returns the code's quota")
    void redeemsAnAvailableCode() {
        var redeemed = license.redeem(code, producer, NOW);

        var codeState = code.snapshot();
        assertThat(codeState.status()).isEqualTo(InvitationCodeStatus.REDEEMED);
        assertThat(codeState.redeemedBy()).isEqualTo(producer);
        assertThat(codeState.redeemedAt()).isEqualTo(NOW);
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("12");
        assertThat(redeemed.quota().hectares()).isEqualByComparingTo("12");
        assertThat(redeemed.codeId()).isEqualTo(codeState.id());
        assertThat(redeemed.cooperativeId()).isEqualTo(cooperative);
        assertThat(redeemed.licensePeriod().endsAt()).isEqualTo(LICENSE_END);
    }

    @Test
    @DisplayName("A code is redeemed only once")
    void codeIsRedeemedOnlyOnce() {
        license.redeem(code, producer, NOW);

        assertThatThrownBy(() -> license.redeem(code, new ProducerId(), NOW))
                .isInstanceOf(InvitationCodeNotRedeemableException.class)
                .hasMessage("subscription.invitation_code.not_redeemable");
        assertThat(code.snapshot().redeemedBy()).isEqualTo(producer);
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("12");
    }

    @Test
    @DisplayName("An expired or revoked code cannot be redeemed and nothing changes")
    void expiredOrRevokedCodeIsNotRedeemable() {
        var atExpiry = code.snapshot().expiresAt();
        assertThatThrownBy(() -> license.redeem(code, producer, atExpiry))
                .isInstanceOf(InvitationCodeNotRedeemableException.class);
        assertThat(code.snapshot().status()).isEqualTo(InvitationCodeStatus.AVAILABLE);

        var state = code.snapshot();
        var revoked = InvitationCode.reconstitute(new InvitationCodeSnapshot(state.id(), state.licenseId(), null,
                state.fingerprint(), state.quota(), InvitationCodeStatus.REVOKED, state.expiresAt(), null, null, 1L));
        assertThatThrownBy(() -> license.redeem(revoked, producer, NOW))
                .isInstanceOf(InvitationCodeNotRedeemableException.class);
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("12");
    }

    @Test
    @DisplayName("A code of a license that is no longer in force cannot be redeemed")
    void codeOfAnEndedLicenseIsNotRedeemable() {
        var lateCode = license.issueCode(new CooperativeCode("COOP-9HX3").fingerprint(),
                new HectaresQuota(BigDecimal.ONE), LICENSE_END.plus(Duration.ofDays(10)));

        assertThatThrownBy(() -> license.redeem(lateCode, producer, LICENSE_END))
                .isInstanceOf(InvitationCodeNotRedeemableException.class);
        assertThat(lateCode.snapshot().status()).isEqualTo(InvitationCodeStatus.AVAILABLE);
    }

    @Test
    @DisplayName("Issuing commits the remaining hectares exactly, but never more, and a refusal changes nothing")
    void licenseAreaIsNeverExceeded() {
        var fingerprint = new CooperativeCode("COOP-4F9T").fingerprint();
        var expiresAt = NOW.plus(Duration.ofDays(30));
        var tooMuch = new HectaresQuota(new BigDecimal("188.01"));
        assertThatThrownBy(() -> license.issueCode(fingerprint, tooMuch, expiresAt))
                .isInstanceOf(InsufficientLicenseAreaException.class)
                .hasMessage("subscription.license.insufficient_area");
        assertThat(license.snapshot().committedAreaHa()).isEqualByComparingTo("12");

        license.issueCode(fingerprint, new HectaresQuota(new BigDecimal("188")), expiresAt);
        assertThat(license.remainingAreaHa()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("A code is redeemed only through the license it belongs to")
    void codeOfAnotherLicenseIsRejected() {
        var other = CooperativeLicense.grant(new CooperativeId(), "Olivar del Sur", 10,
                new HectaresQuota(BigDecimal.TEN), new SubscriptionPeriod(NOW, LICENSE_END));

        assertThatThrownBy(() -> other.redeem(code, producer, NOW))
                .hasMessage("subscription.license.code_of_another_license");
        assertThat(code.snapshot().status()).isEqualTo(InvitationCodeStatus.AVAILABLE);
    }

    @Test
    @DisplayName("A redeemed code activates a sponsored subscription until the license ends and publishes the event")
    void activatesASponsoredSubscription() {
        var redeemed = license.redeem(code, producer, NOW);

        var subscription = Subscription.activateFromCode(redeemed, NOW);

        var state = subscription.snapshot();
        assertThat(state.mode()).isEqualTo(PlanMode.PLAN_COOPERATIVO_PATROCINADO);
        assertThat(state.status()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(state.producerId()).isEqualTo(producer);
        assertThat(state.quota().hectares()).isEqualByComparingTo("12");
        assertThat(state.period()).isEqualTo(new SubscriptionPeriod(NOW, LICENSE_END));
        assertThat(state.cooperativeId()).isEqualTo(cooperative);
        assertThat(state.redeemedCodeId()).isEqualTo(code.snapshot().id());
        assertThat(subscription.hasActiveEntitlement(NOW)).isTrue();
        assertThat(subscription.hasActiveEntitlement(LICENSE_END)).isFalse();

        assertThat(subscription.domainEvents()).singleElement().isEqualTo(new CooperativeCodeRedeemedEvent(
                state.id().subscriptionId(), producer.producerId(), cooperative.cooperativeId(),
                code.snapshot().id().invitationCodeId(), NOW));
    }

    @Test
    @DisplayName("A fingerprint must be 64 lowercase hex characters")
    void fingerprintFormat() {
        assertThatThrownBy(() -> new CodeFingerprint("ABC"))
                .hasMessage("subscription.invitation_code.fingerprint.invalid");
    }
}
