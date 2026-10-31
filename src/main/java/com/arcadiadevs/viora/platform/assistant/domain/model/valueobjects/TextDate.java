package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.time.LocalDate;

/**
 * A date of an answer written the way its language reads it: the long form in the sentences and sources ("29 de
 * octubre", "October 29") and the short one on the cards ("29 oct", "Oct 29"). A plain {@link LocalDate} keeps the
 * single short form of the first screens.
 *
 * @param date      the date
 * @param shortForm whether to use the short form of the cards
 */
public record TextDate(LocalDate date, boolean shortForm) {

    public TextDate {
        if (date == null) {
            throw new IllegalArgumentException("assistant.text.date.null");
        }
    }

    /**
     * Long form, for the sentences and sources.
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
