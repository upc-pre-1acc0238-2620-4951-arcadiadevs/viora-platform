package com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionBlocker;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SustainableCropLoad;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.PrescriptionResource;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Presentation mapper translating thinning prescription domain snapshots to REST resources.
 */
public final class PrescriptionResourceFromEntityAssembler {

    /** The only way a window is placed: the observed full bloom plus the offsets of the technical profile. */
    private static final String WINDOW_BASIS = "FULL_BLOOM_PLUS_PROFILE_OFFSETS";

    private PrescriptionResourceFromEntityAssembler() {
    }

    /**
     * Maps a domain snapshot to the public REST response.
     *
     * @param snapshot prescription domain snapshot
     * @param blockers inputs still missing to issue the prescription
     * @return REST presentation resource
     */
    public static PrescriptionResource toResource(
            FruitThinningPrescriptionSnapshot snapshot, List<PrescriptionBlocker> blockers) {
        if (snapshot == null) {
            return null;
        }
        SustainableCropLoad load = snapshot.sustainableLoad();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate opensOn = load == null ? null : load.windowOpensOn();
        LocalDate closesOn = load == null ? null : load.windowClosesOn();
        boolean windowOpen = closesOn != null && !today.isAfter(closesOn)
                && (opensOn == null || !today.isBefore(opensOn))
                && snapshot.status() == PrescriptionStatus.PRESCRIBED;

        return new PrescriptionResource(
                snapshot.id().prescriptionId(),
                snapshot.plotId().plotId(),
                snapshot.campaignYear().value(),
                load == null ? null : ThinningRounding.load(load.targetFruitsPerShoot()),
                load == null ? null : ThinningRounding.percentage(load.percentageToRemove()),
                ThinningRounding.LOAD_UNIT,
                snapshot.status().name(),
                snapshot.fullBloomOn(),
                opensOn,
                closesOn,
                closesOn == null ? null : WINDOW_BASIS,
                load == null ? null : load.profileVersion(),
                load == null ? null : load.profileStatus(),
                windowOpen,
                blockers == null ? List.of() : blockers.stream().map(Enum::name).toList(),
                snapshot.issuedAt()
        );
    }
}
