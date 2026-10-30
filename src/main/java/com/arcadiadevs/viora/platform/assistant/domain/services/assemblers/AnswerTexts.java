package com.arcadiadevs.viora.platform.assistant.domain.services.assemblers;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.CatalogText;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ClimateData;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactCardTemplate;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.FactTone;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.QuestionId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextDate;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Message keys and rounding shared by the assemblers. Figures are rounded here, once, so the facts hold exactly
 * the number the text shows.
 */
final class AnswerTexts {

    static final String PREFIX = "assistant.template.";

    private AnswerTexts() {
    }

    /** Text of a question, for example {@code assistant.template.plan.if_less.text.minimum}. */
    static CatalogText text(QuestionId questionId, String variant, Object... args) {
        return CatalogText.of(PREFIX + questionId.value() + "." + variant, args);
    }

    /** Shared text such as a card label, a value format or a source. */
    static CatalogText shared(String key, Object... args) {
        return CatalogText.of(PREFIX + key, args);
    }

    /** "Pronóstico de Open-Meteo, actualizado el 30 de octubre". */
    static CatalogText forecastSource(ClimateData.Forecast forecast) {
        return shared("source.forecast", TextDate.of(forecast.syncedOn()));
    }

    static FactCardTemplate card(CatalogText value, CatalogText label, CatalogText pill, FactTone tone) {
        return new FactCardTemplate(value, label, pill, tone);
    }

    /** Two decimals, half up, as loads and indexes are shown. */
    static double twoDecimals(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /** One decimal, half up, as temperatures and moisture are shown. */
    static double oneDecimal(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    /** Whole number, half up. */
    static long whole(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP).longValue();
    }
}
