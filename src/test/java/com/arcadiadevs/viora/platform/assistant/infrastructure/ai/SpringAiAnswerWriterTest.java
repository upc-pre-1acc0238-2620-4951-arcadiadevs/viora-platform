package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.application.internal.AssistantServiceFixture;
import com.arcadiadevs.viora.platform.assistant.application.internal.MutableClock;
import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerModel;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextRejection;
import com.arcadiadevs.viora.platform.assistant.domain.services.assemblers.WhyPercentFactsAssembler;
import com.arcadiadevs.viora.platform.assistant.infrastructure.metrics.AssistantMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.MessageType;

import java.net.SocketTimeoutException;
import java.time.Duration;
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
    private final MutableClock clock = new MutableClock(AssistantServiceFixture.T0);
    private final ModelCircuitBreaker breaker = new ModelCircuitBreaker(5, Duration.ofMinutes(2), clock);
    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();
    private final SpringAiAnswerWriter writer = new SpringAiAnswerWriter(ChatClient.create(chatModel), "groq",
            "test-model", breaker, new AssistantMetrics(meters));
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

    @Test
    @DisplayName("An exception inside the validation falls back to the template as VALIDATION_ERROR, never a 5xx")
    void validationErrorFallsBack() {
        var failingCheck = new SpringAiAnswerWriter(ChatClient.create(chatModel), "groq", "test-model", breaker,
                new AssistantMetrics(meters), (text, locale, facts) -> {
                    throw new IllegalStateException("validator bug");
                });
        chatModel.replyText("Tu carga está en 0,57 frutos por brote.");

        var written = failingCheck.write(draft);

        assertThat(written.rejection()).isEqualTo(TextRejection.VALIDATION_ERROR);
        assertThat(written.model()).isEqualTo(AnswerModel.TEMPLATE);
        assertThat(written.text()).isEqualTo(draft.templateText());
        assertThat(written.inputTokens()).isEqualTo(210);
        assertThat(model("error", "validation_error")).isEqualTo(1);
    }

    @Test
    @DisplayName("Five model failures in a row open the breaker: the template is used and the model is not called")
    void openBreakerSkipsTheModel() {
        chatModel.fail(new IllegalStateException("503 from the provider"));
        for (int i = 0; i < 5; i++) {
            writer.write(draft);
        }
        chatModel.replyText("Tu carga está en 0,57 frutos por brote.");

        var skipped = writer.write(draft);

        assertThat(skipped.rejection()).isEqualTo(TextRejection.BREAKER_OPEN);
        assertThat(skipped.model()).isEqualTo(AnswerModel.TEMPLATE);
        assertThat(skipped.text()).isEqualTo(draft.templateText());
        assertThat(chatModel.prompts()).hasSize(5);
        assertThat(model("error", "model_error")).isEqualTo(5);
        assertThat(model("breaker_open", "breaker_open")).isEqualTo(1);
    }

    @Test
    @DisplayName("Once the breaker time passes, one trial call goes through and a success closes it again")
    void halfOpenRecovers() {
        chatModel.fail(new IllegalStateException("503 from the provider"));
        for (int i = 0; i < 5; i++) {
            writer.write(draft);
        }
        clock.advance(Duration.ofMinutes(2));
        chatModel.replyText("Tu carga está en 0,57 frutos por brote.");

        var trial = writer.write(draft);
        var next = writer.write(draft);

        assertThat(trial.model()).isEqualTo(GROQ);
        assertThat(next.model()).isEqualTo(GROQ);
        assertThat(breaker.state()).isEqualTo(ModelCircuitBreaker.State.CLOSED);
        assertThat(chatModel.prompts()).hasSize(7);
    }

    @Test
    @DisplayName("Validator rejections are a working model: they never open the breaker")
    void rejectionsDoNotOpenTheBreaker() {
        chatModel.replyText("Tu carga está en 0,6 frutos por brote.");
        for (int i = 0; i < 6; i++) {
            assertThat(writer.write(draft).rejection()).isEqualTo(TextRejection.UNBACKED_NUMBER);
        }

        assertThat(breaker.state()).isEqualTo(ModelCircuitBreaker.State.CLOSED);
        assertThat(chatModel.prompts()).hasSize(6);
        assertThat(model("rejected", "unbacked_number")).isEqualTo(6);
    }

    @Test
    @DisplayName("Malformed output counts as a model failure for the breaker")
    void malformedOutputOpensTheBreaker() {
        chatModel.reply("not json", 200, 5);
        for (int i = 0; i < 5; i++) {
            writer.write(draft);
        }

        assertThat(breaker.state()).isEqualTo(ModelCircuitBreaker.State.OPEN);
        assertThat(writer.write(draft).rejection()).isEqualTo(TextRejection.BREAKER_OPEN);
    }

    @Test
    @DisplayName("Records the accepted attempt and its tokens, tagged only by outcome, reason and type")
    void recordsTheModelMetrics() {
        chatModel.replyText("Tu carga está en 0,57 frutos por brote.");

        writer.write(draft);

        assertThat(model("accepted", "none")).isEqualTo(1);
        assertThat(meters.get(AssistantMetrics.TOKENS).tag("type", "input").summary().totalAmount()).isEqualTo(210);
        assertThat(meters.get(AssistantMetrics.TOKENS).tag("type", "output").summary().totalAmount()).isEqualTo(38);
        assertThat(meters.getMeters()).allSatisfy(meter -> assertThat(meter.getId().getTags().toString())
                .doesNotContain(PRODUCER, PLOT.plotId()));
    }

    private long model(String outcome, String reason) {
        var timer = meters.find(AssistantMetrics.MODEL).tag("outcome", outcome).tag("reason", reason).timer();
        return timer == null ? 0 : timer.count();
    }
}
