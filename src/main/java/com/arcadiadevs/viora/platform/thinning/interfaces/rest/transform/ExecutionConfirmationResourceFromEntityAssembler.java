package com.arcadiadevs.viora.platform.thinning.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberProjection;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.ExecutionTimeliness;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.LoadBalance;
import com.arcadiadevs.viora.platform.thinning.domain.services.CaliberModelFittingService;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.CaliberProjectionResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.ExecutionConfirmationResource;
import com.arcadiadevs.viora.platform.thinning.interfaces.rest.resources.LoadBalanceResource;
import org.springframework.context.i18n.LocaleContextHolder;
import java.util.ResourceBundle;

/** Maps saved execution evidence to the public response. */
public final class ExecutionConfirmationResourceFromEntityAssembler {
    private ExecutionConfirmationResourceFromEntityAssembler() { }

    public static ExecutionConfirmationResource toResource(FruitThinningPrescription prescription) {
        var snapshot = prescription.snapshot();
        var confirmation = snapshot.executionConfirmation();
        return new ExecutionConfirmationResource(snapshot.id().prescriptionId(), confirmation.id().confirmationId(),
                confirmation.timeliness().name(), confirmation.executionDate(), confirmation.actualRemovalPercentage(),
                confirmation.removedKg(), confirmation.laborCrewSize(), confirmation.notes(),
                confirmation.timeliness() == ExecutionTimeliness.OPTIMAL, confirmation.recordedAt(),
                confirmation.timeliness() == ExecutionTimeliness.LATE
                        ? ResourceBundle.getBundle("messages", LocaleContextHolder.getLocale())
                                .getString("thinning.execution.late.warning")
                        : null,
                toResource(confirmation.loadBalance()),
                toResource(confirmation.caliberProjection()));
    }

    private static LoadBalanceResource toResource(LoadBalance balance) {
        if (balance == null) {
            return null;
        }
        // Domain values keep full precision; they are rounded only here, for display
        return new LoadBalanceResource(round(balance.preThinningFruitsPerShoot(), 3),
                round(balance.residualFruitsPerShoot(), 3), round(balance.targetFruitsPerShoot(), 3),
                round(balance.deltaFruitsPerShoot(), 3), round(balance.loadRatio(), 3),
                balance.loadState().name());
    }

    private static CaliberProjectionResource toResource(CaliberProjection projection) {
        if (projection == null) {
            return null;
        }
        // Size grades come from the full-precision values; only the displayed counts are rounded
        return new CaliberProjectionResource(projection.status().name(),
                round(projection.mostLikelyFruitsPerKg(), 1), round(projection.fruitsPerKgLow(), 1),
                round(projection.fruitsPerKgHigh(), 1), projection.mostLikelySizeGrade(),
                projection.sizeGradeLow(), projection.sizeGradeHigh(), projection.confidenceLevel(),
                projection.calibrationObservations(), CaliberModelFittingService.MIN_OBSERVATIONS,
                projection.modelVersion());
    }

    private static Double round(Double value, int decimals) {
        if (value == null) {
            return null;
        }
        double factor = Math.pow(10, decimals);
        return Math.round(value * factor) / factor;
    }
}
