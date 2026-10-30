package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerWriter;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.TemplateAnswerWriter;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Chooses the writer of the answer texts with {@code viora.assistant.provider}: {@code template} (default, no
 * model, no key) or {@code groq}, a Spring AI chat model on the OpenAI compatible API of Groq. The groq writer needs
 * the OpenAI chat auto-configuration ({@code spring.ai.model.chat=openai}); the application fails to start without it
 * instead of silently answering with templates.
 */
@Configuration
public class AssistantWriterConfiguration {

    /** Provider of the model writer. */
    public static final String GROQ = "groq";

    /**
     * The template writer.
     *
     * @return the writer
     */
    @Bean
    @ConditionalOnProperty(name = "viora.assistant.provider", havingValue = "template", matchIfMissing = true)
    public AnswerWriter templateAnswerWriter() {
        return new TemplateAnswerWriter();
    }

    /**
     * The Groq writer, with the template text as fallback.
     *
     * @param chatModel the chat model of the OpenAI auto-configuration
     * @param modelName the model name
     * @return the writer
     */
    @Bean
    @ConditionalOnProperty(name = "viora.assistant.provider", havingValue = GROQ)
    public AnswerWriter groqAnswerWriter(ObjectProvider<ChatModel> chatModel,
            @Value("${viora.assistant.model}") String modelName) {
        var model = chatModel.getIfAvailable();
        if (model == null) {
            throw new IllegalStateException(
                    "viora.assistant.provider=groq needs a chat model: set spring.ai.model.chat=openai");
        }
        return new SpringAiAnswerWriter(ChatClient.create(model), GROQ, modelName);
    }
}
