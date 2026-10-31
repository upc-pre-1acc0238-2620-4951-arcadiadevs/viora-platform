package com.arcadiadevs.viora.platform.assistant.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.assistant.application.commandservices.DailySummaryCommandService;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.Freshness;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources.AssistantAnswerResource;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources.DailySummaryResource;
import org.jspecify.annotations.NullMarked;

/**
 * Assembler turning a daily summary into its REST resource.
 */
@NullMarked
public final class DailySummaryResourceFromEntityAssembler {

    private DailySummaryResourceFromEntityAssembler() {
    }

    /**
     * Converts the summary served into its resource.
     *
     * @param outcome the summary of the day
     * @return the resource
     */
    public static DailySummaryResource toResource(DailySummaryCommandService.Outcome outcome) {
        var summary = outcome.summary();
        return new DailySummaryResource(
                summary.id().value(),
                QuestionId.HOME_DAILY_SUMMARY.value(),
                summary.plotId() == null ? null : summary.plotId().plotId(),
                summary.createdAt(),
                summary.dataVersion().value(),
                Freshness.UP_TO_DATE.name(),
                summary.text(),
                summary.facts().stream()
                        .map(fact -> new AssistantAnswerResource.Fact(fact.value(), fact.label(), fact.pill(),
                                fact.tone().name()))
                        .toList(),
                summary.sources().stream().map(source -> new AssistantAnswerResource.Source(source.label())).toList(),
                new AssistantAnswerResource.Model(summary.provider(), summary.modelName(), summary.fallback()));
    }
}
