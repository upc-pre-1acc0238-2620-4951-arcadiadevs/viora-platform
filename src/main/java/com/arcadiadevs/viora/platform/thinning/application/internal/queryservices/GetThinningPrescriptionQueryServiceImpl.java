package com.arcadiadevs.viora.platform.thinning.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetThinningPrescriptionQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetActiveThinningPrescriptionQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Read-only application service for thinning prescriptions.
 */
@Service
@Transactional(readOnly = true)
public class GetThinningPrescriptionQueryServiceImpl implements GetThinningPrescriptionQueryService {

    private final FruitThinningPrescriptionRepository prescriptionRepository;
    private final ExternalOrchardService externalOrchardService;

    /**
     * Constructs the query service.
     *
     * @param prescriptionRepository thinning prescription repository
     * @param externalOrchardService outbound ACL for active plot verification
     */
    public GetThinningPrescriptionQueryServiceImpl(
            FruitThinningPrescriptionRepository prescriptionRepository,
            ExternalOrchardService externalOrchardService
    ) {
        this.prescriptionRepository = prescriptionRepository;
        this.externalOrchardService = externalOrchardService;
    }

    @Override
    public Result<FruitThinningPrescriptionSnapshot, ApplicationError> handle(
            GetActiveThinningPrescriptionQuery query
    ) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "thinning.query.null"));
        }

        if (!externalOrchardService.existsActivePlot(query.plotId())) {
            return Result.failure(ApplicationError.notFound("Plot", query.plotId().plotId()));
        }

        var prescription = prescriptionRepository.findByPlotIdAndCampaignYear(
                query.plotId(),
                query.campaignYear()
        );
        if (prescription.isEmpty()) {
            return Result.failure(ApplicationError.notFound(
                    "ThinningPrescription",
                    query.plotId().plotId()
            ));
        }

        FruitThinningPrescriptionSnapshot snapshot = prescription.get().snapshot();
        PrescriptionStatus statusFilter = query.statusFilter();
        if (statusFilter != null && snapshot.status() != statusFilter) {
            return Result.failure(ApplicationError.notFound(
                    "ThinningPrescription",
                    query.plotId().plotId()
            ));
        }

        return Result.success(snapshot);
    }
}
