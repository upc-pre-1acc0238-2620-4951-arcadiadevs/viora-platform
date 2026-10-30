package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerModel;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextRejection;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.WhyPercentFactsAssembler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.MessageType;

import java.net.SocketTimeoutException;
import java.util.Locale;

import static com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture.PRODUCER;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.PLOT;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.issuedPlan;
import static com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.AnswerDataSamples.plan;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SpringAiAnswerWriter: model text validated, template text as fallback")
class SpringAiAnswerWriterTest {

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final AnswerModel GROQ = new AnswerModel("groq", "test-model", false);

    private final FakeChatModel chatModel = new FakeChatModel();
    private final SpringAiAnswerWriter writer = new SpringAiAnswerWriter(ChatClient.create(chatModel), "groq",
            "test-model");
    private AnswerDraft draft;

    @BeforeEach
    void setUp() {
        var fixture = new AssistantServiceFixture();
        var facts = new WhyPercentFactsAssembler().assemble(plan(issuedPlan())).orElseThrow();
        var rendered = fixture.renderer.render(facts, ES);
        draft = new AnswerDraft(QuestionId.PLAN_WHY_PERCENT, ES, facts.facts(), rendered.text(), rendered.actions());
    }

    @Test
    @DisplayName("Accepts a text backed by the facts: model metadata, no fallback, tokens of the response")
    void acceptsABackedText() {
        chatModel.replyText("Tu carga está en 0,57 frutos por brote y el objetivo es 0,40. Por eso el plan quita "
                + "el 30 % de los frutos.");

        var written = writer.write(draft);

        assertThat(written.text()).startsWith("Tu carga está en 0,57 frutos por brote y el objetivo es 0,40.");
        assertThat(written.model()).isEqualTo(GROQ);
        assertThat(written.inputTokens()).isEqualTo(210);
        assertThat(written.outputTokens()).isEqualTo(38);
        assertThat(written.rejection()).isNull();
        assertThat(chatModel.prompts()).hasSize(1);
    }

    @Test
    @DisplayName("An invented number falls back to the template, keeping the tokens of the attempt")
    void inventedNumberFallsBack() {
        chatModel.replyText("Tu carga está en 0,6 frutos por brote, así que quita el 30 %.");

        var written = writer.write(draft);

        assertThat(written.text()).isEqualTo(draft.templateText());
        assertThat(written.model()).isEqualTo(AnswerModel.TEMPLATE);
        assertThat(written.rejection()).isEqualTo(TextRejection.UNBACKED_NUMBER);
        assertThat(written.inputTokens()).isEqualTo(210);
        assertThat(written.outputTokens()).isEqualTo(38);
    }

    @Test
    @DisplayName("A text over 280 characters falls back")
    void tooLongFallsBack() {
        chatModel.replyText("Tu carga está en 0,57 frutos por brote y el plan apunta a 0,40 ".repeat(5) + ".");

        var written = writer.write(draft);

        assertThat(written.rejection()).isEqualTo(TextRejection.TOO_LONG);
        assertThat(written.text()).isEqualTo(draft.templateText());
    }

    @Test
    @DisplayName("Irrigation advice falls back: no answer of these screens may advise watering")
    void irrigationFallsBack() {
        chatModel.replyText("Tu carga está en 0,57 frutos por brote. Riega el lote antes de aclarar el 30 %.");

        assertThat(writer.write(draft).rejection()).isEqualTo(TextRejection.FORBIDDEN_PHRASE);
    }

    @Test
    @DisplayName("A failed call and a timeout fall back without tokens")
    void failuresFallBack() {
        chatModel.fail(new IllegalStateException("503 from the provider"));
        var failed = writer.write(draft);
        chatModel.fail(new RuntimeException("request failed", new SocketTimeoutException("timeout")));
        var late = writer.write(draft);

        assertThat(failed.rejection()).isEqualTo(TextRejection.MODEL_ERROR);
        assertThat(failed.model()).isEqualTo(AnswerModel.TEMPLATE);
        assertThat(failed.text()).isEqualTo(draft.templateText());
        assertThat(failed.inputTokens()).isNull();
        assertThat(late.rejection()).isEqualTo(TextRejection.TIMEOUT);
        assertThat(late.text()).isEqualTo(draft.templateText());
    }

    @Test
    @DisplayName("Output that is not the expected JSON falls back as malformed")
    void malformedOutputFallsBack() {
        chatModel.reply("Tu carga está en 0,57 frutos por brote.", 200, 20);
        var plainText = writer.write(draft);
        chatModel.reply("{\"answer\":", 200, 5);
        var brokenJson = writer.write(draft);
        chatModel.reply("{\"other\":\"x\"}", 200, 5);
        var missingText = writer.write(draft);

        assertThat(plainText.rejection()).isEqualTo(TextRejection.MALFORMED_OUTPUT);
        assertThat(plainText.text()).isEqualTo(draft.templateText());
        assertThat(plainText.inputTokens()).isEqualTo(200);
        assertThat(brokenJson.rejection()).isEqualTo(TextRejection.MALFORMED_OUTPUT);
        assertThat(missingText.rejection()).isEqualTo(TextRejection.MALFORMED_OUTPUT);
    }

    @Test
    @DisplayName("The prompt carries the instructions and the facts JSON, and no producer, plot or action target")
    void promptCarriesOnlyTheFacts() {
        chatModel.replyText("Tu carga está en 0,57 frutos por brote.");

        writer.write(draft);

        var prompt = chatModel.prompts().getFirst();
        var system = prompt.getInstructions().stream()
                .filter(message -> message.getMessageType() == MessageType.SYSTEM).findFirst().orElseThrow();
        var user = prompt.getInstructions().stream()
                .filter(message -> message.getMessageType() == MessageType.USER).findFirst().orElseThrow();
        assertThat(system.getText()).contains("trusted field technician", "\"tú\"", "280 characters",
                "Never round", "allowedActions", "dato simulado",
                "Question: why the thinning plan removes percentToRemove",
                "- percentToRemove: percent of the fruits the plan removes",
                "- targetLoad: fruits per shoot the plan aims at");
        assertThat(user.getText()).contains("\"percentToRemove\":30", "\"preThinningLoad\":0.57",
                "\"irrigationAdvisable\":false", "\"allowedActions\":[\"Registrar aclareo\"]",
                "\"questionId\":\"plan.why_percent\"", "\"text\"");
        var whole = prompt.getContents();
        assertThat(whole).doesNotContain(PLOT.plotId(), PRODUCER, "thinning/execute", "@", "coordinates",
                "-18.0", "-70.2");
    }

    @Test
    @DisplayName("English answers ask for English with a decimal point")
    void englishPrompt() {
        var english = new AnswerDraft(draft.questionId(), Locale.ENGLISH, draft.facts(), "Template text.",
                draft.actions());
        chatModel.replyText("Your load is 0.57 fruits per shoot and the target is 0.40, so the plan removes 30 %.");

        var written = writer.write(english);

        assertThat(written.model()).isEqualTo(GROQ);
        assertThat(chatModel.prompts().getFirst().getInstructions().getFirst().getText())
                .contains("plain English", "decimals with a point", "simulated data");
    }
}
