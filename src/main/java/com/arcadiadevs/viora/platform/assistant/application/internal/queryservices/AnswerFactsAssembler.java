package com.arcadiadevs.viora.platform.assistant.application.internal.queryservices;

import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenFacts;
import com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects.ScreenKey;
import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Year;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Reads, through the reader of each screen, the figures its catalog and its answers are evaluated against. It reads
 * only what the other contexts already computed and only what the screen needs, so asking a question never
 * recalculates anything. A plot that no longer exists simply has no figures.
 */
@Service
public class AnswerFactsAssembler {

    private final Map<ScreenKey, ScreenFactsReader> readers;
    private final Clock clock;

    /**
     * Constructs the assembler.
     *
     * @param readers one reader per screen
     * @param clock   the application clock, for the current campaign
     * @throws IllegalStateException when a screen has no reader or more than one
     */
    public AnswerFactsAssembler(List<ScreenFactsReader> readers, Clock clock) {
        var byScreen = new EnumMap<ScreenKey, ScreenFactsReader>(ScreenKey.class);
        for (var reader : readers) {
            if (byScreen.putIfAbsent(reader.screen(), reader) != null) {
                throw new IllegalStateException("Two readers for the screen " + reader.screen());
            }
        }
        for (var screen : ScreenKey.values()) {
            if (!byScreen.containsKey(screen)) {
                throw new IllegalStateException("No reader for the screen " + screen);
            }
        }
        this.readers = byScreen;
        this.clock = clock;
    }

    /**
     * Reads the figures the questions of a screen need.
     *
     * @param screen the screen
     * @param plotId the plot
     * @return the figures of that screen
     */
    public ScreenFacts assemble(ScreenKey screen, PlotId plotId) {
        int campaignYear = Year.now(clock.withZone(ProducerTimeZone.ZONE)).getValue();
        return readers.get(screen).read(plotId, campaignYear);
    }
}
