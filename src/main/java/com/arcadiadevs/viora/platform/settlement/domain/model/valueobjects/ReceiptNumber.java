package com.arcadiadevs.viora.platform.settlement.domain.model.valueobjects;

import java.util.regex.Pattern;

/**
 * Official receipt number of a settlement voucher, written as {@code VR-{yy}-{nnnn}}.
 *
 * <p>{@code yy} is the last two digits of the campaign year and {@code nnnn} the per-producer sequence of that
 * year, so {@code 2026} plus the first receipt of the year reads {@code VR-26-0001}. The sequence is zero padded to
 * at least four digits and grows beyond them without breaking the format ({@code VR-26-10000}), up to
 * {@value #MAX_SEQUENCE}.</p>
 *
 * <p>The value object stores the rendered string next to the sequence and the campaign it was minted for, so a
 * receipt read back from persistence is checked against both: a string whose year does not match its campaign year
 * is corrupt data and is rejected instead of being served to the producer.</p>
 *
 * @param value       the rendered receipt number, for example {@code VR-26-0001}
 * @param sequence    the per-producer sequence of the campaign year, always positive
 * @param campaignYear the campaign year the receipt number belongs to
 */
public record ReceiptNumber(String value, int sequence, CampaignYear campaignYear) {

    /**
     * Well formed shape of a receipt number: two year digits followed by between four and six sequence digits, the
     * range {@link #MAX_SEQUENCE} covers.
     */
    private static final Pattern FORMAT = Pattern.compile("^VR-(\\d{2})-(\\d{4,6})$");

    /**
     * Largest sequence a receipt number can carry, which keeps the rendered number within the twelve characters of
     * the {@code receipt_number} column that stores it. A producer would have to close a million campaigns in a
     * single year to reach it.
     */
    public static final int MAX_SEQUENCE = 999_999;

    /**
     * Rebuilds a receipt number from persistence and checks it against the sequence and campaign year it is
     * expected to encode.
     *
     * @param value        the persisted receipt number
     * @param sequence     the sequence the receipt number is expected to carry
     * @param campaignYear the campaign year the receipt number is expected to belong to
     * @throws IllegalArgumentException if the value is null, blank or not well formed, if its sequence is not
     *                                  positive, if the campaign year is missing, or if the value and the campaign
     *                                  year disagree
     */
    public ReceiptNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("settlement.receipt_number.invalid");
        }
        var matcher = FORMAT.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("settlement.receipt_number.invalid");
        }
        sequence = Integer.parseInt(matcher.group(2));
        if (sequence <= 0) {
            throw new IllegalArgumentException("settlement.receipt_number.sequence.invalid");
        }
        if (campaignYear == null) {
            throw new IllegalArgumentException("settlement.reference.null");
        }
        if (Integer.parseInt(matcher.group(1)) != campaignYear.value() % 100) {
            throw new IllegalArgumentException("settlement.receipt_number.invalid");
        }
    }

    /**
     * Mints the receipt number of a settlement from its campaign year and sequence.
     *
     * @param campaignYear campaign the settlement belongs to
     * @param sequence     per-producer sequence of that campaign, starting at one
     * @return the receipt number of the sequence within the campaign year
     * @throws IllegalArgumentException if the campaign year is missing or the sequence is not a positive number
     *                                  within {@value #MAX_SEQUENCE}
     */
    public static ReceiptNumber of(CampaignYear campaignYear, int sequence) {
        if (campaignYear == null) {
            throw new IllegalArgumentException("settlement.reference.null");
        }
        if (sequence <= 0 || sequence > MAX_SEQUENCE) {
            throw new IllegalArgumentException("settlement.receipt_number.sequence.invalid");
        }
        return new ReceiptNumber("VR-%02d-%04d".formatted(campaignYear.value() % 100, sequence), sequence,
                campaignYear);
    }

    /**
     * Rebuilds a receipt number read from persistence, taking the campaign year from the settlement it belongs to
     * rather than from the value itself, and recovering the sequence it carries.
     *
     * @param value        the persisted receipt number
     * @param campaignYear campaign of the settlement the receipt number belongs to
     * @return the rebuilt receipt number
     * @throws IllegalArgumentException if the value is null, blank or not well formed, or if the value and the
     *                                  campaign year disagree
     */
    public static ReceiptNumber parse(String value, CampaignYear campaignYear) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("settlement.receipt_number.invalid");
        }
        var matcher = FORMAT.matcher(value);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("settlement.receipt_number.invalid");
        }
        return new ReceiptNumber(value, Integer.parseInt(matcher.group(2)), campaignYear);
    }
}
