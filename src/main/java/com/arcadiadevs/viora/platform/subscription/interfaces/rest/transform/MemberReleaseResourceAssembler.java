package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.subscription.application.commandservices.MemberRelease;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.MemberReleaseResource;

/**
 * Assembler from a member release to its public representation.
 */
public final class MemberReleaseResourceAssembler {

    private MemberReleaseResourceAssembler() {
    }

    /**
     * Builds the representation of a release.
     *
     * @param release the release
     * @return the resource
     */
    public static MemberReleaseResource toResource(MemberRelease release) {
        return new MemberReleaseResource(release.memberId(), release.readingEndedAt(), release.planEndsAt(),
                release.seatFreesAt());
    }
}
