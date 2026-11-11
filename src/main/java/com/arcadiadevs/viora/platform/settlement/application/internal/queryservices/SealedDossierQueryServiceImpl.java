package com.arcadiadevs.viora.platform.settlement.application.internal.queryservices;

import com.arcadiadevs.viora.platform.settlement.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.settlement.application.queryservices.SealedDossierQueryService;
import com.arcadiadevs.viora.platform.settlement.domain.model.aggregates.SealedDossierSnapshot;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetSealedDossierByPlotIdAndCampaignYearQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetSealedDossierByVerificationHashQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetSealedDossierPdfByPlotIdAndCampaignYearQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.queries.GetSealedDossiersByPlotIdQuery;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects.VerificationHash;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.AgronomicReportRepository;
import com.arcadiadevs.viora.platform.settlement.domain.repositories.SealedDossierRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Read side of the dossiers sealed by producers. */
@Service
@Transactional(readOnly = true)
public class SealedDossierQueryServiceImpl implements SealedDossierQueryService {

    private static final String RESOURCE = "AgronomicDossier";
    private static final String QUERY_NULL = "settlement.query.null";

    private final SealedDossierRepository dossierRepository;
    private final AgronomicReportRepository reportRepository;
    private final ExternalOrchardService orchard;

    public SealedDossierQueryServiceImpl(SealedDossierRepository dossierRepository,
            AgronomicReportRepository reportRepository, ExternalOrchardService orchard) {
        this.dossierRepository = dossierRepository;
        this.reportRepository = reportRepository;
        this.orchard = orchard;
    }

    @Override
    public Result<DossierView, ApplicationError> handle(GetSealedDossierByPlotIdAndCampaignYearQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", QUERY_NULL));
        }
        var dossier = ownedDossier(new PlotId(query.plotId()), new CampaignYear(query.campaignYear()),
                query.actorId());
        if (dossier instanceof Result.Failure<SealedDossierSnapshot, ApplicationError> failure) {
            return Result.failure(failure.error());
        }
        var snapshot = ((Result.Success<SealedDossierSnapshot, ApplicationError>) dossier).value();
        var certification = reportRepository.findByPlotId(snapshot.plotId())
                .flatMap(report -> report.certificationOf(snapshot.campaignYear()))
                .orElse(null);
        return Result.success(new DossierView(snapshot, certification));
    }

    @Override
    public Result<List<SealedDossierSnapshot>, ApplicationError> handle(GetSealedDossiersByPlotIdQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", QUERY_NULL));
        }
        var plot = new PlotId(query.plotId());
        var ownership = checkOwner(plot, query.actorId());
        if (ownership != null) {
            return Result.failure(ownership);
        }
        return Result.success(dossierRepository.findByPlotId(plot));
    }

    @Override
    public Result<DossierFile, ApplicationError> handle(GetSealedDossierPdfByPlotIdAndCampaignYearQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", QUERY_NULL));
        }
        var dossier = ownedDossier(new PlotId(query.plotId()), new CampaignYear(query.campaignYear()),
                query.actorId());
        if (dossier instanceof Result.Failure<SealedDossierSnapshot, ApplicationError> failure) {
            return Result.failure(failure.error());
        }
        var snapshot = ((Result.Success<SealedDossierSnapshot, ApplicationError>) dossier).value();
        return dossierRepository.findDocument(snapshot.id())
                .<Result<DossierFile, ApplicationError>>map(document -> Result.success(new DossierFile(snapshot, document)))
                .orElseGet(() -> Result.failure(ApplicationError.notFound(RESOURCE, snapshot.id().value())));
    }

    @Override
    public Result<SealedDossierSnapshot, ApplicationError> handle(GetSealedDossierByVerificationHashQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", QUERY_NULL));
        }
        var hash = new VerificationHash(query.verificationHash());
        return dossierRepository.findByVerificationHash(hash)
                .<Result<SealedDossierSnapshot, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(ApplicationError.notFound(RESOURCE, hash.value())));
    }

    private Result<SealedDossierSnapshot, ApplicationError> ownedDossier(PlotId plot, CampaignYear year,
            String actorId) {
        var ownership = checkOwner(plot, actorId);
        if (ownership != null) {
            return Result.failure(ownership);
        }
        return dossierRepository.findByPlotIdAndCampaignYear(plot, year)
                .<Result<SealedDossierSnapshot, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(ApplicationError.notFound(RESOURCE,
                        plot.plotId() + "/" + year.value())));
    }

    /** Null when the actor owns the active plot; otherwise the not found or forbidden error. */
    private ApplicationError checkOwner(PlotId plot, String actorId) {
        var owner = orchard.findActivePlotOwner(plot);
        if (owner.isEmpty()) {
            return ApplicationError.notFound("Plot", plot.plotId());
        }
        if (!owner.get().equals(new UserId(actorId))) {
            return ApplicationError.forbidden("Plot", "settlement.plot.not_owner");
        }
        return null;
    }
}
