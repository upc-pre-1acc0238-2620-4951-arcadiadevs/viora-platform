package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.CooperativeLicenseResource;

import java.time.Instant;

/**
 * Assembler from a cooperative license to its public representation.
 */
public final class CooperativeLicenseResourceAssembler {

    private CooperativeLicenseResourceAssembler() {
    }

    /**
     * Builds the representation of a license.
     *
     * @param license the license
     * @param now     the current instant, to tell whether it is in force
     * @return the resource
     */
    public static CooperativeLicenseResource toResource(CooperativeLicense license, Instant now) {
        var snapshot = license.snapshot();
        return new CooperativeLicenseResource(
                snapshot.id().cooperativeLicenseId(),
                snapshot.cooperativeName(),
                snapshot.seats(),
                snapshot.contractedArea().hectares(),
                snapshot.period().startsAt(),
                snapshot.period().endsAt(),
                license.isInForce(now));
    }
}
