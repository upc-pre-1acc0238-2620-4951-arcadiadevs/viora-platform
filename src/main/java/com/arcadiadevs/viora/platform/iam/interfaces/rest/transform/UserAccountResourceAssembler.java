package com.arcadiadevs.viora.platform.iam.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.interfaces.rest.resources.UserAccountResource;

/**
 * Assembler transforming UserAccount domain aggregate to UserAccountResource DTO.
 */
public final class UserAccountResourceAssembler {

    private UserAccountResourceAssembler() {
    }

    public static UserAccountResource toResource(UserAccount account) {
        if (account == null) {
            return null;
        }
        var snapshot = account.snapshot();
        return new UserAccountResource(
                snapshot.id().userId(),
                snapshot.email().value(),
                snapshot.role().name(),
                snapshot.createdAt()
        );
    }
}
