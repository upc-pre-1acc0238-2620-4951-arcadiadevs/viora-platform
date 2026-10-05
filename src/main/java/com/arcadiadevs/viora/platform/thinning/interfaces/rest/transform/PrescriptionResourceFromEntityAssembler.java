package com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SustainableCropLoad;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.PrescriptionResource;

import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Presentation mapper translating thinning prescription domain snapshots to REST resources.
 */
public final class PrescriptionResourceFromEntityAssembler {

    private PrescriptionResourceFromEntityAssembler() {
    }

    /**
     * Maps a domain snapshot to the public REST response.
     *
     * @param snapshot prescription domain snapshot
     * @return REST presentation resource
     */
    public static PrescriptionResource toResource(FruitThinningPrescriptionSnapshot snapshot) {
        if (snapshot == null) {
            return null;
        }
        SustainableCropLoad load = snapshot.sustainableLoad();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate closesOn = load == null ? null : load.windowClosesOn();
        boolean windowOpen = closesOn != null && !today.isAfter(closesOn)
                && snapshot.status() == com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus.PRESCRIBED;

        return new PrescriptionResource(
                snapshot.id().prescriptionId(),
                snapshot.plotId().plotId(),
                snapshot.campaignYear().value(),
                load == null ? null : load.targetFruitsPerShoot(),
                load == null ? null : load.percentageToRemove(),
                snapshot.status().name(),
                closesOn,
                windowOpen,
                snapshot.issuedAt()
        );
    }
}
