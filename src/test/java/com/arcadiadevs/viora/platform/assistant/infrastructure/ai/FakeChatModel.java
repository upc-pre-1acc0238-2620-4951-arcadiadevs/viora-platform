package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;

/** A chat model without network: answers with the configured output or failure and records every prompt. */
public class FakeChatModel implements ChatModel {

    private final List<Prompt> prompts = new CopyOnWriteArrayList<>();
    private volatile Supplier<ChatResponse> next = () -> response("{\"text\":\"\"}", null, null);

    /** Answers with this raw output and token counts. */
    public void reply(String rawOutput, Integer inputTokens, Integer outputTokens) {
        next = () -> response(rawOutput, inputTokens, outputTokens);
    }

    /** Answers with a JSON {@code {"text": ...}} carrying this text. */
    public void replyText(String text) {
        reply("{\"text\":\"" + text.replace("\"", "\\\"") + "\"}", 210, 38);
    }

    /** Fails with this exception. */
    public void fail(RuntimeException exception) {
        next = () -> {
            throw exception;
        };
    }

    public List<Prompt> prompts() {
        return List.copyOf(prompts);
    }

    public void reset() {
        prompts.clear();
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        prompts.add(prompt);
        return next.get();
    }

    private static ChatResponse response(String rawOutput, Integer inputTokens, Integer outputTokens) {
        var metadata = ChatResponseMetadata.builder()
                .usage(new DefaultUsage(inputTokens, outputTokens))
                .build();
        return new ChatResponse(List.of(new Generation(new AssistantMessage(rawOutput))), metadata);
    }
}
