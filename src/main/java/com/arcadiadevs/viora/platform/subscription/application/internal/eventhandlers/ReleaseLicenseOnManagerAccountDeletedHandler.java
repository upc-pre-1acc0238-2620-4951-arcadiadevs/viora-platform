package com.arcadiadevs.viora.platform.subscription.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event handler in Subscription leaving a license without a manager when that manager's account is deleted.
 *
 * <p>It runs inside the transaction that deletes the account. The license, its producers and its codes stay; Viora
 * issues another manager code for the organization.</p>
 */
@Component
public class ReleaseLicenseOnManagerAccountDeletedHandler {

    private final CooperativeLicenseRepository licenses;

    /**
     * Creates the handler.
     *
     * @param licenses the cooperative licenses
     * @throws IllegalArgumentException if the repository is null
     */
    public ReleaseLicenseOnManagerAccountDeletedHandler(CooperativeLicenseRepository licenses) {
        if (licenses == null) {
            throw new IllegalArgumentException("subscription.license_repository.null");
        }
        this.licenses = licenses;
    }

    /**
     * Releases the license the deleted account was in charge of. An account in charge of none is ignored.
     *
     * @param event the deleted account
     */
    @EventListener
    public void on(UserAccountDeletedEvent event) {
        if (event == null) {
            return;
        }
        var manager = new ManagerId(event.userId());
        licenses.findByManagerIdForUpdate(manager).ifPresent(license -> {
            if (license.releaseManager(manager)) {
                licenses.save(license);
            }
        });
    }
}
