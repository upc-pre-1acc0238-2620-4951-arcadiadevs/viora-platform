package com.arcadiadevs.viora.platform.thinning.application.internal.queryservices;

import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.application.queryservices.GetThinningPrescriptionQueryService;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetActiveThinningPrescriptionQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetThinningPrescriptionByIdQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.queries.GetThinningPrescriptionsQuery;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

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

    @Override
    public Result<List<FruitThinningPrescriptionSnapshot>, ApplicationError> handle(GetThinningPrescriptionsQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "thinning.query.null"));
        }
        var plotIds = externalOrchardService.findActivePlotIdsByProducerId(query.producerId());
        if (plotIds.isEmpty()) {
            return Result.success(List.of());
        }
        return Result.success(prescriptionRepository.findByPlotIdInAndCampaignYear(plotIds, query.campaignYear())
                .stream()
                .map(FruitThinningPrescription::snapshot)
                .sorted(Comparator.comparing(snapshot -> snapshot.plotId().plotId()))
                .toList());
    }

    @Override
    public Result<FruitThinningPrescriptionSnapshot, ApplicationError> handle(GetThinningPrescriptionByIdQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "thinning.query.null"));
        }
        var snapshot = prescriptionRepository.findById(query.prescriptionId()).map(FruitThinningPrescription::snapshot);
        if (snapshot.isEmpty() || !externalOrchardService.existsActivePlot(snapshot.get().plotId())) {
            return Result.failure(ApplicationError.notFound("ThinningPrescription",
                    query.prescriptionId().prescriptionId()));
        }
        return Result.success(snapshot.get());
    }
}
