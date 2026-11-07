package com.arcadiadevs.viora.platform.subscription.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gives the seat and hectares of a member back to their license when the member deletes their account. Their code is
 * released, never handed out again.
 */
@Component
public class ReleaseSeatOnMemberAccountDeletedHandler {

    private final InvitationCodeRepository invitationCodeRepository;
    private final CooperativeLicenseRepository cooperativeLicenseRepository;

    /**
     * Creates the handler.
     *
     * @param invitationCodeRepository     the codes members redeemed
     * @param cooperativeLicenseRepository the licenses
     * @throws IllegalArgumentException if any dependency is null
     */
    public ReleaseSeatOnMemberAccountDeletedHandler(InvitationCodeRepository invitationCodeRepository,
                                                    CooperativeLicenseRepository cooperativeLicenseRepository) {
        if (invitationCodeRepository == null || cooperativeLicenseRepository == null) {
            throw new IllegalArgumentException("subscription.member_deletion_handler.dependency.null");
        }
        this.invitationCodeRepository = invitationCodeRepository;
        this.cooperativeLicenseRepository = cooperativeLicenseRepository;
    }

    /**
     * Releases the codes the deleted account redeemed.
     *
     * @param event the deleted account
     */
    @EventListener
    @Transactional
    public void on(UserAccountDeletedEvent event) {
        if (event == null) {
            return;
        }
        final ProducerId producerId;
        try {
            producerId = new ProducerId(event.userId());
        } catch (IllegalArgumentException ex) {
            return;
        }
        for (var code : invitationCodeRepository.findRedeemedByProducerId(producerId)) {
            cooperativeLicenseRepository.findByIdForRedemption(code.snapshot().licenseId()).ifPresent(license -> {
                if (license.releaseMemberCode(code, event.occurredOn())) {
                    invitationCodeRepository.save(code);
                    cooperativeLicenseRepository.save(license);
                }
            });
        }
    }
}
