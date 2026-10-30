package com.arcadiadevs.viora.platform.assistant.infrastructure.metrics;

import com.arcadiadevs.viora.platform.assistant.application.internal.answers.AnswerMetrics;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextRejection;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Locale;

/**
 * Micrometer meters of the assistant. Tags only carry the question and the outcome, never producer or plot ids.
 *
 * <ul>
 *     <li>{@value #ANSWERS}: counter tagged {@code questionId} and {@code outcome} (created, cache, replay,
 *     unavailable, rate_limited).</li>
 *     <li>{@value #MODEL}: timer of each model attempt tagged {@code outcome} (accepted, rejected, error,
 *     breaker_open) and {@code reason} (the {@link TextRejection}, or {@code none}).</li>
 *     <li>{@value #TOKENS}: distribution summary tagged {@code type} (input, output) with the tokens the model
 *     reports.</li>
 * </ul>
 *
 * <p>The application has no {@link MeterRegistry} bean today (no actuator), so the meters go to the global
 * registry of Micrometer, which drops them until a registry is added; Spring Boot adds its registry to the global
 * one, so exposing them later only needs the ops decision (actuator, Prometheus), not a change here.</p>
 */
@Component
public class AssistantMetrics implements AnswerMetrics {

    /** Name of the questions counter. */
    public static final String ANSWERS = "viora.assistant.answers";
    /** Name of the model attempts timer. */
    public static final String MODEL = "viora.assistant.model";
    /** Name of the token summaries. */
    public static final String TOKENS = "viora.assistant.tokens";

    /** Outcome of a model attempt. */
    public enum ModelOutcome {
        /** The text of the model is the answer. */
        ACCEPTED,
        /** The validator rejected the text. */
        REJECTED,
        /** The call failed, took too long, returned malformed output, or the validation failed. */
        ERROR,
        /** The circuit breaker is open: no call was made. */
        BREAKER_OPEN
    }

    private final MeterRegistry registry;

    /**
     * Creates the meters on the registry of the application, or on the global one when there is none.
     *
     * @param registry the registry of the application, if any
     */
    @Autowired
    public AssistantMetrics(ObjectProvider<MeterRegistry> registry) {
        this(registry.getIfAvailable(() -> Metrics.globalRegistry));
    }

    /**
     * Creates the meters on a registry.
     *
     * @param registry the registry
     */
    public AssistantMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void answer(QuestionId questionId, Outcome outcome) {
        Counter.builder(ANSWERS)
                .tag("questionId", questionId.value())
                .tag("outcome", tagOf(outcome))
                .register(registry)
                .increment();
    }

    /**
     * Records one model attempt and the tokens it used.
     *
     * @param outcome      how it ended
     * @param reason       why its text was not used; null when it was
     * @param duration     how long it took
     * @param inputTokens  tokens sent; null when unknown
     * @param outputTokens tokens received; null when unknown
     */
    public void modelAttempt(ModelOutcome outcome, TextRejection reason, Duration duration, Integer inputTokens,
            Integer outputTokens) {
        Timer.builder(MODEL)
                .tag("outcome", tagOf(outcome))
                .tag("reason", reason == null ? "none" : tagOf(reason))
                .register(registry)
                .record(duration);
        tokens("input", inputTokens);
        tokens("output", outputTokens);
    }

    private void tokens(String type, Integer count) {
        if (count != null) {
            DistributionSummary.builder(TOKENS).tag("type", type).baseUnit("tokens").register(registry).record(count);
        }
    }

    private static String tagOf(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }
}
