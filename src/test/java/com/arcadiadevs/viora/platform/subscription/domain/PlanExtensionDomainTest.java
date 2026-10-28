package com.arcadiadevs.viora.platform.subscription.domain;

import com.arcadiadevs.viora.platform.subscription.domain.exceptions.ExtensionCodeNotRedeemableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.QuotaExtension;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.Subscription;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.subscription.domain.services.PlanExtensionPolicy;
import com.arcadiadevs.viora.platform.subscription.domain.services.SubscriptionPricingPolicy;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class PlanExtensionDomainTest {

    private static final Instant NOW = Instant.parse("2028-03-01T12:00:00Z");
    private final SubscriptionPricingPolicy pricing = new SubscriptionPricingPolicy(new BigDecimal("55"), new BigDecimal("0.18"));

    @Test
    void billsCompleteMonthsLeftAndRefusesTheRenewalWindow() {
        var period = new SubscriptionPeriod(Instant.parse("2028-01-01T12:00:00Z"), Instant.parse("2029-01-01T12:00:00Z"));
        assertEquals(10, PlanExtensionPolicy.billableMonths(period, NOW));
        assertEquals(9, PlanExtensionPolicy.billableMonths(period, NOW.plus(Duration.ofDays(2))));
        assertFalse(PlanExtensionPolicy.isInRenewalWindow(period, NOW));
        assertTrue(PlanExtensionPolicy.isInRenewalWindow(period, period.endsAt().minus(Duration.ofDays(30))));
        assertEquals(1, PlanExtensionPolicy.billableMonths(period, period.endsAt().minus(Duration.ofDays(3))));
    }

    @Test
    void pricesOnlyTheAddedHectaresForTheDaysLeft() {
        // 3 ha x (55 x 12 x 300 / 365 = 542.465753 per ha) = 1627.397259 -> 1627.40
        assertEquals(new BigDecimal("542.465753"), pricing.extensionRatePerHa(300, 365));
        var price = pricing.quoteExtension(new IndividualHectaresQuota(new BigDecimal("3")), 300, 365);
        assertEquals(new Money(new BigDecimal("1627.40")), price.total());
        assertEquals(price.total().amount(), price.base().amount().add(price.tax().amount()));
        assertThrows(IllegalArgumentException.class,
                () -> pricing.quoteExtension(new IndividualHectaresQuota(new BigDecimal("3")), 0, 365));
        assertThrows(IllegalArgumentException.class,
                () -> pricing.quoteExtension(new IndividualHectaresQuota(new BigDecimal("3")), 366, 365));
    }

    @Test
    void aWholePlanCostsTheSameAsBuyingItAndAStartedDayCountsWhole() {
        assertEquals(new Money(new BigDecimal("1980.00")),
                pricing.quoteExtension(new IndividualHectaresQuota(new BigDecimal("3")), 365, 365).total());
        var period = new SubscriptionPeriod(Instant.parse("2028-01-01T12:00:00Z"), Instant.parse("2029-01-01T12:00:00Z"));
        assertEquals(366, PlanExtensionPolicy.periodDays(period));
        assertEquals(1, PlanExtensionPolicy.billableDays(period, period.endsAt().minusSeconds(60)));
        assertEquals(2, PlanExtensionPolicy.billableDays(period, period.endsAt().minus(Duration.ofHours(25))));
    }

    @Test
    void paidExtensionAppliesOnceAndKeepsTheEndDate() {
        var plan = activeIndividual(new BigDecimal("12"), NOW.minus(Duration.ofDays(60)));
        var end = plan.snapshot().period().endsAt();
        var added = new IndividualHectaresQuota(new BigDecimal("3"));
        var price = pricing.quoteExtension(added, 300, 365);
        var extension = QuotaExtension.requestPaid(plan, added, 10, price, NOW);
        assertEquals(QuotaExtensionStatus.PENDING_PAYMENT, extension.snapshot().status());

        assertTrue(extension.approvePayment("77", price.total(), "PEN", NOW.plusSeconds(5)));
        plan.extendQuota(extension.snapshot().addedArea());
        assertFalse(extension.approvePayment("77", price.total(), "PEN", NOW.plusSeconds(5)));
        assertThrows(IllegalStateException.class,
                () -> extension.approvePayment("78", price.total(), "PEN", NOW.plusSeconds(5)));
        assertEquals(new BigDecimal("15.00"), plan.snapshot().quota().hectares());
        assertEquals(end, plan.snapshot().period().endsAt());
    }

    @Test
    void paidExtensionIsRefusedNearTheEnd() {
        var plan = activeIndividual(new BigDecimal("12"), NOW.minus(Duration.ofDays(350)));
        var added = new IndividualHectaresQuota(new BigDecimal("1"));
        assertThrows(IllegalStateException.class,
                () -> QuotaExtension.requestPaid(plan, added, 1, pricing.quoteExtension(added, 15, 365), NOW));
    }

    @Test
    void renewalStartsWhenTheCurrentPlanEnds() {
        var quota = new IndividualHectaresQuota(new BigDecimal("12"));
        var price = pricing.quote(quota);
        var renewal = Subscription.createIndividual(new ProducerId(java.util.UUID.randomUUID().toString()),
                "PRODUCER_ANNUAL", quota, price, NOW);
        var currentEnd = NOW.plus(Duration.ofDays(10));
        renewal.activateIndividual("90", price.total(), "PEN", NOW.plusSeconds(1), currentEnd);
        assertEquals(currentEnd, renewal.snapshot().period().startsAt());
        assertFalse(renewal.hasActiveEntitlement(NOW.plusSeconds(2)));
        assertTrue(renewal.hasActiveEntitlement(currentEnd));
    }

    @Test
    void extensionCodeCommitsLicenseHectaresOnlyForItsOwnSponsoredPlans() {
        var license = CooperativeLicense.grant(new CooperativeId(), "Coop", 5, new HectaresQuota(new BigDecimal("20")),
                new SubscriptionPeriod(NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(300))));
        var invitation = license.issueCode(new CooperativeCode("COOP-AB12").fingerprint(),
                new HectaresQuota(new BigDecimal("12")), NOW.plus(Duration.ofDays(5)));
        var plan = Subscription.activateFromCode(license.redeem(invitation, new ProducerId(java.util.UUID.randomUUID().toString()), NOW), NOW);
        var code = license.issueExtensionCode(new ExtensionCode("ampl-ab12").fingerprint(),
                new HectaresQuota(new BigDecimal("5")), NOW.plus(Duration.ofDays(5)));

        var extension = license.redeemExtensionCode(code, plan, NOW);
        assertEquals(QuotaExtensionSource.CODE, extension.snapshot().source());
        assertEquals(new BigDecimal("17.00"), plan.snapshot().quota().hectares());
        assertEquals(new BigDecimal("3.00"), license.remainingAreaHa());
        // Only 3 ha left on the license: a second redemption cannot commit 5.
        assertThrows(ExtensionCodeNotRedeemableException.class, () -> license.redeemExtensionCode(code, plan, NOW));

        var individual = activeIndividual(new BigDecimal("2"), NOW.minus(Duration.ofDays(10)));
        assertThrows(ExtensionCodeNotRedeemableException.class, () -> license.redeemExtensionCode(code, individual, NOW));
        license.addContractedArea(new HectaresQuota(new BigDecimal("10")));
        assertEquals(new BigDecimal("13.00"), license.remainingAreaHa());
    }

    private Subscription activeIndividual(BigDecimal hectares, Instant approvedAt) {
        var quota = new IndividualHectaresQuota(hectares);
        var price = pricing.quote(quota);
        var plan = Subscription.createIndividual(new ProducerId(java.util.UUID.randomUUID().toString()),
                "PRODUCER_ANNUAL", quota, price, approvedAt.minusSeconds(10));
        plan.activateIndividual("1" + Math.abs(approvedAt.getEpochSecond()), price.total(), "PEN", approvedAt);
        return plan;
    }
}
