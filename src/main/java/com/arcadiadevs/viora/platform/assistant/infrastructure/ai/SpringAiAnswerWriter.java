package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerWriter;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.ModelFacts;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.WrittenText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerModel;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextRejection;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator;
import com.arcadiadevs.viora.platform.assistant.infrastructure.metrics.AssistantMetrics;
import com.arcadiadevs.viora.platform.assistant.infrastructure.metrics.AssistantMetrics.ModelOutcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;

import java.io.InterruptedIOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

/**
 * Writes the answer text with a chat model through Spring AI. The model gets only the instructions and the facts
 * ({@link ModelFacts}); its JSON output is validated ({@link AnswerTextValidator}) and, when it is malformed,
 * rejected, late, the call fails or the validation itself fails, the template text is used and the answer is marked
 * as a fallback. A {@link ModelCircuitBreaker} skips the call while the model keeps failing. Each attempt logs one
 * line with the question, latency, outcome and tokens, never the facts or the text, and records its metrics.
 */
public class SpringAiAnswerWriter implements AnswerWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAiAnswerWriter.class);

    /** Checks a model text against the facts it was written from; the validator in production. */
    @FunctionalInterface
    interface TextCheck {
        Optional<TextRejection> check(String text, Locale locale, Map<String, ?> facts);
    }

    private final ChatClient chatClient;
    private final AnswerModel model;
    private final ModelCircuitBreaker breaker;
    private final AssistantMetrics metrics;
    private final TextCheck textCheck;
    private final BeanOutputConverter<ModelText> converter = new BeanOutputConverter<>(ModelText.class);

    /**
     * Constructs the writer.
     *
     * @param chatClient the client of the chat model
     * @param provider   the provider, for example {@code groq}
     * @param modelName  the model name
     * @param breaker    stops the calls while the model keeps failing
     * @param metrics    the meters of the model attempts
     */
    public SpringAiAnswerWriter(ChatClient chatClient, String provider, String modelName,
            ModelCircuitBreaker breaker, AssistantMetrics metrics) {
        this(chatClient, provider, modelName, breaker, metrics, AnswerTextValidator::check);
    }

    SpringAiAnswerWriter(ChatClient chatClient, String provider, String modelName, ModelCircuitBreaker breaker,
            AssistantMetrics metrics, TextCheck textCheck) {
        this.chatClient = chatClient;
        this.model = new AnswerModel(provider, modelName, false);
        this.breaker = breaker;
        this.metrics = metrics;
        this.textCheck = textCheck;
    }

    @Override
    public boolean usesModel() {
        return true;
    }

    @Override
    public WrittenText write(AnswerDraft draft) {
        var facts = ModelFacts.of(draft);
        if (!breaker.tryAcquire()) {
            LOGGER.warn("Assistant model attempt: question={} latencyMs=0 outcome=breaker_open",
                    draft.questionId().value());
            metrics.modelAttempt(ModelOutcome.BREAKER_OPEN, TextRejection.BREAKER_OPEN, Duration.ZERO, null, null);
            return fallback(draft, TextRejection.BREAKER_OPEN, null, null);
        }
        long start = System.nanoTime();
        ChatResponse response;
        try {
            response = chatClient.prompt(AnswerPrompt.of(facts, draft.locale(), converter.getFormat()))
                    .call().chatResponse();
        } catch (RuntimeException exception) {
            breaker.onFailure();
            var reason = isTimeout(exception) ? TextRejection.TIMEOUT : TextRejection.MODEL_ERROR;
            long latency = elapsedMs(start);
            LOGGER.warn("Assistant model attempt: question={} latencyMs={} outcome=error:{} type={}",
                    draft.questionId().value(), latency, reason, exception.getClass().getSimpleName());
            metrics.modelAttempt(ModelOutcome.ERROR, reason, Duration.ofMillis(latency), null, null);
            return fallback(draft, reason, null, null);
        }
        long latency = elapsedMs(start);
        Integer inputTokens = tokens(response, true);
        Integer outputTokens = tokens(response, false);
        var text = textOf(response);
        if (text == null) {
            breaker.onFailure();
            return rejected(draft, ModelOutcome.ERROR, TextRejection.MALFORMED_OUTPUT, latency, inputTokens,
                    outputTokens);
        }
        // The model answered: whatever the validator says, it is working
        breaker.onSuccess();
        Optional<TextRejection> rejection;
        try {
            rejection = textCheck.check(text, draft.locale(), facts);
        } catch (RuntimeException exception) {
            LOGGER.warn("Assistant validation failed: question={} type={}", draft.questionId().value(),
                    exception.getClass().getSimpleName());
            return rejected(draft, ModelOutcome.ERROR, TextRejection.VALIDATION_ERROR, latency, inputTokens,
                    outputTokens);
        }
        if (rejection.isPresent()) {
            return rejected(draft, ModelOutcome.REJECTED, rejection.get(), latency, inputTokens, outputTokens);
        }
        LOGGER.info("Assistant model attempt: question={} latencyMs={} outcome=accepted tokens={}/{}",
                draft.questionId().value(), latency, inputTokens, outputTokens);
        metrics.modelAttempt(ModelOutcome.ACCEPTED, null, Duration.ofMillis(latency), inputTokens, outputTokens);
        return new WrittenText(text.strip(), model, inputTokens, outputTokens, null);
    }

    private WrittenText rejected(AnswerDraft draft, ModelOutcome outcome, TextRejection reason, long latency,
            Integer inputTokens, Integer outputTokens) {
        LOGGER.warn("Assistant model attempt: question={} latencyMs={} outcome=rejected:{} tokens={}/{}",
                draft.questionId().value(), latency, reason, inputTokens, outputTokens);
        metrics.modelAttempt(outcome, reason, Duration.ofMillis(latency), inputTokens, outputTokens);
        return fallback(draft, reason, inputTokens, outputTokens);
    }

    /** The text of the structured output, or null when the output is not the expected JSON. */
    private String textOf(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return null;
        }
        var raw = response.getResult().getOutput().getText();
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            var output = converter.convert(raw);
            return output == null ? null : output.text();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static WrittenText fallback(AnswerDraft draft, TextRejection reason, Integer inputTokens,
            Integer outputTokens) {
        return new WrittenText(draft.templateText(), AnswerModel.TEMPLATE, inputTokens, outputTokens, reason);
    }

    private static Integer tokens(ChatResponse response, boolean input) {
        if (response == null || response.getMetadata() == null) {
            return null;
        }
        Usage usage = response.getMetadata().getUsage();
        if (usage == null) {
            return null;
        }
        Integer count = input ? usage.getPromptTokens() : usage.getCompletionTokens();
        return count == null || count <= 0 ? null : count;
    }

    private static boolean isTimeout(Throwable exception) {
        // SocketTimeoutException is an InterruptedIOException; the SDK wraps it in its own exceptions
        int depth = 0;
        for (Throwable cause = exception; cause != null && depth < 10; cause = cause.getCause(), depth++) {
            if (cause instanceof InterruptedIOException || cause instanceof TimeoutException
                    || cause.getClass().getSimpleName().contains("Timeout")) {
                return true;
            }
        }
        return false;
    }

    private static long elapsedMs(long start) {
        return (System.nanoTime() - start) / 1_000_000;
    }
}
