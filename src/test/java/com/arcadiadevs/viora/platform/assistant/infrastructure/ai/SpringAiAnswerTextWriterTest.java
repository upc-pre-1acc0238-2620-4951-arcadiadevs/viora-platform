package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.ai.AnswerTextWriter;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.DefaultUsage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpringAiAnswerTextWriter: the adapter of the language model, with a fake model and no network")
class SpringAiAnswerTextWriterTest {

    private static final AnswerTextWriter.Request REQUEST = new AnswerTextWriter.Request(
            AnswerLanguage.SPANISH,
            "¿Por qué 30 %?",
            "Tu carga está en 0,60 frutos por brote y el árbol sostiene 0,40.",
            List.of(new AnswerFact("0,60", "frutos por brote · estimado", "objetivo 0,40", FactTone.PRIMARY),
                    new AnswerFact("−30 %", "de los frutos", "ventana hasta el 14 nov", FactTone.HIGHLIGHT)));

    /** A chat model that answers with a function of the prompt and remembers the prompts it got. */
    private static final class FakeChatModel implements ChatModel {
        private final Function<Prompt, ChatResponse> answer;
        private final List<Prompt> prompts = new ArrayList<>();

        FakeChatModel(Function<Prompt, ChatResponse> answer) {
            this.answer = answer;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);
            return answer.apply(prompt);
        }
    }

    private static ChatResponse reply(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))),
                ChatResponseMetadata.builder().usage(new DefaultUsage(210, 52)).build());
    }

    private static SpringAiAnswerTextWriter writer(ChatModel model, boolean enabled, String apiKey) {
        return new SpringAiAnswerTextWriter(ChatClient.builder(model), enabled, apiKey, "GROQ", "openai/gpt-oss-120b");
    }

    @Test
    @DisplayName("sends the rules, the question, the cards and the template, and returns the text with its tokens")
    void rewritesWithTheModel() {
        var model = new FakeChatModel(prompt -> reply("Llevas 0,60 frutos por brote y el árbol sostiene 0,40."));
        var writer = writer(model, true, "gsk_test");

        var written = writer.rewrite(REQUEST);

        assertThat(written).contains(new AnswerTextWriter.Written(
                "Llevas 0,60 frutos por brote y el árbol sostiene 0,40.", 210, 52));
        var messages = model.prompts.getFirst().getInstructions();
        assertThat(messages).extracting(message -> message.getMessageType())
                .containsExactly(MessageType.SYSTEM, MessageType.USER);
        assertThat(messages.get(0).getText()).contains("español neutro").contains("Nunca calcules")
                .contains("Nunca conviertas una tendencia en una certeza");
        assertThat(messages.get(1).getText()).isEqualTo("""
                PREGUNTA: ¿Por qué 30 %?
                HECHOS:
                - 0,60 | frutos por brote · estimado | objetivo 0,40
                - −30 % | de los frutos | ventana hasta el 14 nov
                TEXTO BASE: Tu carga está en 0,60 frutos por brote y el árbol sostiene 0,40.""");
    }

    @Test
    @DisplayName("asks for English when the answer is in English")
    void englishAnswer() {
        var model = new FakeChatModel(prompt -> reply("ok"));
        var english = new AnswerTextWriter.Request(new AnswerLanguage("en"), "Why 30%?", "Base.", REQUEST.facts());

        writer(model, true, "gsk_test").rewrite(english);

        var messages = model.prompts.getFirst().getInstructions();
        assertThat(messages.get(0).getText()).contains("in English only").doesNotContain("tú");
        assertThat(messages.get(1).getText()).startsWith("QUESTION: Why 30%?").contains("BASE TEXT: Base.");
    }

    @Test
    @DisplayName("a failing model returns nothing instead of throwing")
    void modelFailure() {
        var model = new FakeChatModel(prompt -> {
            throw new IllegalStateException("429 Too Many Requests");
        });

        assertThat(writer(model, true, "gsk_test").rewrite(REQUEST)).isEmpty();
    }

    @Test
    @DisplayName("without a key, or turned off, the model is off and never called")
    void offWithoutAKey() {
        var model = new FakeChatModel(prompt -> reply("never"));

        var withoutKey = writer(model, true, " ");
        var turnedOff = writer(model, false, "gsk_test");

        assertThat(withoutKey.enabled()).isFalse();
        assertThat(turnedOff.enabled()).isFalse();
        assertThat(withoutKey.rewrite(REQUEST)).isEmpty();
        assertThat(turnedOff.rewrite(REQUEST)).isEmpty();
        assertThat(model.prompts).isEmpty();
    }

}
