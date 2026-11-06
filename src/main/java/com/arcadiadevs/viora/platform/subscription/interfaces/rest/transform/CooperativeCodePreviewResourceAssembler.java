package com.arcadiadevs.viora.platform.subscription.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.subscription.application.queryservices.CooperativeCodePreview;
import com.arcadiadevs.viora.platform.subscription.interfaces.rest.resources.CooperativeCodePreviewResource;

/**
 * Assembler from the preview of a cooperative code to its public representation.
 */
public final class CooperativeCodePreviewResourceAssembler {

    private CooperativeCodePreviewResourceAssembler() {
    }

    /**
     * Builds the representation of a preview.
     *
     * @param preview the preview
     * @return the resource
     */
    public static CooperativeCodePreviewResource toResource(CooperativeCodePreview preview) {
        return new CooperativeCodePreviewResource(preview.organizationName(), preview.managerName(),
                preview.quotaHa(), preview.planEndsAt(), preview.registeredAreaHa(), preview.exceedsQuota(),
                preview.consentTextVersion());
    }
}
