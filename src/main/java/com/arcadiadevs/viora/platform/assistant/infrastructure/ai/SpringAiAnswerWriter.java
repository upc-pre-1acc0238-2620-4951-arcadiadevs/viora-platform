package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerWriter;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.ModelFacts;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.WrittenText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerModel;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextRejection;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.converter.BeanOutputConverter;

import java.io.InterruptedIOException;
import java.util.Optional;
import java.util.concurrent.TimeoutException;

/**
 * Writes the answer text with a chat model through Spring AI. The model gets only the instructions and the facts
 * ({@link ModelFacts}); its JSON output is validated ({@link AnswerTextValidator}) and, when it is malformed,
 * rejected, late or the call fails, the template text is used and the answer is marked as a fallback. Each attempt
 * logs one line with the question, latency, outcome and tokens, never the facts or the text.
 */
public class SpringAiAnswerWriter implements AnswerWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringAiAnswerWriter.class);

    private final ChatClient chatClient;
    private final AnswerModel model;
    private final BeanOutputConverter<ModelText> converter = new BeanOutputConverter<>(ModelText.class);

    /**
     * Constructs the writer.
     *
     * @param chatClient the client of the chat model
     * @param provider   the provider, for example {@code groq}
     * @param modelName  the model name
     */
    public SpringAiAnswerWriter(ChatClient chatClient, String provider, String modelName) {
        this.chatClient = chatClient;
        this.model = new AnswerModel(provider, modelName, false);
    }

    @Override
    public WrittenText write(AnswerDraft draft) {
        var facts = ModelFacts.of(draft);
        long start = System.nanoTime();
        ChatResponse response;
        try {
            response = chatClient.prompt(AnswerPrompt.of(facts, draft.locale(), converter.getFormat()))
                    .call().chatResponse();
        } catch (RuntimeException exception) {
            var reason = isTimeout(exception) ? TextRejection.TIMEOUT : TextRejection.MODEL_ERROR;
            LOGGER.warn("Assistant model attempt: question={} latencyMs={} outcome=error:{} type={}",
                    draft.questionId().value(), elapsedMs(start), reason, exception.getClass().getSimpleName());
            return fallback(draft, reason, null, null);
        }
        long latency = elapsedMs(start);
        Integer inputTokens = tokens(response, true);
        Integer outputTokens = tokens(response, false);
        var text = textOf(response);
        var rejection = text == null
                ? Optional.of(TextRejection.MALFORMED_OUTPUT)
                : AnswerTextValidator.check(text, draft.locale(), facts);
        if (rejection.isPresent()) {
            LOGGER.warn("Assistant model attempt: question={} latencyMs={} outcome=rejected:{} tokens={}/{}",
                    draft.questionId().value(), latency, rejection.get(), inputTokens, outputTokens);
            return fallback(draft, rejection.get(), inputTokens, outputTokens);
        }
        LOGGER.info("Assistant model attempt: question={} latencyMs={} outcome=accepted tokens={}/{}",
                draft.questionId().value(), latency, inputTokens, outputTokens);
        return new WrittenText(text.strip(), model, inputTokens, outputTokens, null);
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
