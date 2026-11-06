package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.subscription.application.queryservices.DataSharingConsentView;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.DataSharingConsentResource;

/**
 * Assembler from data sharing consents to their public representation.
 */
public final class DataSharingConsentResourceAssembler {

    private DataSharingConsentResourceAssembler() {
    }

    /**
     * Builds the representation of a consent.
     *
     * @param view the consent as its producer sees it
     * @return the resource
     */
    public static DataSharingConsentResource toResource(DataSharingConsentView view) {
        return new DataSharingConsentResource(view.id(), view.organizationName(), view.managerName(), view.status(),
                view.acceptedAt(), view.withdrawnAt(), view.planEndsAt(), view.consentTextVersion());
    }
}
