package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.util.Locale;

/**
 * Screen of the app the assistant is opened from. Each screen has its own closed set of questions.
 */
public enum ScreenKey {
    /** Home of the producer (P110-P113): about one plot or, without plot, the producer's whole field. */
    HOME,

    /** Alternate bearing of a plot (P40, P114-P119). */
    ALTERNATION,

    /** Thinning plan of a plot (P61, P120-P122). */
    PLAN,

    /** Agroclimatic alert of a plot (T15, P123-P125). */
    ALERT,

    /** Climate of a plot: forecast and sensors (P90, P130-P132). */
    CLIMATE,

    /** Winter chill of a plot: Erez chill portions of the season (P80, P133-P135). */
    WINTER_CHILL,

    /** Detail of a plot: its state, today's tasks, the last sampling and the harvest estimate (P26, P136-P138). */
    PLOT_DETAIL,

    /** Nodes of a plot: what they read, how reliable it is and how to link one (P85, P139-P141). */
    SENSORS,

    /** Summary of the sampling of a plot: enough trees, what it means, what to do next (P54, P142-P144). */
    SAMPLING_SUMMARY,

    /** After the thinning of a plot is recorded: the harvest, the target, the next sampling (P63, P145-P147). */
    THINNING_DONE;

    /**
     * Whether the questions of the screen are about one plot, so the request must name it. HOME has field scope:
     * without a plot its questions are about every active plot of the producer.
     *
     * @return true for plot screens, false for HOME
     */
    public boolean requiresPlot() {
        return this != HOME;
    }

    /**
     * Whether the questions of the screen answer from Telemetry (alerts, forecast, nodes) instead of the harvest and
     * thinning data.
     *
     * @return true for ALERT, CLIMATE and SENSORS
     */
    public boolean readsClimate() {
        return this == ALERT || this == CLIMATE || this == SENSORS;
    }

    /**
     * Whether the questions of the screen are about one plot read as HOME reads it: its thinning plan, sampling,
     * alerts, harvests and forecast.
     *
     * @return true for PLOT_DETAIL, SAMPLING_SUMMARY and THINNING_DONE
     */
    public boolean readsOnePlot() {
        return this == PLOT_DETAIL || this == SAMPLING_SUMMARY || this == THINNING_DONE;
    }

    /**
     * Whether the questions of the screen answer from the winter chill season of the plot (Phenology).
     *
     * @return true for WINTER_CHILL
     */
    public boolean readsChill() {
        return this == WINTER_CHILL;
    }

    /**
     * Prefix shared by the identifiers of the screen questions, such as {@code plan.}. WINTER_CHILL and PLOT_DETAIL
     * SAMPLING_SUMMARY and THINNING_DONE questions use the shorter {@code chill.}, {@code plot.}, {@code sampling.} and
     * {@code thinning.} of the contract.
     *
     * @return the prefix
     */
    public String questionPrefix() {
        return switch (this) {
            case WINTER_CHILL -> "chill.";
            case PLOT_DETAIL -> "plot.";
            case SAMPLING_SUMMARY -> "sampling.";
            case THINNING_DONE -> "thinning.";
            default -> name().toLowerCase(Locale.ROOT) + ".";
        };
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
