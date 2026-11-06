package com.arcadiadevs.viora.platform.territory.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.territory.application.queryservices.CooperativeMemberDetailView;
import com.arcadiadevs.viora.platform.territory.application.queryservices.CooperativeMemberView;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.CooperativeMemberDetailResource;
import com.arcadiadevs.viora.platform.territory.interfaces.rest.resources.CooperativeMemberResource;

/**
 * Assembler from members of a license to their public representations.
 */
public final class CooperativeMemberResourceAssembler {

    private CooperativeMemberResourceAssembler() {
    }

    /**
     * Builds the representation of a member in the roll.
     *
     * @param view the member
     * @return the resource
     */
    public static CooperativeMemberResource toResource(CooperativeMemberView view) {
        return new CooperativeMemberResource(view.id(), view.fullName(), view.sharing(), view.withdrawnAt(),
                view.plotCount(), view.registeredAreaHa(), view.sectorNames(), view.worstRiskLevel(),
                MemberPlotResourceAssembler.toResource(view.worstReason()));
    }

    /**
     * Builds the representation of one member in detail.
     *
     * @param view the member
     * @return the resource
     */
    public static CooperativeMemberDetailResource toResource(CooperativeMemberDetailView view) {
        return new CooperativeMemberDetailResource(view.id(), view.fullName(), view.sharing(), view.withdrawnAt(),
                view.memberSince(), view.quotaHa(), view.maskedCode(), view.phone(), view.email(),
                view.registeredAreaHa(),
                view.plots().stream().map(MemberPlotResourceAssembler::toResource).toList());
    }
}
