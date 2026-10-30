package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.BestTimeFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.BreakCycleFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.IfLessFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.IfNoneFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.NextYearFactsAssembler;
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
     * The assemblers of the ALTERNATION and PLAN questions.
     *
     * @return the registry
     */
    public static FactsAssemblerRegistry standard() {
        return new FactsAssemblerRegistry(List.of(
                new WhyIndexFactsAssembler(), new BreakCycleFactsAssembler(), new VsCampaignsFactsAssembler(),
                new NextYearFactsAssembler(), new WhyPercentFactsAssembler(), new IfLessFactsAssembler(),
                new IfNoneFactsAssembler(), new BestTimeFactsAssembler()));
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
