package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.infrastructure.metrics.AssistantMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.convert.ConversionService;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

/** The writer configuration with provider=groq and no chat model refuses to start instead of using templates. */
@DisplayName("Assistant writer with provider=groq and no chat model")
class AssistantGroqWithoutModelTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(AssistantWriterConfiguration.class)
            // Converts PT2M to a Duration, as the application does
            .withBean("conversionService", ConversionService.class, ApplicationConversionService::getSharedInstance)
            .withBean(Clock.class, Clock::systemUTC)
            .withBean(AssistantMetrics.class, () -> new AssistantMetrics(new SimpleMeterRegistry()))
            .withPropertyValues("viora.assistant.provider=groq", "viora.assistant.model=test-model");

    @Test
    @DisplayName("Fails to start and says how to turn the chat model on")
    void refusesToStart() {
        runner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).rootCause()
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("viora.assistant.provider=groq needs a chat model: set spring.ai.model.chat=openai");
        });
    }

    @Test
    @DisplayName("Starts with a chat model and wraps it with the breaker")
    void startsWithAModel() {
        runner.withBean(FakeChatModel.class, FakeChatModel::new).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(SpringAiAnswerWriter.class);
        });
    }
}
