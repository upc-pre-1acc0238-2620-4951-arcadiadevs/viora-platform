package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import java.util.Map;
import java.util.Optional;

/**
 * What each closed question asks and what each fact means, so the model keeps every number with its meaning. Only
 * the question asked and the facts present go in the prompt.
 */
final class FactGlossary {

    private static final Map<String, String> QUESTIONS = Map.of(
            "alternation.why_index", "why the alternate bearing index of the plot has its value",
            "alternation.break_cycle", "what can break the alternate bearing; thinning is the only measure known",
            "alternation.vs_campaigns", "how the last campaign compares with the average of the previous ones",
            "alternation.next_year", "what the harvest of this campaign is expected to be after thinning",
            "plan.why_percent", "why the thinning plan removes percentToRemove of the fruits",
            "plan.if_less", "what happens if the producer removes only lessPercent instead of percentToRemove",
            "plan.if_none", "what happens if the producer does not thin",
            "plan.best_time", "when to thin, from the window of the plan");

    private static final Map<String, String> FACTS = Map.ofEntries(
            Map.entry("bearingIndex", "alternate bearing index; higher means stronger alternation"),
            Map.entry("bearingBand", "band of the index"),
            Map.entry("harvestCampaigns", "campaigns with a recorded harvest"),
            Map.entry("lastCampaignYear", "year of the last recorded harvest"),
            Map.entry("lastCampaignKg", "kg harvested in lastCampaignYear"),
            Map.entry("previousCampaigns", "campaigns before the last one"),
            Map.entry("previousAverageKg", "average kg of the previous campaigns"),
            Map.entry("differencePercent", "percent the last campaign is above or below previousAverageKg"),
            Map.entry("trend", "last campaign against the previous average"),
            Map.entry("prescriptionIssued", "whether a thinning plan was issued"),
            Map.entry("percentToRemove", "percent of the fruits the plan removes"),
            Map.entry("preThinningLoad", "fruits per shoot measured before thinning"),
            Map.entry("targetLoad", "fruits per shoot the plan aims at"),
            Map.entry("loadAfterPlan", "fruits per shoot left after removing percentToRemove"),
            Map.entry("lessPercent", "smaller percent the producer asks about"),
            Map.entry("loadAfterLess", "fruits per shoot left after removing only lessPercent"),
            Map.entry("loadRatio", "preThinningLoad divided by targetLoad"),
            Map.entry("windowOpensOn", "first day of the thinning window"),
            Map.entry("windowClosesOn", "last day of the thinning window"),
            Map.entry("windowState", "where the window stands today"),
            Map.entry("daysLeft", "days left in the open window, today included"),
            Map.entry("daysUntilOpen", "days until the window opens"),
            Map.entry("sampledTrees", "trees sampled"),
            Map.entry("lastSampledOn", "day of the latest sampling"),
            Map.entry("thinningRecordedOn", "day the thinning was recorded"),
            Map.entry("campaignYear", "current campaign year"),
            Map.entry("mostLikelyFruitsPerKg", "expected fruits per kilo after thinning"),
            Map.entry("commercialSizeGrade", "expected commercial size grade"),
            Map.entry("estimatedKgPerHa", "expected harvest in kg per hectare after thinning"));

    private FactGlossary() {
    }

    static Optional<String> question(String questionId) {
        return questionId == null ? Optional.empty() : Optional.ofNullable(QUESTIONS.get(questionId));
    }

    static Optional<String> fact(String name) {
        return name == null ? Optional.empty() : Optional.ofNullable(FACTS.get(name));
    }
}
