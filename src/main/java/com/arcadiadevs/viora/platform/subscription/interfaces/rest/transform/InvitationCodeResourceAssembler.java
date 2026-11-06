package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.subscription.application.commandservices.InvitationCodeBatch;
import com.arcadiadevs.viora.platform.subscription.application.queryservices.InvitationCodeView;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.InvitationCodeBatchResource;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.InvitationCodeResource;

import java.time.Instant;

/**
 * Assembler from invitation codes to their public representations.
 */
public final class InvitationCodeResourceAssembler {

    private InvitationCodeResourceAssembler() {
    }

    /**
     * Builds the representation of a new batch.
     *
     * @param batch the issued batch
     * @return the resource
     */
    public static InvitationCodeBatchResource toResource(InvitationCodeBatch batch) {
        return new InvitationCodeBatchResource(batch.batchId(), batch.issuedAt(), batch.expiresAt(),
                batch.hectaresPerCode(), batch.codes().stream()
                .map(code -> new InvitationCodeBatchResource.IssuedCodeResource(code.id(), code.code()))
                .toList());
    }

    /**
     * Builds the representation of a listed code.
     *
     * @param view the code as the manager sees it
     * @return the resource
     */
    public static InvitationCodeResource toResource(InvitationCodeView view) {
        return new InvitationCodeResource(view.id(), view.batchId(), view.issuedAt(), view.expiresAt(),
                view.hectares(), view.status(), view.code(), view.maskedCode(), view.redeemedBy(), view.redeemedAt());
    }

    /**
     * Builds the representation of a code just revoked: masked, as it can no longer be shared.
     *
     * @param code the revoked code
     * @param now  the current instant
     * @return the resource
     */
    public static InvitationCodeResource toResource(InvitationCode code, Instant now) {
        var snapshot = code.snapshot();
        return new InvitationCodeResource(snapshot.id().invitationCodeId(), snapshot.batchId(), snapshot.issuedAt(),
                snapshot.expiresAt(), snapshot.quota().hectares(), code.statusAt(now).name(), null,
                "COOP-••" + (snapshot.hint() == null ? "••" : snapshot.hint()), null, null);
    }
}
