package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.ai.AnswerTextWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Spring AI adapter of the language model that rephrases the answers. The provider is configured with the
 * {@code spring.ai.openai.*} properties: Groq through its OpenAI compatible API by default. Without an API key the
 * adapter reports itself disabled and is never called.
 *
 * <p>The messages are built here as plain text, not with the prompt template engine, because the cards may carry
 * characters the engine would read as placeholders.</p>
 */
@Component
public class SpringAiAnswerTextWriter implements AnswerTextWriter {

    private static final Logger LOG = LoggerFactory.getLogger(SpringAiAnswerTextWriter.class);

    /**
     * What the model is for and what it must never do, in the language of the answer: rules written in the language
     * keep the model from drifting into the other one.
     */
    static final String SYSTEM_SPANISH = """
            Eres la voz de Viora, asistente de olivicultores en Perú.
            Reescribe el TEXTO BASE en 2 o 3 frases cortas en español neutro, cercanas y técnicas.
            Tutea al productor.
            Reglas:
            - Usa solo las cifras que aparecen en HECHOS o en el TEXTO BASE, escritas igual.
            - Nunca calcules, redondees, conviertas ni agregues cifras.
            - No agregues consejos, dosis, riego, productos, promesas ni fechas que no estén en el TEXTO BASE.
            - Conserva el sentido y la cautela del TEXTO BASE: si dice "tiende a", "puede" o "se espera", mantenlo.
            - Nunca conviertas una tendencia en una certeza.
            - Solo texto plano: sin listas, sin markdown, sin comillas, sin saludo ni despedida.
            - Máximo 280 caracteres. Si dudas, devuelve el TEXTO BASE tal cual.""";

    static final String SYSTEM_ENGLISH = """
            You are the voice of Viora, an assistant for olive growers in Peru.
            Rewrite BASE TEXT as 2 or 3 short sentences in English only: warm, plain and technical.
            Speak directly to the grower.
            Rules:
            - Use only numbers that appear in FACTS or BASE TEXT, written exactly as they appear there.
            - Never calculate, round, convert or add a number.
            - Do not add advice, doses, irrigation, products, promises or dates that are not in BASE TEXT.
            - Keep the meaning and the caution of BASE TEXT: if it says "tends to", "can" or "is expected", keep it.
            - Never turn a tendency into a certainty.
            - Plain text only: no lists, no markdown, no quotes, no greeting, no sign-off.
            - At most 280 characters. When in doubt, return BASE TEXT unchanged.""";

    private final ChatClient chatClient;
    private final boolean enabled;
    private final String provider;
    private final String model;

    /**
     * Creates the adapter.
     *
     * @param chatClientBuilder the chat client of the configured model
     * @param enabledByConfig   {@code viora.assistant.model.enabled}: lets an environment turn the model off
     * @param apiKey            the key of the provider; blank leaves the model off
     * @param provider          {@code viora.assistant.model.provider}, for example GROQ
     * @param model             {@code spring.ai.openai.chat.model}
     */
    public SpringAiAnswerTextWriter(
            ChatClient.Builder chatClientBuilder,
            @Value("${viora.assistant.model.enabled:true}") boolean enabledByConfig,
            @Value("${GROQ_API_KEY:}") String apiKey,
            @Value("${viora.assistant.model.provider:GROQ}") String provider,
            @Value("${spring.ai.openai.chat.model:}") String model
    ) {
        this.chatClient = chatClientBuilder.build();
        this.enabled = enabledByConfig && apiKey != null && !apiKey.isBlank();
        this.provider = provider;
        this.model = model;
        LOG.info("Assistant language model {}", enabled ? "on: " + provider + " " + model : "off: answers use templates");
    }

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public String provider() {
        return provider;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    public Optional<Written> rewrite(Request request) {
        if (!enabled) {
            return Optional.empty();
        }
        try {
            var response = chatClient.prompt()
                    .system(english(request) ? SYSTEM_ENGLISH : SYSTEM_SPANISH)
                    .user(userMessage(request))
                    .call()
                    .chatResponse();
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return Optional.empty();
            }
            var usage = response.getMetadata() == null ? null : response.getMetadata().getUsage();
            return Optional.of(new Written(
                    response.getResult().getOutput().getText(),
                    usage == null ? null : usage.getPromptTokens(),
                    usage == null ? null : usage.getCompletionTokens()));
        } catch (RuntimeException e) {
            // Only the kind of failure: the request carries the figures of a producer's plot
            LOG.warn("Assistant language model failed: {}", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    /** The question, the cards one per line and the template, labelled in the language of the answer. */
    static String userMessage(Request request) {
        var facts = request.facts().stream()
                .map(fact -> "- " + fact.value() + " | " + fact.label() + " | " + fact.pill())
                .collect(Collectors.joining("\n"));
        return english(request)
                ? "QUESTION: " + request.question() + "\nFACTS:\n" + facts + "\nBASE TEXT: " + request.template()
                : "PREGUNTA: " + request.question() + "\nHECHOS:\n" + facts + "\nTEXTO BASE: " + request.template();
    }

    private static boolean english(Request request) {
        return "en".equals(request.language().code());
    }
}
