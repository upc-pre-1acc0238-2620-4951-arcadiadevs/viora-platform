package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogQuestion;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * The message keys and the arithmetic every screen writes its questions and answers with, so each screen rounds
 * and names its texts the same way.
 */
final class ScreenTexts {

    private static final String QUESTION_KEY = "assistant.question.";
    private static final String ANSWER_KEY = "assistant.answer.";
    private static final String BAND_KEY = "assistant.band.";

    static final String THINNING_PLAN_ROUTE = "thinning/plan?plotId=";
    static final String THINNING_EXECUTE_ROUTE = "thinning/execute?plotId=";
    static final String SAMPLING_ROUTE = "thinning/sampling?plotId=";

    private ScreenTexts() {
    }

    /** A label variant of a catalog question. */
    static CatalogText label(QuestionId id, String variant, Object... args) {
        return CatalogText.of(QUESTION_KEY + id.value() + "." + variant, args);
    }

    /** A catalog question, enabled exactly when there is no reason against it. */
    static CatalogQuestion question(QuestionId id, CatalogText label, String icon, CatalogText reason) {
        return new CatalogQuestion(id, label, icon, reason == null, reason);
    }

    /** A part of the template of an answer. */
    static CatalogText text(String question, String part, Object... args) {
        return CatalogText.of(ANSWER_KEY + question + "." + part, args);
    }

    /** A text several answers share, such as a card label, a value format or a source. */
    static CatalogText shared(String key, Object... args) {
        return CatalogText.of(ANSWER_KEY + key, args);
    }

    /** "Pronóstico de Open-Meteo, actualizado el 30 de octubre". */
    static CatalogText forecastSource(ClimateData.Forecast forecast) {
        return shared("source.forecast", TextDate.of(forecast.syncedOn()));
    }

    /** By how many whole percent {@code value} is above (positive) or below (negative) {@code reference}. */
    static int percentOf(double value, double reference) {
        return (int) Math.round((value / reference - 1.0) * 100.0);
    }

    /** Rounds half up, so the figure of the text and of the card are the same number. */
    static double round(double value, int decimals) {
        return BigDecimal.valueOf(value).setScale(decimals, RoundingMode.HALF_UP).doubleValue();
    }

    /** Rounds half up to a whole number, as percentages, kilograms and calibers are shown. */
    static long whole(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    /** "producción regular", "vecería moderada", "vecería severa": the band of a bearing index category. */
    static Optional<CatalogText> band(String category) {
        if (category == null) {
            return Optional.empty();
        }
        return switch (category) {
            case "REGULAR" -> Optional.of(CatalogText.of(BAND_KEY + "regular"));
            case "MODERATE_ALTERNATION" -> Optional.of(CatalogText.of(BAND_KEY + "moderate"));
            case "SEVERE_ALTERNATION" -> Optional.of(CatalogText.of(BAND_KEY + "severe"));
            default -> Optional.empty();
        };
    }
}
