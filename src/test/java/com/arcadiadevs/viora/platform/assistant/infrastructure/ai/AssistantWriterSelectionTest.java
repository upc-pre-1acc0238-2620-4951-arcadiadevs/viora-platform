package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerWriter;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.TemplateAnswerWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/** The whole application with the default provider and no GROQ_API_KEY: templates only, no chat model at all. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantwriter;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.ai.openai.api-key="
})
@DisplayName("Assistant writer with provider=template")
class AssistantWriterSelectionTest {

    @Autowired ApplicationContext context;
    @Autowired AnswerWriter writer;

    @Test
    @DisplayName("Boots without a key, writes with the templates and creates no chat model")
    void templateProviderNeedsNoModel() {
        assertThat(context.getEnvironment().getProperty("viora.assistant.provider")).isEqualTo("template");
        assertThat(writer).isInstanceOf(TemplateAnswerWriter.class);
        assertThat(context.getBeanProvider(ChatModel.class).getIfAvailable()).isNull();
        assertThat(context.getBeansOfType(SpringAiAnswerWriter.class)).isEmpty();
    }
}
