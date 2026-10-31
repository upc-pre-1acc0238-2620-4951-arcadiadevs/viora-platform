package com.arcadiadevs.viora.platform.assistant.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources.AnswerHistoryResource;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources.AssistantAnswerResource;
import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * Assembler turning a saved answer into its REST resource.
 */
@NullMarked
public final class AssistantAnswerResourceFromEntityAssembler {

    private AssistantAnswerResourceFromEntityAssembler() {
    }

    /**
     * Converts an answer into its resource.
     *
     * @param view the answer with its freshness
     * @return the resource
     */
    public static AssistantAnswerResource toResource(AnswerView view) {
        var answer = view.answer();
        return new AssistantAnswerResource(
                answer.id().value(),
                answer.questionId().value(),
                answer.question(),
                answer.plotId().plotId(),
                answer.createdAt(),
                answer.dataVersion().value(),
                view.freshness().name(),
                answer.text(),
                answer.facts().stream()
                        .map(fact -> new AssistantAnswerResource.Fact(fact.value(), fact.label(), fact.pill(),
                                fact.tone().name()))
                        .toList(),
                answer.sources().stream().map(source -> new AssistantAnswerResource.Source(source.label())).toList(),
                answer.actions().stream()
                        .map(action -> new AssistantAnswerResource.Action(action.type(), action.label(), action.target()))
                        .toList(),
                new AssistantAnswerResource.Model(answer.provider(), answer.modelName(), answer.fallback()));
    }

    /**
     * Converts the history into its resource.
     *
     * @param views the answers, newest first
     * @return the resource
     */
    public static AnswerHistoryResource toHistoryResource(List<AnswerView> views) {
        return new AnswerHistoryResource(views.stream()
                .map(AssistantAnswerResourceFromEntityAssembler::toResource).toList());
    }
}
