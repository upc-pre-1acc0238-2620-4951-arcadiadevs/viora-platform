package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ExtensionCodeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.IndividualHectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.Money;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PlanMode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionSource;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.QuotaExtensionStatus;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPrice;
import com.arcadiadevs.viora.platform.subscription.domain.services.PlanExtensionPolicy;

import java.time.Instant;

/**
 * Aggregate root of hectares added to a plan in force without changing when it ends.
 * <p>A paid extension of an individual plan waits for its payment and adds its hectares when the payment is approved.
 * An extension from a code of the sponsoring license is applied as soon as it is redeemed. Either way the record stays,
 * so the producer's plan keeps the history of how it grew.</p>
 */
public class QuotaExtension extends AbstractDomainAggregateRoot<QuotaExtension> {

    private final QuotaExtensionId id;
    private final SubscriptionId subscriptionId;
    private final ProducerId producerId;
    private final QuotaExtensionSource source;
    private final HectaresQuota addedArea;
    private QuotaExtensionStatus status;
    private final Integer billedMonths;
    private final SubscriptionPrice price;
    private final ExtensionCodeId extensionCodeId;
    private String paymentId;
    private final Instant createdAt;
    private Instant appliedAt;
    private final Long revision;

    private QuotaExtension(QuotaExtensionSnapshot snapshot) {
        this.id = snapshot.id();
        this.subscriptionId = snapshot.subscriptionId();
        this.producerId = snapshot.producerId();
        this.source = snapshot.source();
        this.addedArea = snapshot.addedArea();
        this.status = snapshot.status();
        this.billedMonths = snapshot.billedMonths();
        this.price = snapshot.price();
        this.extensionCodeId = snapshot.extensionCodeId();
        this.paymentId = snapshot.paymentId();
        this.createdAt = snapshot.createdAt();
        this.appliedAt = snapshot.appliedAt();
        this.revision = snapshot.revision();
    }

    /**
     * Requests a paid extension of an individual plan in force, outside its renewal window.
     *
     * @param plan         the individual plan that grows
     * @param addedQuota   the hectares to add, in half-hectare steps
     * @param billedMonths the complete months left of the plan
     * @param price        the server-calculated terms for those hectares and months
     * @param at           the instant of the request
     * @return the extension waiting for its payment
     * @throws IllegalArgumentException if an argument is missing
     * @throws IllegalStateException    if the plan is not an individual plan in force or is in its renewal window
     */
    public static QuotaExtension requestPaid(Subscription plan, IndividualHectaresQuota addedQuota, int billedMonths,
                                             SubscriptionPrice price, Instant at) {
        if (plan == null || addedQuota == null || price == null || at == null || billedMonths < 1) {
            throw new IllegalArgumentException("subscription.extension.incomplete");
        }
        var current = plan.snapshot();
        if (current.mode() != PlanMode.PLAN_PRODUCTOR_INDIVIDUAL || !plan.hasActiveEntitlement(at)) {
            throw new IllegalStateException("subscription.extension.not_active");
        }
        if (PlanExtensionPolicy.isInRenewalWindow(current.period(), at)) {
            throw new IllegalStateException("subscription.extension.renewal_window");
        }
        return new QuotaExtension(new QuotaExtensionSnapshot(new QuotaExtensionId(), current.id(),
                current.producerId(), QuotaExtensionSource.PAYMENT, new HectaresQuota(addedQuota.value()),
                QuotaExtensionStatus.PENDING_PAYMENT, billedMonths, price, null, null, at, null, 0L));
    }

    /**
     * Records the hectares a producer added with an extension code. Only {@link CooperativeLicense#redeemExtensionCode}
     * calls it, after the license committed the hectares and the plan grew.
     *
     * @param plan the sponsored plan that grew
     * @param code the redeemed code
     * @param at   the instant of the redemption
     * @return the applied extension
     */
    static QuotaExtension fromCode(Subscription plan, QuotaExtensionCode code, Instant at) {
        var current = plan.snapshot();
        return new QuotaExtension(new QuotaExtensionSnapshot(new QuotaExtensionId(), current.id(),
                current.producerId(), QuotaExtensionSource.CODE, code.snapshot().addedArea(),
                QuotaExtensionStatus.APPLIED, null, null, code.snapshot().id(), null, at, at, 0L));
    }

    /**
     * Rebuilds an extension from its persisted state.
     *
     * @param snapshot the persisted state
     * @return the extension
     * @throws IllegalArgumentException if the snapshot is null or misses a required part
     */
    public static QuotaExtension reconstitute(QuotaExtensionSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("subscription.extension.snapshot.null");
        }
        if (snapshot.id() == null || snapshot.subscriptionId() == null || snapshot.producerId() == null
                || snapshot.source() == null || snapshot.addedArea() == null || snapshot.status() == null
                || snapshot.createdAt() == null) {
            throw new IllegalArgumentException("subscription.extension.snapshot.incomplete");
        }
        var paid = snapshot.source() == QuotaExtensionSource.PAYMENT;
        if (paid && (snapshot.price() == null || snapshot.billedMonths() == null)
                || !paid && snapshot.extensionCodeId() == null) {
            throw new IllegalArgumentException("subscription.extension.snapshot.incomplete");
        }
        return new QuotaExtension(snapshot);
    }

    /**
     * Applies a paid extension with verified payment evidence; the caller then adds the hectares to the plan. A repeated
     * notification of the same payment changes nothing.
     *
     * @param paymentId  provider payment identity
     * @param amount     verified total
     * @param currency   verified settlement currency
     * @param approvedAt verified approval time
     * @return true if this call applied the extension, false if the same payment had already applied it
     * @throws IllegalArgumentException if the evidence does not match the extension
     * @throws IllegalStateException    if another payment already applied it
     */
    public boolean approvePayment(String paymentId, Money amount, String currency, Instant approvedAt) {
        if (source != QuotaExtensionSource.PAYMENT || paymentId == null || paymentId.isBlank() || amount == null
                || !"PEN".equals(currency) || !price.total().equals(amount)) {
            throw new IllegalArgumentException("subscription.payment.mismatch");
        }
        if (status == QuotaExtensionStatus.APPLIED) {
            if (!paymentId.equals(this.paymentId)) {
                throw new IllegalStateException("subscription.payment.already_approved");
            }
            return false;
        }
        if (approvedAt == null || approvedAt.isBefore(createdAt)) {
            throw new IllegalStateException("subscription.activation.invalid");
        }
        this.paymentId = paymentId;
        this.status = QuotaExtensionStatus.APPLIED;
        this.appliedAt = approvedAt;
        return true;
    }

    /**
     * Returns the current state of the extension.
     *
     * @return the snapshot
     */
    public QuotaExtensionSnapshot snapshot() {
        return new QuotaExtensionSnapshot(id, subscriptionId, producerId, source, addedArea, status, billedMonths, price,
                extensionCodeId, paymentId, createdAt, appliedAt, revision);
    }
}
