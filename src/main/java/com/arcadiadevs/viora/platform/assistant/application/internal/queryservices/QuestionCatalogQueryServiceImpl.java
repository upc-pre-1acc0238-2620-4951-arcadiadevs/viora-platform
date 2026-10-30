package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.application.internal.answers.ClimateDataReader;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.HomeDataReader;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalPhenologyService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalSubscriptionService;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.acl.ExternalThinningService;
import com.arcadiadevs.viora.platform.assistant.application.queryservices.QuestionCatalogQueryService;
import com.arcadiadevs.viora.platform.assistant.domain.model.queries.GetQuestionCatalogQuery;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.HomeData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenQuestionCatalog;
import com.arcadiadevs.viora.platform.assistant.domain.services.QuestionCatalog;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Year;
import java.util.List;
import java.util.Optional;

/**
 * Application query service evaluating the question catalog of a screen against the data of the plot. It only
 * reads figures other contexts already computed, so the catalog stays cheap and never calls the model.
 */
@Service
public class QuestionCatalogQueryServiceImpl implements QuestionCatalogQueryService {

    private static final String CALIBER_ESTIMATED = "ESTIMATED";

    private final ExternalSubscriptionService externalSubscriptionService;
    private final ExternalOrchardService externalOrchardService;
    private final ExternalPhenologyService externalPhenologyService;
    private final ExternalThinningService externalThinningService;
    private final ClimateDataReader climateDataReader;
    private final HomeDataReader homeDataReader;
    private final Clock clock;

    /**
     * Constructs the query service.
     *
     * @param externalSubscriptionService tells whether the producer's plan is active
     * @param externalOrchardService      verifies the plot
     * @param externalPhenologyService    reads the harvest history, the bearing index and the winter chill
     * @param externalThinningService     reads the prescription and the caliber projection
     * @param climateDataReader           reads the alerts, forecast and nodes of the ALERT and CLIMATE screens
     * @param homeDataReader              reads the plot, or every active plot of the producer, for HOME
     * @param clock                       the application clock, for the current campaign
     */
    @Autowired
    public QuestionCatalogQueryServiceImpl(
            ExternalSubscriptionService externalSubscriptionService,
            ExternalOrchardService externalOrchardService,
            ExternalPhenologyService externalPhenologyService,
            ExternalThinningService externalThinningService,
            ClimateDataReader climateDataReader,
            HomeDataReader homeDataReader,
            Clock clock
    ) {
        this.externalSubscriptionService = externalSubscriptionService;
        this.externalOrchardService = externalOrchardService;
        this.externalPhenologyService = externalPhenologyService;
        this.externalThinningService = externalThinningService;
        this.climateDataReader = climateDataReader;
        this.homeDataReader = homeDataReader;
        this.clock = clock;
    }

    /**
     * Constructs the query service for the screens that read no climate data (unit tests of ALTERNATION and PLAN):
     * the ALERT and CLIMATE screens then see no climate data and offer their questions disabled, and HOME no plot.
     */
    QuestionCatalogQueryServiceImpl(
            ExternalSubscriptionService externalSubscriptionService,
            ExternalOrchardService externalOrchardService,
            ExternalPhenologyService externalPhenologyService,
            ExternalThinningService externalThinningService,
            Clock clock
    ) {
        this(externalSubscriptionService, externalOrchardService, externalPhenologyService, externalThinningService,
                null, null, clock);
    }

    @Override
    public Result<ScreenQuestionCatalog, ApplicationError> handle(GetQuestionCatalogQuery query) {
        if (query == null) {
            return Result.failure(ApplicationError.validationError("query", "assistant.query.null"));
        }
        if (!externalSubscriptionService.hasActivePlan(query.producerId())) {
            return Result.failure(ApplicationError.forbidden("assistant", "assistant.subscription.inactive"));
        }
        // Without plot (HOME about the whole field) there is no plot to verify
        if (query.plotId() != null && !externalOrchardService.existsActivePlot(query.plotId())) {
            return Result.failure(ApplicationError.notFound("plot", query.plotId().plotId()));
        }
        var facts = factsFor(query.screen(), query.producerId(), query.plotId());
        return Result.success(new ScreenQuestionCatalog(query.screen(), query.plotId(),
                QuestionCatalog.questionsFor(query.screen(), facts)));
    }

    /** Reads only the data the screen needs. */
    private PlotFacts factsFor(ScreenKey screen, String producerId, PlotId plotId) {
        int campaignYear = Year.now(clock.withZone(ProducerTimeZone.ZONE)).getValue();
        if (screen == ScreenKey.HOME) {
            return PlotFacts.ofHome(campaignYear, homeDataReader == null
                    ? new HomeData(plotId == null, List.of(), null)
                    : homeDataReader.readForCatalog(producerId, plotId, campaignYear));
        }
        if (screen == ScreenKey.PLOT_DETAIL) {
            return PlotFacts.ofPlotDetail(campaignYear,
                    externalPhenologyService.findBiennialBearingIndex(plotId).orElse(null),
                    homeDataReader == null
                            ? new HomeData(false, List.of(), null)
                            : homeDataReader.readForCatalog(producerId, plotId, campaignYear));
        }
        if (screen.readsChill()) {
            return PlotFacts.ofChill(campaignYear, externalPhenologyService.findChillData(plotId));
        }
        if (screen.readsClimate()) {
            return PlotFacts.ofClimate(campaignYear,
                    climateDataReader == null ? ClimateData.NONE : climateDataReader.read(screen, plotId));
        }
        Optional<PrescriptionAvailability> prescription =
                externalThinningService.findPrescriptionAvailability(plotId, campaignYear);
        Double bearingIndex = null;
        int historicalCampaigns = 0;
        if (screen == ScreenKey.ALTERNATION) {
            bearingIndex = externalPhenologyService.findBiennialBearingIndex(plotId).orElse(null);
            historicalCampaigns = externalPhenologyService.countHistoricalCampaigns(plotId);
        }
        return new PlotFacts(
                campaignYear,
                bearingIndex,
                historicalCampaigns,
                prescription.map(p -> CALIBER_ESTIMATED.equals(p.caliberProjectionStatus())).orElse(false),
                prescription.map(PrescriptionAvailability::issued).orElse(false),
                prescription.map(PrescriptionAvailability::percentageToRemove).orElse(null),
                prescription.map(PrescriptionAvailability::sampledTrees).orElse(0),
                prescription.map(PrescriptionAvailability::treesNeeded).orElse(0));
    }
}
