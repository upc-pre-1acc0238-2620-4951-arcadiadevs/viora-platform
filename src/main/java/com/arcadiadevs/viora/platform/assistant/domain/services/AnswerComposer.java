package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerDraft;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.DataVersion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Writes the answer to a closed question from the figures of the plot, with fixed sentences. The server computes
 * every figure; here nothing is estimated or invented, and a question whose figures are missing has no answer.
 *
 * <p>The sentences are the deterministic template of each question, kept by the domain class of its screen. The
 * language model of a later step only rephrases them; these stay as its fallback.</p>
 */
public final class AnswerComposer {

    private static final String DIGEST = "SHA-256";

    private AnswerComposer() {
    }

    /**
     * Fingerprint of the figures the questions of a screen read. It changes exactly when one of them changes, so an
     * answer written from an older fingerprint is stale.
     *
     * @param screen the screen
     * @param facts  the figures of the plot, of that screen
     * @return the version
     * @throws IllegalArgumentException with {@code assistant.screen.invalid} when the figures are of another screen
     */
    public static DataVersion dataVersionOf(ScreenKey screen, ScreenFacts facts) {
        if (facts.screen() != screen) {
            throw new IllegalArgumentException("assistant.screen.invalid");
        }
        try {
            var digest = MessageDigest.getInstance(DIGEST).digest(facts.canonical().getBytes(StandardCharsets.UTF_8));
            return new DataVersion("sha256:" + HexFormat.of().formatHex(digest));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(DIGEST + " is always available", e);
        }
    }

    /**
     * Writes the answer to a question.
     *
     * @param questionId the question
     * @param facts      the figures of the plot
     * @param plotId     the plot, for the routes of the buttons
     * @return the answer, or empty when the figures it needs are missing
     */
    public static Optional<AnswerDraft> compose(QuestionId questionId, ScreenFacts facts, PlotId plotId) {
        return facts.answer(questionId, plotId);
    }
}
