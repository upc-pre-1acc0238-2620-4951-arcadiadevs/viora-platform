package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class DossierValueObjectsTest {
    private static final String HASH = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

    private static byte[] pdf(String body) {
        return ("%PDF-1.7 " + body).getBytes(StandardCharsets.US_ASCII);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855",
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85",
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b8555",
            "g3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"})
    void rejectsInvalidVerificationHashes(String value) {
        assertThrows(IllegalArgumentException.class, () -> new VerificationHash(value));
    }

    @Test
    void acceptsAValidVerificationHash() {
        assertEquals(HASH, new VerificationHash(HASH).value());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void rejectsBlankSignatures(String value) {
        assertThrows(IllegalArgumentException.class, () -> new AuditorSignature(value));
    }

    @Test
    void trimsSignaturesAndRejectsOverlongOnes() {
        assertEquals("CIP-49120", new AuditorSignature("  CIP-49120 ").value());
        assertDoesNotThrow(() -> new AuditorSignature("x".repeat(AuditorSignature.MAX_LENGTH)));
        assertThrows(IllegalArgumentException.class,
                () -> new AuditorSignature("x".repeat(AuditorSignature.MAX_LENGTH + 1)));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\t"})
    void blankNotesAreAbsent(String value) {
        var notes = new CertificationNotes(value);
        assertNull(notes.value());
        assertFalse(notes.isPresent());
        assertEquals(CertificationNotes.none(), notes);
    }

    @Test
    void trimsNotesAndRejectsOverlongOnes() {
        assertEquals("Verified", new CertificationNotes("  Verified ").value());
        assertTrue(new CertificationNotes("x").isPresent());
        assertDoesNotThrow(() -> new CertificationNotes("x".repeat(CertificationNotes.MAX_LENGTH)));
        var error = assertThrows(IllegalArgumentException.class,
                () -> new CertificationNotes("x".repeat(CertificationNotes.MAX_LENGTH + 1)));
        assertEquals("settlement.certification.notes.too_long", error.getMessage());
    }

    @Test
    void validatesTheCertifierIdentity() {
        var identity = new CertifierIdentity(" Ing. Sanchez ", " 49120 ");
        assertEquals("Ing. Sanchez", identity.name());
        assertEquals("49120", identity.cipNumber());
        assertThrows(IllegalArgumentException.class, () -> new CertifierIdentity(" ", "1"));
        assertThrows(IllegalArgumentException.class, () -> new CertifierIdentity("A", null));
        assertThrows(IllegalArgumentException.class, () -> new CertifierIdentity("x".repeat(121), "1"));
        assertThrows(IllegalArgumentException.class, () -> new CertifierIdentity("A", "1".repeat(21)));
    }

    @Test
    void validatesTheMetadata() {
        var hash = new VerificationHash(HASH);
        var signature = new AuditorSignature("sig");
        assertThrows(IllegalArgumentException.class, () -> new DossierMetadata(null, signature, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> new DossierMetadata(hash, null, Instant.now()));
        assertThrows(IllegalArgumentException.class, () -> new DossierMetadata(hash, signature, null));
    }

    @Test
    void certificationIdsAreUuids() {
        assertNotEquals(new CertificationId(), new CertificationId());
        assertThrows(IllegalArgumentException.class, () -> new CertificationId("nope"));
        assertThrows(IllegalArgumentException.class, () -> new CertificationId(" "));
    }

    @Test
    void rejectsContentThatIsNotAPdf() {
        assertThrows(IllegalArgumentException.class, () -> new DossierDocument(null));
        assertThrows(IllegalArgumentException.class, () -> new DossierDocument(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> new DossierDocument(new byte[]{'%', 'P', 'D', 'F'}));
        assertThrows(IllegalArgumentException.class,
                () -> new DossierDocument("{\"json\":true}".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsOversizedDocuments() {
        var big = new byte[DossierDocument.MAX_BYTES + 1];
        System.arraycopy(pdf(""), 0, big, 0, 9);
        assertThrows(IllegalArgumentException.class, () -> new DossierDocument(big));
    }

    @Test
    void copiesTheBytesOnTheWayInAndOut() {
        var source = pdf("original");
        var document = new DossierDocument(source);
        source[10] = 'X';
        assertArrayEquals(pdf("original"), document.content());
        var exposed = document.content();
        exposed[10] = 'Y';
        assertArrayEquals(pdf("original"), document.content());
        assertEquals(pdf("original").length, document.size());
    }

    @Test
    void comparesDocumentsByContent() {
        assertEquals(new DossierDocument(pdf("a")), new DossierDocument(pdf("a")));
        assertEquals(new DossierDocument(pdf("a")).hashCode(), new DossierDocument(pdf("a")).hashCode());
        assertNotEquals(new DossierDocument(pdf("a")), new DossierDocument(pdf("b")));
        assertFalse(new DossierDocument(pdf("a")).toString().contains("PDF-"));
    }
}
