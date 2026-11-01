package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.ai.AnswerTextWriter;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AlternationFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlanFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerComposer;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;
import java.util.TreeMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asks the real model to rephrase the eight answers with fixed figures, and reports what the validator kept, how long
 * it took and the tokens it spent. It never runs with the suite: it needs {@code -Dviora.assistant.live=true} and
 * GROQ_API_KEY in the environment or in the local {@code .env}. To compare models add
 * {@code -Dspring.ai.openai.chat.model=openai/gpt-oss-20b}. The calls are spaced to stay inside the free tier.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:assistantlive;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "viora.assistant.model.enabled=true"
})
@EnabledIfSystemProperty(named = "viora.assistant.live", matches = "true")
@DisplayName("Assistant model · live evaluation against the provider")
class AssistantModelLiveEvaluationTest {

    private static final long PAUSE_MILLIS = 6_000;
    private static final PlotId PLOT = new PlotId(UUID.randomUUID().toString());

    @Autowired AnswerTextWriter writer;
    @Autowired AnswerLocalizer localizer;

    private record Case(QuestionId question, String language) {
    }

    @Test
    @DisplayName("rephrases the eight answers and the validator keeps most of them")
    void evaluate() throws InterruptedException {
        Assumptions.assumeTrue(writer.enabled(), "GROQ_API_KEY is not set");
        var cases = List.of(
                new Case(QuestionId.ALTERNATION_WHY_INDEX, "es"), new Case(QuestionId.ALTERNATION_BREAK_CYCLE, "es"),
                new Case(QuestionId.ALTERNATION_VS_CAMPAIGNS, "es"), new Case(QuestionId.ALTERNATION_NEXT_YEAR, "es"),
                new Case(QuestionId.PLAN_WHY_PERCENT, "es"), new Case(QuestionId.PLAN_IF_LESS, "es"),
                new Case(QuestionId.PLAN_IF_NONE, "es"), new Case(QuestionId.PLAN_BEST_TIME, "es"),
                new Case(QuestionId.ALTERNATION_WHY_INDEX, "en"), new Case(QuestionId.PLAN_WHY_PERCENT, "en"));

        int accepted = 0;
        var report = new StringBuilder("\nModel: " + writer.provider() + " " + writer.model() + "\n");
        for (var c : cases) {
            var language = new AnswerLanguage(c.language());
            var facts = c.question().screen() == ScreenKey.ALTERNATION ? alternationFacts() : planFacts();
            var draft = AnswerComposer.compose(c.question(), facts, PLOT).orElseThrow();
            var template = localizer.localize(draft, language);
            var question = localizer.resolve(label(c.question()), language.locale());

            long start = System.nanoTime();
            var written = writer.rewrite(new AnswerTextWriter.Request(language, question, template.text(),
                    template.facts()));
            long millis = (System.nanoTime() - start) / 1_000_000;
            var verdict = written.map(w -> AnswerTextValidator.validate(
                    AnswerTextValidator.normalize(w.text()), template.text(), template.facts(), language))
                    .orElse(null);
            if (verdict == AnswerTextValidator.Verdict.ACCEPTED) {
                accepted++;
            }
            report.append("\n[").append(c.question().value()).append(' ').append(c.language()).append("] ")
                    .append(verdict == null ? "NO ANSWER" : verdict).append(" · ").append(millis).append(" ms · tokens ")
                    .append(written.map(w -> w.inputTokens() + "/" + w.outputTokens()).orElse("-"))
                    .append("\n  template: ").append(template.text())
                    .append("\n  model:    ").append(written.map(AnswerTextWriter.Written::text).orElse("-")).append('\n');
            Thread.sleep(PAUSE_MILLIS);
        }
        report.append("\nAccepted ").append(accepted).append(" of ").append(cases.size()).append('\n');
        System.out.println(report);

        assertThat(accepted).as("answers the validator kept").isGreaterThan(0);
    }

    /** The label the catalog shows for the same figures. */
    private static CatalogText label(QuestionId question) {
        var key = "assistant.question." + question.value() + ".label";
        if (question.equals(QuestionId.ALTERNATION_WHY_INDEX)) {
            return CatalogText.of(key, 0.50);
        } else if (question.equals(QuestionId.ALTERNATION_NEXT_YEAR)) {
            return CatalogText.of(key, "2027");
        } else if (question.equals(QuestionId.PLAN_WHY_PERCENT)) {
            return CatalogText.of(key, 30);
        } else if (question.equals(QuestionId.PLAN_IF_LESS)) {
            return CatalogText.of(key, 20);
        }
        return CatalogText.of(key);
    }

    private static AlternationFacts alternationFacts() {
        var yields = new TreeMap<Integer, Double>();
        yields.put(2023, 40_000.0);
        yields.put(2024, 100_000.0);
        yields.put(2025, 50_000.0);
        return new AlternationFacts(2026, 10.0, yields, 0.50, "MODERATE_ALTERNATION", false);
    }

    private static PlanFacts planFacts() {
        return new PlanFacts(2026, 0.60, 0.40, 30.0,
                LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 14), 5, false, null, 0);
    }
}
