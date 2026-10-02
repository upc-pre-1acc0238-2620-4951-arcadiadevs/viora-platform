package com.arcadiadevs.viora.platform.settlement.domain.services;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class CryptographicHashServiceTest {
    private final CryptographicHashService service = new CryptographicHashService();

    @Test
    void matchesTheKnownVectorOfTheEmptyInput() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
                service.sha256(new byte[0]).value());
    }

    @Test
    void matchesTheKnownVectorOfAbc() {
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
                service.sha256("abc".getBytes(StandardCharsets.US_ASCII)).value());
    }

    @Test
    void sameBytesGiveTheSameHash() {
        var bytes = "%PDF-1.4 dossier".getBytes(StandardCharsets.US_ASCII);
        assertEquals(service.sha256(bytes), service.sha256(bytes.clone()));
    }

    @Test
    void changingOneByteChangesTheHash() {
        var bytes = "%PDF-1.4 dossier".getBytes(StandardCharsets.US_ASCII);
        var changed = bytes.clone();
        changed[changed.length - 1] ^= 0x01;
        assertNotEquals(service.sha256(bytes), service.sha256(changed));
    }

    @Test
    void producesSixtyFourLowercaseHexadecimalCharacters() {
        var hash = service.sha256(new byte[]{(byte) 0xFF, 0x00, (byte) 0xAB}).value();
        assertEquals(64, hash.length());
        assertTrue(hash.matches("[0-9a-f]{64}"));
    }

    @Test
    void rejectsNullContent() {
        assertThrows(IllegalArgumentException.class, () -> service.sha256(null));
    }
}
