package com.arcadiadevs.viora.platform.assistant.infrastructure.ai;

import com.arcadiadevs.viora.platform.assistant.application.internal.AnswerLocalizer;
import com.arcadiadevs.viora.platform.assistant.application.internal.outboundservices.ai.AnswerTextWriter;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerComposer;
import com.arcadiadevs.viora.platform.assistant.domain.services.AnswerTextValidator;
import com.arcadiadevs.viora.platform.assistant.domain.services.AssistantScreenSamples;
import com.arcadiadevs.viora.platform.assistant.domain.services.DailySummaryTexts;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asks the real model to rephrase every enabled question of the eleven screens and the daily summary, in Spanish and
 * in English, with fixed figures, and reports what the validator kept, how long it took and the tokens it spent. The
 * report is printed and written to {@code target/assistant-live-evaluation.txt}, so a person reads the texts: the
 * validator alone misses a lost nuance.
 *
 * <p>It never runs with the suite: it needs {@code -Dviora.assistant.live=true} and GROQ_API_KEY in the environment or
 * in the local {@code .env}. Narrow it with {@code -Dviora.assistant.live.screens=CLIMATE,ALERT} and
 * {@code -Dviora.assistant.live.languages=es}; compare models with {@code -Dspring.ai.openai.chat.model=...}. The
 * calls are spaced to stay inside the free tier (30 requests and about 8K tokens a minute).</p>
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
    private static final Path REPORT = Path.of("target", "assistant-live-evaluation.txt");

    @Autowired AnswerTextWriter writer;
    @Autowired AnswerLocalizer localizer;

    /** One text to rephrase: the question as the producer reads it, the template and its cards. */
    private record Case(String name, AnswerLanguage language, String question, String template,
                        List<AnswerFact> facts) {
    }

    @Test
    @DisplayName("rephrases every enabled question and the daily summary, and the validator keeps most of them")
    void evaluate() throws InterruptedException, IOException {
        Assumptions.assumeTrue(writer.enabled(), "GROQ_API_KEY is not set");
        var cases = cases();

        int accepted = 0;
        var report = new StringBuilder("Model: " + writer.provider() + " " + writer.model() + "\n");
        for (var c : cases) {
            long start = System.nanoTime();
            var written = writer.rewrite(new AnswerTextWriter.Request(c.language(), c.question(), c.template(),
                    c.facts()));
            long millis = (System.nanoTime() - start) / 1_000_000;
            var verdict = written.map(w -> AnswerTextValidator.validate(
                    AnswerTextValidator.normalize(w.text()), c.template(), c.facts(), c.language()))
                    .orElse(null);
            if (verdict == AnswerTextValidator.Verdict.ACCEPTED) {
                accepted++;
            }
            report.append("\n[").append(c.name()).append(' ').append(c.language().code()).append("] ")
                    .append(verdict == null ? "NO ANSWER" : verdict).append(" · ").append(millis).append(" ms · tokens ")
                    .append(written.map(w -> w.inputTokens() + "/" + w.outputTokens()).orElse("-"))
                    .append("\n  question: ").append(c.question())
                    .append("\n  template: ").append(c.template())
                    .append("\n  model:    ").append(written.map(AnswerTextWriter.Written::text).orElse("-"))
                    .append('\n');
            Thread.sleep(PAUSE_MILLIS);
        }
        report.append("\nAccepted ").append(accepted).append(" of ").append(cases.size()).append('\n');
        System.out.println(report);
        Files.createDirectories(REPORT.getParent());
        Files.writeString(REPORT, report, StandardCharsets.UTF_8);

        assertThat(accepted).as("answers the validator kept").isGreaterThan(0);
    }

    /** Every enabled question of the chosen screens, then the daily summary, in each chosen language. */
    private List<Case> cases() {
        var screens = System.getProperty("viora.assistant.live.screens", "");
        var languages = System.getProperty("viora.assistant.live.languages", "es,en").split(",");
        var cases = new ArrayList<Case>();
        for (var code : languages) {
            var language = new AnswerLanguage(code.trim());
            for (ScreenFacts facts : AssistantScreenSamples.everyScreen()) {
                if (!screens.isBlank() && !screens.contains(facts.screen().name())) {
                    continue;
                }
                for (var question : facts.questions()) {
                    // Steps of the app are never rephrased in production, so the model is not asked here either
                    if (!question.enabled() || question.questionId().writtenAsIs()) {
                        continue;
                    }
                    var draft = AnswerComposer.compose(question.questionId(), facts, PLOT).orElseThrow(
                            () -> new IllegalStateException("Enabled without answer: " + question.questionId()));
                    var template = localizer.localize(draft, language);
                    cases.add(new Case(question.questionId().value(), language,
                            localizer.resolve(question.label(), language.locale()), template.text(),
                            template.facts()));
                }
            }
            if (screens.isBlank() || screens.contains("DAILY_SUMMARY")) {
                var summary = DailySummaryTexts.summarize(AssistantScreenSamples.home()).orElseThrow();
                var template = localizer.localize(summary, language);
                cases.add(new Case("home.daily_summary", language, localizer.resolve(
                        CatalogText.of("assistant.question.home.daily_summary.label"), language.locale()),
                        template.text(), template.facts()));
            }
        }
        return cases;
    }
}
