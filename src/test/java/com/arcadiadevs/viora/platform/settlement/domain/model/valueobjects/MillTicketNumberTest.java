package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MillTicketNumberTest {

    @Test
    void trimsBeforeCheckingTheLength() {
        var ticket = MillTicketNumber.of("  " + "T".repeat(30) + "  ");

        assertTrue(ticket.isPresent());
        assertEquals("T".repeat(30), ticket.value());
    }

    @Test
    void rejectsATicketThatIsStillTooLongOnceTrimmed() {
        var exception = assertThrows(IllegalArgumentException.class,
                () -> MillTicketNumber.of(" " + "T".repeat(31) + " "));

        assertEquals("settlement.mill_ticket.too_long", exception.getMessage());
    }

    @Test
    void aBlankOrMissingTicketIsAbsent() {
        assertFalse(MillTicketNumber.of(null).isPresent());
        assertFalse(MillTicketNumber.of("   ").isPresent());
    }
}
