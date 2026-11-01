package com.arcadiadevs.viora.platform.assistant.domain.services;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerFact;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.AnswerLanguage;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Decides whether a text the language model wrote may replace the template of an answer. The model only rephrases:
 * every number it writes must already be in the template or in the cards, and it may not bring up a practice the
 * template does not mention. Anything else keeps the template, so a wrong figure never reaches the producer.
 */
public final class AnswerTextValidator {

    /** Upper bound of the text: the voice of Viora reads 2-3 short sentences. */
    public static final int MAX_CHARACTERS = 360;

    /** Upper bound of sentences. */
    public static final int MAX_SENTENCES = 3;

    /** A number as the answers write it: digits with an optional decimal part after a comma or a point. */
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)?");
    private static final Pattern SENTENCE_END = Pattern.compile("[.!?](?:\\s|$)");
    /** A sign glued to a number, as the cards write it ("−30 %", "+50 %"); sentences say "quitar 30 %". */
    private static final Pattern SIGNED_NUMBER = Pattern.compile("(?:^|[\\s(])[+−-]\\d");
    private static final Pattern MARKUP = Pattern.compile("[*#`_\\[\\]<>]|https?:|\\R");

    /**
     * Practices the assistant must not recommend on its own: irrigation, inputs and promises. Stems without accents,
     * matched against the text without accents.
     */
    private static final List<String> FORBIDDEN_STEMS = List.of(
            "riego", "regar", "riega", "riegue", "irrigat", "water the", "fertiliz", "abona", "abono", "dosis", "dose",
            "pesticid", "fungicid", "insecticid", "herbicid", "fumig", "spray", "garantiz", "guarantee");

    /** Short words that only one of the two languages uses, to tell a text written in the other language. */
    /**
     * Stems of judgments the model must not add: a template states figures, and a rating such as "good yield" can
     * read the other way round (an overload is not a good yield).
     */
    private static final List<String> JUDGMENT_STEMS = List.of(
            "buen", "mal ", "mala ", "malo", "excelent", "significativ", "notabl", "positiv", "negativ",
            "preocup", "favorabl", "desfavorabl", "mejora", "empeor", "saludabl", "excepcional", "optim",
            "good", "great", "excellent", "significant", "improv", "positive", "negative", "worr",
            "healthy", "strong", "poor", "remarkabl", "outstanding", "impressive", "optimal");

    /** "Aclarar" means to clarify: thinning fruit is "aclarear". */
    private static final Pattern WRONG_THINNING_VERB =
            Pattern.compile("\\baclar(?:a|as|o|ar|e|en|an|amos|ando|ado|ados)\\b");

    /** A verb in the conditional (llegaría, podría, sería; would, could, might): an estimate, not a fact. */
    private static final Pattern SPANISH_CONDITIONAL = Pattern.compile("\\b\\p{L}+(?:ar|er|ir|dr|br)ian?\\b");
    private static final Pattern ENGLISH_CONDITIONAL = Pattern.compile("\\b(?:would|could|might)\\b");

    private static final List<String> SPANISH_WORDS = List.of("el", "la", "los", "las", "de", "del", "que", "tu", "es",
            "con", "por", "y", "en", "un", "una");
    private static final List<String> ENGLISH_WORDS = List.of("the", "and", "your", "is", "of", "to", "with", "you",
            "it", "in", "at", "for");
    private static final Pattern SPANISH_LETTERS = Pattern.compile("[áéíóúñ¿¡]", Pattern.CASE_INSENSITIVE);

    /**
     * The caution of a template: "tends to", "can", "is expected". A text that drops all of it turns a tendency
     * into a promise, which the assistant must never make. Stems without accents.
     */
    private static final List<String> SPANISH_HEDGES = List.of("tiende", "puede", "se espera", "suele", "podria",
            "probable");
    private static final List<String> ENGLISH_HEDGES = List.of("tend", "can ", "may ", "expected", "likely", "could");

    /** Why a text was accepted or kept out. */
    public enum Verdict {
        /** The text may replace the template. */
        ACCEPTED,
        /** The model wrote nothing. */
        EMPTY,
        /** Longer than {@link #MAX_CHARACTERS} or than {@link #MAX_SENTENCES} sentences. */
        TOO_LONG,
        /** Lists, markup, links, line breaks, or a sign glued to a number the template writes without it. */
        FORMAT,
        /** Written, even in part, in the other language. */
        WRONG_LANGUAGE,
        /** A number that is not in the template nor in the cards. */
        UNKNOWN_NUMBER,
        /** A practice the template does not mention. */
        FORBIDDEN_TERM,

        /** A rating ("good", "mejora") the template does not make. */
        ADDED_JUDGMENT,
        /** The template is cautious ("tends to", "can", a conditional) and the text states it as a certainty. */
        LOST_HEDGE
    }

    private AnswerTextValidator() {
    }

    /**
     * The text as an answer keeps it. Models like to put thin and no-break spaces before units and percent signs;
     * the templates use plain spaces, so the text uses them too.
     *
     * @param text the text of the model; may be null
     * @return the text with plain spaces and without surrounding blanks, or null
     */
    public static String normalize(String text) {
        if (text == null) {
            return null;
        }
        return text.replace('\u202F', ' ').replace('\u00A0', ' ').replace('\u2009', ' ').strip();
    }

    /**
     * Checks a text against the template it rephrases.
     *
     * @param candidate the text of the model
     * @param template  the text of the template, in the same language
     * @param facts     the two cards of the answer
     * @param language  the language the answer must be written in
     * @return {@link Verdict#ACCEPTED} or the first reason to keep the template
     */
    public static Verdict validate(String candidate, String template, List<AnswerFact> facts,
                                   AnswerLanguage language) {
        if (candidate == null || candidate.isBlank()) {
            return Verdict.EMPTY;
        }
        var text = candidate.strip();
        if (text.length() > MAX_CHARACTERS || sentences(text) > MAX_SENTENCES) {
            return Verdict.TOO_LONG;
        }
        if (MARKUP.matcher(text).find()
                || (SIGNED_NUMBER.matcher(text).find() && !SIGNED_NUMBER.matcher(template).find())) {
            return Verdict.FORMAT;
        }
        if (!isWrittenIn(text, language)) {
            return Verdict.WRONG_LANGUAGE;
        }
        var allowed = new HashSet<BigDecimal>(numbers(template));
        for (var fact : facts) {
            allowed.addAll(numbers(fact.value()));
            allowed.addAll(numbers(fact.label()));
            allowed.addAll(numbers(fact.pill()));
        }
        if (!allowed.containsAll(numbers(text))) {
            return Verdict.UNKNOWN_NUMBER;
        }
        var plainText = plain(text);
        var plainTemplate = plain(template);
        for (var stem : FORBIDDEN_STEMS) {
            if (plainText.contains(stem) && !plainTemplate.contains(stem)) {
                return Verdict.FORBIDDEN_TERM;
            }
        }
        if (WRONG_THINNING_VERB.matcher(plainText).find() && !WRONG_THINNING_VERB.matcher(plainTemplate).find()) {
            return Verdict.FORBIDDEN_TERM;
        }
        for (var stem : JUDGMENT_STEMS) {
            if (plainText.contains(stem) && !plainTemplate.contains(stem)) {
                return Verdict.ADDED_JUDGMENT;
            }
        }
        var conditional = "en".equals(language.code()) ? ENGLISH_CONDITIONAL : SPANISH_CONDITIONAL;
        if (conditional.matcher(plainTemplate).find() && !conditional.matcher(plainText).find()) {
            return Verdict.LOST_HEDGE;
        }
        var hedges = "en".equals(language.code()) ? ENGLISH_HEDGES : SPANISH_HEDGES;
        if (hedges.stream().anyMatch(plainTemplate::contains) && hedges.stream().noneMatch(plainText::contains)) {
            return Verdict.LOST_HEDGE;
        }
        return Verdict.ACCEPTED;
    }

    /**
     * Whether a text reads as the language: more of its own short words than of the other's, and an English text
     * without Spanish letters at all, so a stray "tú" is caught too.
     */
    private static boolean isWrittenIn(String text, AnswerLanguage language) {
        var words = List.of(text.toLowerCase(Locale.ROOT).split("[^\\p{L}]+"));
        long spanish = words.stream().filter(SPANISH_WORDS::contains).count();
        long english = words.stream().filter(ENGLISH_WORDS::contains).count();
        if ("en".equals(language.code())) {
            return english >= spanish && !SPANISH_LETTERS.matcher(text).find();
        }
        return spanish >= english;
    }

    /**
     * The numbers of a text as values: "0,57", "0.57" and "0.570" are the same number, so Spanish and English
     * writings compare equal. Signs are left out: "−30 %" and "30 %" name the same figure.
     */
    static Set<BigDecimal> numbers(String text) {
        var found = new HashSet<BigDecimal>();
        if (text == null) {
            return found;
        }
        var matcher = NUMBER.matcher(text);
        while (matcher.find()) {
            found.add(new BigDecimal(matcher.group().replace(',', '.')).stripTrailingZeros());
        }
        return found;
    }

    private static int sentences(String text) {
        var matcher = SENTENCE_END.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        // A last sentence without its full stop still counts
        return text.matches(".*[.!?]\\s*$") ? count : count + 1;
    }

    private static String plain(String text) {
        return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
