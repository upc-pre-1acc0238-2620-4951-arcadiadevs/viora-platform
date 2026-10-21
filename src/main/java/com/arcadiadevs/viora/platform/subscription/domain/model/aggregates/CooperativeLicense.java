package com.arcadiadevs.viora.platform.subscription.domain.model.aggregates;

import com.arcadiadevs.viora.platform.shared.domain.model.aggregates.AbstractDomainAggregateRoot;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InsufficientLicenseAreaException;
import com.arcadiadevs.viora.platform.subscription.domain.exceptions.InvitationCodeNotRedeemableException;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.RedeemedCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Aggregate root of the collective license a cooperative contracts for its producers.
 * <p>Each invitation code holds one of its seats from the moment it is issued. The hectares are committed when a
 * producer redeems a code, for the surface that producer declared, and can never exceed the contracted area.</p>
 */
public class CooperativeLicense extends AbstractDomainAggregateRoot<CooperativeLicense> {

    private final CooperativeLicenseId id;
    private final CooperativeId cooperativeId;
    private final String cooperativeName;
    private final int seats;
    private final HectaresQuota contractedArea;
    private BigDecimal committedAreaHa;
    private final SubscriptionPeriod period;
    private final Long revision;

    private CooperativeLicense(CooperativeLicenseSnapshot snapshot) {
        this.id = snapshot.id();
        this.cooperativeId = snapshot.cooperativeId();
        this.cooperativeName = snapshot.cooperativeName();
        this.seats = snapshot.seats();
        this.contractedArea = snapshot.contractedArea();
        this.committedAreaHa = snapshot.committedAreaHa().setScale(2, RoundingMode.HALF_UP);
        this.period = snapshot.period();
        this.revision = snapshot.revision();
    }

    /**
     * Grants a new license to a cooperative, with no hectares committed yet.
     *
     * @param cooperativeId   the cooperative
     * @param cooperativeName the display name of the cooperative
     * @param seats           the number of producers covered; at least one
     * @param contractedArea  the hectares covered in total
     * @param period          the window in which the license is in force
     * @return the license
     * @throws IllegalArgumentException if an argument is null, the name is blank or there are no seats
     */
    public static CooperativeLicense grant(CooperativeId cooperativeId, String cooperativeName, int seats,
                                           HectaresQuota contractedArea, SubscriptionPeriod period) {
        return reconstitute(new CooperativeLicenseSnapshot(new CooperativeLicenseId(), cooperativeId, cooperativeName,
                seats, contractedArea, BigDecimal.ZERO, period, 0L));
    }

    /**
     * Rebuilds a license from its persisted state.
     *
     * @param snapshot the persisted state
     * @return the license
     * @throws IllegalArgumentException if the snapshot is null or breaks an invariant of the license
     */
    public static CooperativeLicense reconstitute(CooperativeLicenseSnapshot snapshot) {
        if (snapshot == null) {
            throw new IllegalArgumentException("subscription.license.snapshot.null");
        }
        if (snapshot.id() == null || snapshot.cooperativeId() == null || snapshot.contractedArea() == null
                || snapshot.committedAreaHa() == null || snapshot.period() == null) {
            throw new IllegalArgumentException("subscription.license.snapshot.incomplete");
        }
        if (snapshot.cooperativeName() == null || snapshot.cooperativeName().isBlank()) {
            throw new IllegalArgumentException("subscription.license.cooperative_name.blank");
        }
        if (snapshot.seats() < 1) {
            throw new IllegalArgumentException("subscription.license.seats.positive");
        }
        if (snapshot.committedAreaHa().signum() < 0
                || snapshot.committedAreaHa().compareTo(snapshot.contractedArea().hectares()) > 0) {
            throw new IllegalArgumentException("subscription.license.committed_area.out_of_range");
        }
        return new CooperativeLicense(snapshot);
    }

    /**
     * Redeems one of the license's codes for a producer: the code is consumed and the producer's hectares are
     * committed, or nothing changes.
     *
     * @param code       the code being redeemed; it must belong to this license
     * @param producerId the producer redeeming it
     * @param quota      the hectares the producer wants covered
     * @param at         the instant of the redemption
     * @return the evidence of the redemption, used to activate the sponsored subscription
     * @throws IllegalArgumentException             if an argument is null or the code belongs to another license
     * @throws InvitationCodeNotRedeemableException if the license is not in force or the code cannot be redeemed
     * @throws InsufficientLicenseAreaException     if the license has fewer hectares left than {@code quota}
     */
    public RedeemedCode redeem(InvitationCode code, ProducerId producerId, HectaresQuota quota, Instant at) {
        if (code == null || producerId == null || quota == null || at == null) {
            throw new IllegalArgumentException("subscription.license.redemption.incomplete");
        }
        if (!id.equals(code.snapshot().licenseId())) {
            throw new IllegalArgumentException("subscription.license.code_of_another_license");
        }
        if (!period.contains(at) || !code.isRedeemable(at)) {
            throw new InvitationCodeNotRedeemableException();
        }
        var committed = committedAreaHa.add(quota.hectares());
        if (committed.compareTo(contractedArea.hectares()) > 0) {
            throw new InsufficientLicenseAreaException();
        }
        code.redeem(producerId, at);
        this.committedAreaHa = committed;
        return new RedeemedCode(code.snapshot().id(), id, cooperativeId, producerId, quota, period);
    }

    /**
     * Returns the hectares the license can still commit.
     *
     * @return the contracted area minus the committed area
     */
    public BigDecimal remainingAreaHa() {
        return contractedArea.hectares().subtract(committedAreaHa);
    }

    /**
     * Returns the current state of the license.
     *
     * @return the snapshot
     */
    public CooperativeLicenseSnapshot snapshot() {
        return new CooperativeLicenseSnapshot(id, cooperativeId, cooperativeName, seats, contractedArea,
                committedAreaHa, period, revision);
    }
}
