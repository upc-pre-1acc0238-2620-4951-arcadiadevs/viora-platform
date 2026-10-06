package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/** The receipt number is the identity a producer sees on a settlement voucher, so its format is a contract. */
class ReceiptNumberTest {
    private static final CampaignYear YEAR_2026 = new CampaignYear(2026);

    @Test
    void mintsTheFirstReceiptOfACampaign() {
        var receipt = ReceiptNumber.of(YEAR_2026, 1);
        assertEquals("VR-26-0001", receipt.value());
        assertEquals(1, receipt.sequence());
        assertEquals(YEAR_2026, receipt.campaignYear());
    }

    @Test
    void padsTheSequenceToFourDigits() {
        assertEquals("VR-26-0001", ReceiptNumber.of(YEAR_2026, 1).value());
        assertEquals("VR-26-0009", ReceiptNumber.of(YEAR_2026, 9).value());
        assertEquals("VR-26-0010", ReceiptNumber.of(YEAR_2026, 10).value());
        assertEquals("VR-26-0100", ReceiptNumber.of(YEAR_2026, 100).value());
        assertEquals("VR-26-9999", ReceiptNumber.of(YEAR_2026, 9999).value());
    }

    @Test
    void takesTheLastTwoDigitsOfTheCampaignYear() {
        assertEquals("VR-05-0001", ReceiptNumber.of(new CampaignYear(2005), 1).value());
        assertEquals("VR-26-0001", ReceiptNumber.of(new CampaignYear(2026), 1).value());
        assertEquals("VR-99-0001", ReceiptNumber.of(new CampaignYear(2099), 1).value());
    }

    @Test
    void growsBeyondFourDigitsWithoutBreakingTheFormat() {
        var receipt = ReceiptNumber.of(YEAR_2026, 10_000);
        assertEquals("VR-26-10000", receipt.value());
        assertEquals(10_000, receipt.sequence());
        assertEquals("VR-26-100000", ReceiptNumber.of(YEAR_2026, 100_000).value());
    }

    @Test
    void rendersTheLongestSequenceWithinTheColumnThatStoresIt() {
        var longest = ReceiptNumber.of(YEAR_2026, ReceiptNumber.MAX_SEQUENCE);
        assertEquals("VR-26-999999", longest.value());
        // harvest_settlements.receipt_number is varchar(12): the domain may never mint a longer value.
        assertEquals(12, longest.value().length());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -999, ReceiptNumber.MAX_SEQUENCE + 1, Integer.MAX_VALUE})
    void rejectsASequenceTheColumnCouldNotStore(int sequence) {
        var error = assertThrows(IllegalArgumentException.class, () -> ReceiptNumber.of(YEAR_2026, sequence));
        assertEquals("settlement.receipt_number.sequence.invalid", error.getMessage());
    }

    @Test
    void rejectsAMissingCampaignYear() {
        assertEquals("settlement.reference.null",
                assertThrows(IllegalArgumentException.class, () -> ReceiptNumber.of(null, 1)).getMessage());
        assertEquals("settlement.reference.null",
                assertThrows(IllegalArgumentException.class, () -> ReceiptNumber.parse("VR-26-0001", null))
                        .getMessage());
    }

    @Test
    void readsBackTheSequenceAndTheCampaignOfAStoredValue() {
        var receipt = ReceiptNumber.parse("VR-26-0012", YEAR_2026);
        assertEquals(12, receipt.sequence());
        assertEquals(YEAR_2026, receipt.campaignYear());
        assertEquals(receipt, ReceiptNumber.of(YEAR_2026, 12));
        assertEquals(receipt.hashCode(), ReceiptNumber.of(YEAR_2026, 12).hashCode());
    }

    @Test
    void readsBackTheLongestStoredValue() {
        var stored = ReceiptNumber.parse("VR-26-999999", YEAR_2026);
        assertEquals(ReceiptNumber.MAX_SEQUENCE, stored.sequence());
        assertEquals(ReceiptNumber.of(YEAR_2026, ReceiptNumber.MAX_SEQUENCE), stored);
    }

    @Test
    void rejectsAStoredValueWhoseYearDisagreesWithItsCampaign() {
        var error = assertThrows(IllegalArgumentException.class,
                () -> ReceiptNumber.parse("VR-27-0001", YEAR_2026));
        assertEquals("settlement.receipt_number.invalid", error.getMessage());
        assertEquals("settlement.receipt_number.invalid",
                assertThrows(IllegalArgumentException.class, () -> ReceiptNumber.parse("VR-05-0001",
                        new CampaignYear(2026))).getMessage());
    }

    @Test
    void rejectsAStoredValueWithoutAPositiveSequence() {
        assertEquals("settlement.receipt_number.sequence.invalid",
                assertThrows(IllegalArgumentException.class, () -> ReceiptNumber.parse("VR-26-0000", YEAR_2026))
                        .getMessage());
        assertEquals("settlement.receipt_number.sequence.invalid",
                assertThrows(IllegalArgumentException.class, () -> new ReceiptNumber("VR-26-0000", 0, YEAR_2026))
                        .getMessage());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "26-0001",             // no prefix
            "vr-26-0001",          // lowercase prefix
            "VR-26-1",             // three sequence digits
            "VR-260001",           // no separator
            "VR-26-0001 ",         // not trimmed: what is stored is what was minted
            "VR-26-0001x",
            "VR-26--0001",
            "VR-2-0001",           // one year digit
            "VR-2026-0001",        // the full year
            "XR-26-0001",
            "VR-26-1000000"        // seven sequence digits: longer than the column can store
    })
    void rejectsAValueThatIsNotWellFormed(String value) {
        assertEquals("settlement.receipt_number.invalid",
                assertThrows(IllegalArgumentException.class, () -> ReceiptNumber.parse(value, YEAR_2026))
                        .getMessage());
        assertEquals("settlement.receipt_number.invalid",
                assertThrows(IllegalArgumentException.class, () -> new ReceiptNumber(value, 1, YEAR_2026))
                        .getMessage());
    }

    @Test
    void readsBackFiveAndSixDigitSequencesBecauseBothStillFitTheColumn() {
        assertEquals(1, ReceiptNumber.parse("VR-26-00001", YEAR_2026).sequence());
        assertEquals(123456, ReceiptNumber.parse("VR-26-123456", YEAR_2026).sequence());
        assertEquals("VR-26-123456", ReceiptNumber.of(YEAR_2026, 123_456).value());
    }
}
