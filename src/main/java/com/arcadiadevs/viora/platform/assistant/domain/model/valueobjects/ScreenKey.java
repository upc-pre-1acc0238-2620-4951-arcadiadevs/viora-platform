package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.Locale;

/**
 * Screen of the app the assistant is opened from. Each screen has its own closed set of questions.
 */
public enum ScreenKey {

    /** Alternate bearing of a plot (P40, P114-P119). */
    ALTERNATION,

    /** Thinning plan of a plot (P61, P120-P122). */
    PLAN,

    /** Agroclimatic alert of a plot (T15, P123-P125). */
    ALERT,

    /** Climate of a plot: forecast and sensors (P90, P130-P132). */
    CLIMATE,

    /** Winter chill of a plot: Erez chill portions of the season (P80, P133-P135). */
    WINTER_CHILL;

    /**
     * Whether the questions of the screen are about one plot, so the request must name it.
     *
     * @return true for plot screens
     */
    public boolean requiresPlot() {
        return true;
    }

    /**
     * Prefix shared by the identifiers of the screen questions, such as {@code plan.}. WINTER_CHILL questions use
     * the shorter {@code chill.} of the contract.
     *
     * @return the prefix
     */
    public String questionPrefix() {
        return this == WINTER_CHILL ? "chill." : name().toLowerCase(Locale.ROOT) + ".";
    }

    /**
     * Parses the screen sent by the app, ignoring case.
     *
     * @param value the raw screen key
     * @return the screen
     * @throws IllegalArgumentException with {@code assistant.screen.invalid} when missing or unknown
     */
    public static ScreenKey fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("assistant.screen.invalid");
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("assistant.screen.invalid", e);
        }
    }
}
