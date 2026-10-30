package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * A date shown in a text of an answer, formatted with the language of the request ("29 de octubre", "October 29").
 *
 * @param date      the date
 * @param shortForm whether to use the short form of the cards ("29 oct")
 */
public record TextDate(LocalDate date, boolean shortForm) {

    public TextDate {
        if (date == null) {
            throw new IllegalArgumentException("assistant.text.date.null");
        }
    }

    /**
     * Long form, for the texts and sources.
     *
     * @param date the date
     * @return the text date
     */
    public static TextDate of(LocalDate date) {
        return new TextDate(date, false);
    }

    /**
     * Short form, for the cards.
     *
     * @param date the date
     * @return the text date
     */
    public static TextDate shortOf(LocalDate date) {
        return new TextDate(date, true);
    }
}
