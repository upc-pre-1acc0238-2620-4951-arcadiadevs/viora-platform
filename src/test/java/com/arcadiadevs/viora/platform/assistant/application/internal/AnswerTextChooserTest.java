package com.arcadiadevs.viora.platform.assistant.application.internal;

import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.ai.AnswerTextWriter;
import com.arcadiadevs.viora.platform.assistant.domain.model.aggregates.AssistantAnswer;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("AnswerTextChooser: the text of the model or the template")
class AnswerTextChooserTest {

    private static final String TEMPLATE = "Sin aclarear te quedas en 0,60 frutos por brote, 50 % por encima de lo que "
            + "el árbol sostiene (0,40).";
    private static final List<AnswerFact> FACTS = List.of(
            new AnswerFact("0,60", "frutos por brote · sin aclarear", "objetivo 0,40", FactTone.PRIMARY),
            new AnswerFact("+50 %", "sobre el objetivo", "sin aclareo", FactTone.HIGHLIGHT));

    /** A model that answers what it is told and remembers what it got. */
    private static final class FakeWriter implements AnswerTextWriter {
        private final boolean enabled;
        private final Optional<Written> answer;
        private final List<Request> requests = new ArrayList<>();

        FakeWriter(boolean enabled, Optional<Written> answer) {
            this.enabled = enabled;
            this.answer = answer;
        }

        @Override public boolean enabled() { return enabled; }
        @Override public String provider() { return "GROQ"; }
        @Override public String model() { return "openai/gpt-oss-120b"; }
        @Override public Optional<Written> rewrite(Request request) {
            requests.add(request);
            return answer;
        }
    }

    private static AnswerTextChooser.Chosen choose(FakeWriter writer) {
        return new AnswerTextChooser(writer).choose(QuestionId.PLAN_IF_NONE, "¿Y si no aclaro?",
                AnswerLanguage.SPANISH, TEMPLATE, FACTS);
    }

    @Test
    @DisplayName("without a model the template is the answer, by design and not as a fallback")
    void withoutAModel() {
        var writer = new FakeWriter(false, Optional.empty());

        var chosen = choose(writer);

        assertThat(chosen).isEqualTo(new AnswerTextChooser.Chosen(TEMPLATE, AssistantAnswer.TEMPLATE, null, false,
                null, null));
        assertThat(writer.requests).isEmpty();
    }

    @Test
    @DisplayName("a valid rephrasing is kept with its model and tokens, trimmed")
    void keepsAValidText() {
        var text = "Si no aclareas, tu árbol queda en 0,60 frutos por brote, un 50 % más de lo que sostiene (0,40).";
        var writer = new FakeWriter(true, Optional.of(new AnswerTextWriter.Written("  " + text + "\n", 180, 45)));

        var chosen = choose(writer);

        assertThat(chosen).isEqualTo(new AnswerTextChooser.Chosen(text, "GROQ", "openai/gpt-oss-120b", false, 180, 45));
        assertThat(writer.requests).singleElement().satisfies(request -> {
            assertThat(request.question()).isEqualTo("¿Y si no aclaro?");
            assertThat(request.template()).isEqualTo(TEMPLATE);
            assertThat(request.facts()).isEqualTo(FACTS);
        });
    }

    @Test
    @DisplayName("a model that does not answer leaves the template as a fallback")
    void modelDown() {
        var chosen = choose(new FakeWriter(true, Optional.empty()));

        assertThat(chosen).isEqualTo(new AnswerTextChooser.Chosen(TEMPLATE, "GROQ", "openai/gpt-oss-120b", true,
                null, null));
    }

    @Test
    @DisplayName("an invented figure leaves the template as a fallback, keeping the tokens spent")
    void inventedFigure() {
        var writer = new FakeWriter(true, Optional.of(new AnswerTextWriter.Written(
                "Si no aclareas, el año siguiente cosecharás un 40 % menos.", 180, 30)));

        var chosen = choose(writer);

        assertThat(chosen).isEqualTo(new AnswerTextChooser.Chosen(TEMPLATE, "GROQ", "openai/gpt-oss-120b", true,
                180, 30));
    }
}
