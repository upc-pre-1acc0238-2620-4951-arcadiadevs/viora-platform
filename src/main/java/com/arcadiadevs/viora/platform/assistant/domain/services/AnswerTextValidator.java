package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.TextRejection;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Decides whether a text written by a model can be shown instead of the template text. The text must be short (at
 * most {@value #MAX_LENGTH} characters and {@value #MAX_SENTENCES} sentences), every number must be a fact
 * ({@link TextNumbers}) and it must not give advice the facts do not allow: irrigating unless the fact
 * {@value #IRRIGATION_ADVISABLE} is true, or recording the thinning unless that action is among
 * {@value #ALLOWED_ACTIONS}. When irrigation may be mentioned but the fact {@value #PRODUCER_DECIDES_IRRIGATION}
 * is true, an order to irrigate ("riega", "debes regar", "you should irrigate") is still rejected. The phrase checks
 * are simple word patterns in Spanish and English, not a grammar.
 */
public final class AnswerTextValidator {

    /** Longest text the answer shows. */
    public static final int MAX_LENGTH = 280;

    /** Most sentences the answer shows. */
    public static final int MAX_SENTENCES = 3;

    /** Interpreted fact: whether the facts support irrigation advice; missing means no. */
    public static final String IRRIGATION_ADVISABLE = "irrigationAdvisable";

    /**
     * Interpreted fact: the producer decides whether to irrigate, so even an advisable irrigation may only be
     * described, never ordered.
     */
    public static final String PRODUCER_DECIDES_IRRIGATION = "producerDecidesIrrigation";

    /** Interpreted fact: labels of the actions the answer offers. */
    public static final String ALLOWED_ACTIONS = "allowedActions";

    private static final Pattern SENTENCE_END = Pattern.compile("(?<=[.!?…])\\s+");

    private static final Pattern IRRIGATION = Pattern.compile(
            "\\b(?:riegu?e?[sn]?|riega[sn]?|regar\\w*|riegos?|regad[ií]o)\\b"
                    + "|\\birrig\\w*|\\bwater(?:ing)?\\s+(?:the|your|it|them|this)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

    /** Orders and obligations to irrigate: imperative forms and "debes/tienes que/hay que regar", "you should ...". */
    private static final Pattern IRRIGATION_ORDER = Pattern.compile(
            "\\b(?:riega|riegue[ns]?)\\b"
                    + "|\\b(?:debes|deber[ií]as|tienes\\s+que|hay\\s+que|necesitas)\\s+regar\\b"
                    + "|\\b(?:you\\s+(?:should|must|need\\s+to|have\\s+to)|make\\s+sure\\s+to)\\s+(?:irrigate|water)\\b"
                    + "|(?:^|[.!?:;]\\s*)(?:irrigate|water)\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

    private static final Pattern RECORD_THINNING = Pattern.compile(
            "\\bregistr(?:a|e|en|ar)\\s+(?:el\\s+|tu\\s+)?aclareo"
                    + "|\\b(?:record|register|log)\\s+(?:the\\s+|your\\s+)?thinning",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.UNICODE_CHARACTER_CLASS);

    private AnswerTextValidator() {
    }

    /**
     * Checks a model text against the facts it was written from.
     *
     * @param text   the text
     * @param locale the language of the text
     * @param facts  the facts sent to the model, interpreted facts included
     * @return why the text cannot be shown; empty when it can
     */
    public static Optional<TextRejection> check(String text, Locale locale, Map<String, ?> facts) {
        if (text == null || text.isBlank()) {
            return Optional.of(TextRejection.BLANK);
        }
        var trimmed = text.strip();
        if (trimmed.length() > MAX_LENGTH) {
            return Optional.of(TextRejection.TOO_LONG);
        }
        if (sentences(trimmed) > MAX_SENTENCES) {
            return Optional.of(TextRejection.TOO_MANY_SENTENCES);
        }
        if (!TextNumbers.unbackedNumbers(trimmed, locale, facts).isEmpty()) {
            return Optional.of(TextRejection.UNBACKED_NUMBER);
        }
        if (forbidden(trimmed, facts)) {
            return Optional.of(TextRejection.FORBIDDEN_PHRASE);
        }
        return Optional.empty();
    }

    private static long sentences(String text) {
        return Arrays.stream(SENTENCE_END.split(text)).filter(part -> !part.isBlank()).count();
    }

    private static boolean forbidden(String text, Map<String, ?> facts) {
        var irrigationAllowed = facts != null && Boolean.TRUE.equals(facts.get(IRRIGATION_ADVISABLE));
        if (!irrigationAllowed && IRRIGATION.matcher(text).find()) {
            return true;
        }
        if (facts != null && Boolean.TRUE.equals(facts.get(PRODUCER_DECIDES_IRRIGATION))
                && IRRIGATION_ORDER.matcher(text).find()) {
            return true;
        }
        return RECORD_THINNING.matcher(text).find() && !recordingOffered(facts);
    }

    private static boolean recordingOffered(Map<String, ?> facts) {
        if (facts != null && facts.get(ALLOWED_ACTIONS) instanceof Collection<?> actions) {
            return actions.stream().anyMatch(label -> label != null
                    && RECORD_THINNING.matcher(label.toString()).find());
        }
        return false;
    }
}
