package com.arcadiadevs.viora.platform.territory.application.internal.security;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;

/**
 * The license a signed-in user reaches, and as whom: the manager in charge of it, or one of its members.
 *
 * @param licenseId  the license
 * @param managerId  the manager; null for a member
 * @param producerId the member; null for a manager
 */
public record LicenseScope(LicenseId licenseId, ManagerId managerId, ProducerId producerId) {

    /**
     * Tells whether the user is the manager of the license.
     *
     * @return true for the manager
     */
    public boolean isManager() {
        return managerId != null;
    }
}
