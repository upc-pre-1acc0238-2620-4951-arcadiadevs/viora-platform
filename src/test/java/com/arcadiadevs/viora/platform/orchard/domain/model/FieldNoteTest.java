package com.arcadiadevs.viora.platform.orchard.domain.model;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.FieldNote;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNoteId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNotePhoto;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNoteText;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNoteType;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Field note of the logbook (P75)")
class FieldNoteTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 18);
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};
    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0};

    private final PlotId plotId = new PlotId();
    private final FieldNoteText text = new FieldNoteText("  Revisar goteros de la fila 4.  ");

    private FieldNote note(LocalDate observedOn) {
        return FieldNote.record(new FieldNoteId(UUID.randomUUID().toString()), plotId,
                new ProducerId(UUID.randomUUID().toString()), FieldNoteType.IRRIGATION, text, observedOn, TODAY,
                Instant.parse("2026-11-18T15:00:00Z"));
    }

    @Test
    @DisplayName("Is written for today or an earlier day, never a future one")
    void rejectsFutureDays() {
        assertEquals(TODAY, note(TODAY).snapshot().observedOn());
        assertEquals(TODAY.minusDays(3), note(TODAY.minusDays(3)).snapshot().observedOn());
        assertThrows(IllegalArgumentException.class, () -> note(TODAY.plusDays(1)));
    }

    @Test
    @DisplayName("Trims the text and refuses an empty or too long one")
    void validatesTheText() {
        assertEquals("Revisar goteros de la fila 4.", text.value());
        assertThrows(IllegalArgumentException.class, () -> new FieldNoteText("   "));
        assertThrows(IllegalArgumentException.class, () -> new FieldNoteText("a".repeat(501)));
        assertEquals(500, new FieldNoteText("a".repeat(500)).value().length());
    }

    @Test
    @DisplayName("Reads the type case insensitive and refuses an unknown one")
    void readsTheType() {
        assertEquals(FieldNoteType.PESTS, FieldNoteType.of("pests"));
        assertThrows(IllegalArgumentException.class, () -> FieldNoteType.of("RIEGO"));
    }

    @Test
    @DisplayName("Takes a JPEG or PNG photo detected from its bytes")
    void detectsThePhotoType() {
        var note = note(TODAY);
        assertNull(note.snapshot().photoMediaType());
        assertEquals("image/jpeg", note.attachPhoto(FieldNotePhoto.of(JPEG)).snapshot().photoMediaType());
        assertEquals("image/png", FieldNotePhoto.of(PNG).mediaType());
        assertThrows(IllegalArgumentException.class, () -> FieldNotePhoto.of("GIF89a".getBytes()));
        assertThrows(IllegalArgumentException.class, () -> FieldNotePhoto.of(new byte[0]));
        var tooLarge = new byte[FieldNotePhoto.MAX_BYTES + 1];
        System.arraycopy(JPEG, 0, tooLarge, 0, JPEG.length);
        assertThrows(IllegalArgumentException.class, () -> FieldNotePhoto.of(tooLarge));
    }

    @Test
    @DisplayName("Recognises a resent copy of itself")
    void recognisesItsCopy() {
        var note = note(TODAY);
        assertTrue(note.sameAs(plotId, FieldNoteType.IRRIGATION, new FieldNoteText("Revisar goteros de la fila 4."), TODAY));
        assertFalse(note.sameAs(plotId, FieldNoteType.PRUNING, text, TODAY));
        assertFalse(note.sameAs(new PlotId(), FieldNoteType.IRRIGATION, text, TODAY));
    }
}
