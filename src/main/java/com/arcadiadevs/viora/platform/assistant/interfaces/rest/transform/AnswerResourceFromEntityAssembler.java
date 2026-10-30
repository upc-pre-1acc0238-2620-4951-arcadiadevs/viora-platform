package com.arcadiadevs.viora.platform.assistant.interfaces.rest.transform;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerView;
import com.arcadiadevs.viora.platform.assistant.interfaces.rest.resources.AnswerResource;
import org.jspecify.annotations.NullMarked;

/**
 * Assembler turning a stored answer into its REST resource. The texts were localized when the answer was created.
 */
@NullMarked
public final class AnswerResourceFromEntityAssembler {

    private AnswerResourceFromEntityAssembler() {
    }

    /**
     * Converts an answer with its freshness into its resource.
     *
     * @param view the answer
     * @return the resource
     */
    public static AnswerResource toResource(AnswerView view) {
        var answer = view.answer();
        return new AnswerResource(
                answer.id().toString(),
                answer.questionId().value(),
                answer.plotId().plotId(),
                answer.createdAt().toString(),
                answer.dataVersion().value(),
                view.freshness().name(),
                answer.text(),
                answer.facts().stream()
                        .map(fact -> new AnswerResource.AnswerFactResource(fact.value(), fact.label(), fact.pill(),
                                fact.tone().name()))
                        .toList(),
                answer.sources().stream().map(source -> new AnswerResource.AnswerSourceResource(source.label()))
                        .toList(),
                answer.actions().stream()
                        .map(action -> new AnswerResource.AnswerActionResource(action.type(), action.label(),
                                action.target()))
                        .toList(),
                new AnswerResource.AnswerModelResource(answer.model().provider(), answer.model().name(),
                        answer.model().fallback()));
    }
}
