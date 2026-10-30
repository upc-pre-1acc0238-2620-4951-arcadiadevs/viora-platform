package com.arcadiadevs.viora.platform.assistant.domain.services;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the numbers of an answer text and checks them against its facts: every number a text shows must be a fact.
 * The templates are tested with it, and the model output will be validated with it before it is shown. Signs are
 * ignored ("−30 %" reads 30) and numbers are compared by value, so "0,40" matches the fact 0.4.
 */
public final class TextNumbers {

    /** Numbers inside fact texts such as dates ({@code 2026-11-12}) or grades ({@code 91/100}). */
    private static final Pattern PLAIN_NUMBER = Pattern.compile("\\d+(?:\\.\\d+)?");

    private TextNumbers() {
    }

    /**
     * Extracts the numbers of a text written in a language, with its decimal and grouping separators.
     *
     * @param text   the text
     * @param locale the language of the text
     * @return the numbers in order of appearance, without sign
     */
    public static List<BigDecimal> extract(String text, Locale locale) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        var symbols = DecimalFormatSymbols.getInstance(locale);
        String grouping = Pattern.quote(String.valueOf(symbols.getGroupingSeparator()));
        String decimal = Pattern.quote(String.valueOf(symbols.getDecimalSeparator()));
        var pattern = Pattern.compile("\\d{1,3}(?:" + grouping + "\\d{3})+(?:" + decimal + "\\d+)?"
                + "|\\d+(?:" + decimal + "\\d+)?");
        var numbers = new ArrayList<BigDecimal>();
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            String token = matcher.group()
                    .replace(String.valueOf(symbols.getGroupingSeparator()), "")
                    .replace(symbols.getDecimalSeparator(), '.');
            numbers.add(new BigDecimal(token));
        }
        return List.copyOf(numbers);
    }

    /**
     * Collects the numbers of a set of facts: the numeric values and the numbers inside the text values.
     *
     * @param facts the facts by name
     * @return the numbers, without sign
     */
    public static List<BigDecimal> factNumbers(Map<String, ?> facts) {
        var numbers = new ArrayList<BigDecimal>();
        if (facts == null) {
            return numbers;
        }
        facts.values().forEach(value -> {
            switch (value) {
                case null -> { }
                case Boolean ignored -> { }
                case Number number -> numbers.add(new BigDecimal(number.toString()).abs());
                default -> {
                    Matcher matcher = PLAIN_NUMBER.matcher(value.toString());
                    while (matcher.find()) {
                        numbers.add(new BigDecimal(matcher.group()));
                    }
                }
            }
        });
        return numbers;
    }

    /**
     * Finds the numbers of a text that are not among its facts.
     *
     * @param text   the text
     * @param locale the language of the text
     * @param facts  the facts the text was written from
     * @return the numbers without a matching fact; empty when the text is fully backed
     */
    public static List<BigDecimal> unbackedNumbers(String text, Locale locale, Map<String, ?> facts) {
        var known = factNumbers(facts);
        return extract(text, locale).stream()
                .filter(number -> known.stream().noneMatch(fact -> fact.compareTo(number) == 0))
                .toList();
    }
}
