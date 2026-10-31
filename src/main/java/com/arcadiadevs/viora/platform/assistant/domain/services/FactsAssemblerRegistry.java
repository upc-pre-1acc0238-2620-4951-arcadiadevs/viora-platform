package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AlertFruitsetEffectFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AlertPlotsAtRiskFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AlertWhatToDoFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AlertWhenEndsFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.BestTimeFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.CampaignPlanNextFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.CampaignVsPreviousFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillIfIncompleteFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillMissingFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillReachFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ChillVsLastWinterFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ClimateFruitsetEffectFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.DailySummaryFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HeatEndsFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeAlertsAffectingFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeCampaignVsPreviousFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeHarvestEstimateFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.HomeWeekPlanFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.IrrigateFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.SamplingEnoughTreesFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.SamplingMeaningFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.SamplingNextFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.SensorsHowLinkFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.SensorsReliabilityFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.SensorsSayFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.BreakCycleFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.IfLessFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.IfNoneFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.NextYearFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.PlotHowIsFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.PlotLastSamplingFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.PlotTodayFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ThinningHarvestChangeFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ThinningNextSamplingFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.ThinningWhyTargetFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.VsCampaignsFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.WhyIndexFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.WhyPercentFactsAssembler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The facts assembler of each question, one per question.
 */
public final class FactsAssemblerRegistry {

    private final Map<QuestionId, FactsAssembler> assemblers;

    /**
     * Creates the registry.
     *
     * @param assemblers one assembler per question
     * @throws IllegalArgumentException when two assemblers answer the same question
     */
    public FactsAssemblerRegistry(List<? extends FactsAssembler> assemblers) {
        var byQuestion = new HashMap<QuestionId, FactsAssembler>();
        assemblers.forEach(assembler -> {
            if (byQuestion.put(assembler.questionId(), assembler) != null) {
                throw new IllegalArgumentException("assistant.assembler.duplicated");
            }
        });
        this.assemblers = Map.copyOf(byQuestion);
    }

    /**
     * The assemblers of the HOME, ALTERNATION, PLAN, ALERT, CLIMATE, WINTER_CHILL, PLOT_DETAIL, SENSORS, SAMPLING_SUMMARY,
     * THINNING_DONE and CAMPAIGN_CLOSED questions, and the daily summary of HOME. {@code campaign.caliber_change} has
     * none: it is always disabled.
     *
     * @return the registry
     */
    public static FactsAssemblerRegistry standard() {
        return new FactsAssemblerRegistry(List.of(
                new HomeWeekPlanFactsAssembler(), new HomeCampaignVsPreviousFactsAssembler(),
                new HomeAlertsAffectingFactsAssembler(), new HomeHarvestEstimateFactsAssembler(),
                new DailySummaryFactsAssembler(),
                new WhyIndexFactsAssembler(), new BreakCycleFactsAssembler(), new VsCampaignsFactsAssembler(),
                new NextYearFactsAssembler(), new WhyPercentFactsAssembler(), new IfLessFactsAssembler(),
                new IfNoneFactsAssembler(), new BestTimeFactsAssembler(), new AlertWhatToDoFactsAssembler(),
                new AlertFruitsetEffectFactsAssembler(), new AlertWhenEndsFactsAssembler(),
                new AlertPlotsAtRiskFactsAssembler(), new IrrigateFactsAssembler(),
                new ClimateFruitsetEffectFactsAssembler(), new HeatEndsFactsAssembler(),
                new SensorsSayFactsAssembler(), new ChillMissingFactsAssembler(), new ChillReachFactsAssembler(),
                new ChillVsLastWinterFactsAssembler(), new ChillIfIncompleteFactsAssembler(),
                new PlotHowIsFactsAssembler(), new PlotTodayFactsAssembler(), new PlotLastSamplingFactsAssembler(),
                new NextYearFactsAssembler(QuestionId.PLOT_NEXT_YEAR_HARVEST),
                new SensorsSayFactsAssembler(QuestionId.SENSORS_WHAT_SAY), new SensorsReliabilityFactsAssembler(),
                new SensorsHowLinkFactsAssembler(), new SamplingEnoughTreesFactsAssembler(),
                new SamplingMeaningFactsAssembler(), new SamplingNextFactsAssembler(),
                new ThinningHarvestChangeFactsAssembler(), new ThinningWhyTargetFactsAssembler(),
                new ThinningNextSamplingFactsAssembler(), new CampaignVsPreviousFactsAssembler(),
                new CampaignPlanNextFactsAssembler()));
    }

    /**
     * Finds the assembler of a question.
     *
     * @param questionId the question
     * @return its assembler, or empty for a question without one
     */
    public Optional<FactsAssembler> find(QuestionId questionId) {
        return Optional.ofNullable(assemblers.get(questionId));
    }
}
