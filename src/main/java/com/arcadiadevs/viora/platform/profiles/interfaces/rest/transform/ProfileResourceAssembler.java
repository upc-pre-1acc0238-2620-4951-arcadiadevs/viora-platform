package com.arcadiadevs.viora.platform.profiles.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.profiles.domain.model.aggregates.Profile;
import com.arcadiadevs.viora.platform.profiles.interfaces.rest.resources.ProfileResource;

/**
 * Assembler transforming Profile domain aggregate to ProfileResource DTO.
 */
public final class ProfileResourceAssembler {

    private ProfileResourceAssembler() {
    }

    public static ProfileResource toResource(Profile profile) {
        if (profile == null) {
            return null;
        }
        var snapshot = profile.snapshot();
        return new ProfileResource(
                snapshot.id().profileId(),
                snapshot.userId().userId(),
                snapshot.fullName().value(),
                snapshot.country().code(),
                snapshot.phoneNumber().value(),
                snapshot.createdAt(),
                snapshot.updatedAt()
        );
    }
}
