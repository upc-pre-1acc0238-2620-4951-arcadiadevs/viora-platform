package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Harvest is weighed when it is delivered, so a weighing date cannot be in the future. Every case uses a fixed
 * clock on purpose: the rule has to follow the clock the application injects, never the wall clock of the machine
 * running the suite.
 */
class WeighingDateTest {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-18T10:00:00Z"), ZoneOffset.UTC);
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    @Test
    void acceptsTheCurrentDateOfTheInjectedClock() {
        assertEquals(TODAY, WeighingDate.of(TODAY, CLOCK).value());
    }

    @Test
    void acceptsAnEarlierDate() {
        assertEquals(LocalDate.of(2026, 11, 17), WeighingDate.of(LocalDate.of(2026, 11, 17), CLOCK).value());
        assertEquals(LocalDate.of(2026, 1, 15), WeighingDate.of(LocalDate.of(2026, 1, 15), CLOCK).value());
    }

    @Test
    void rejectsTheDayAfterTheCurrentDateOfTheInjectedClock() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> WeighingDate.of(LocalDate.of(2026, 11, 19), CLOCK));
        assertEquals("settlement.weighed_on.future", error.getMessage());
    }

    @Test
    void judgesTheSameDateAgainstWhicheverClockItIsGiven() {
        var yesterday = Clock.fixed(Instant.parse("2026-11-17T23:59:59Z"), ZoneOffset.UTC);
        assertEquals("settlement.weighed_on.future",
                assertThrows(IllegalArgumentException.class, () -> WeighingDate.of(TODAY, yesterday)).getMessage());
        assertEquals(TODAY, WeighingDate.of(TODAY, CLOCK).value());
    }

    @Test
    void rejectsAMissingDateOrClock() {
        assertEquals("settlement.weighed_on.null",
                assertThrows(IllegalArgumentException.class, () -> WeighingDate.of(null, CLOCK)).getMessage());
        assertEquals("settlement.weighed_on.null",
                assertThrows(IllegalArgumentException.class, () -> WeighingDate.of(TODAY, null)).getMessage());
        assertEquals("settlement.weighed_on.null",
                assertThrows(IllegalArgumentException.class, () -> new WeighingDate(null)).getMessage());
    }

    @Test
    void readsBackAStoredDateWithoutJudgingIt() {
        // A settlement settled before this rule existed, or a clock that moved backwards, must still reload.
        assertEquals(LocalDate.of(2030, 12, 31), new WeighingDate(LocalDate.of(2030, 12, 31)).value());
    }
}
