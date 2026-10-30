package com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.purge;

import com.arcadiadevs.viora.platform.assistant.domain.repositories.AssistantAnswerRepository;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Removes the answers of a deleted account. They hold figures of the producer's plots, so they leave with it.
 */
@Component
public class AssistantAccountDataPurger {

    private final AssistantAnswerRepository answers;

    /**
     * Creates the purger.
     *
     * @param answers the saved answers
     */
    public AssistantAccountDataPurger(AssistantAnswerRepository answers) {
        this.answers = answers;
    }

    /**
     * Removes the assistant data of the account.
     *
     * @param event the deleted account
     */
    @EventListener
    public void on(UserAccountDeletedEvent event) {
        answers.deleteAllByProducerId(event.userId());
    }
}
