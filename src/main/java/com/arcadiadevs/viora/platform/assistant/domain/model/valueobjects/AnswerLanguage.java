package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.Locale;

/**
 * Language an answer is written in. The API supports Spanish and English; anything else is answered in Spanish,
 * like the app does. Regions are dropped on purpose: a Peruvian locale would write decimals with a point, and the
 * design writes them with a comma.
 *
 * @param code {@code es} or {@code en}
 */
public record AnswerLanguage(String code) {

    /** Spanish, the field language. */
    public static final AnswerLanguage SPANISH = new AnswerLanguage("es");

    /**
     * Compact constructor validating the code.
     */
    public AnswerLanguage {
        if (!"es".equals(code) && !"en".equals(code)) {
            throw new IllegalArgumentException("assistant.language.invalid");
        }
    }

    /**
     * Picks the supported language of a request locale.
     *
     * @param locale the locale of the request; may be null
     * @return English for an English locale, Spanish otherwise
     */
    public static AnswerLanguage of(Locale locale) {
        return locale != null && "en".equals(locale.getLanguage()) ? new AnswerLanguage("en") : SPANISH;
    }

    /**
     * The locale to format the answer with.
     *
     * @return a locale without region
     */
    public Locale locale() {
        return Locale.forLanguageTag(code);
    }
}
