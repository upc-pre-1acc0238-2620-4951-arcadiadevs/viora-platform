package com.arcadiadevs.viora.platform.profiles.domain.model.ports;

/**
 * Domain port defining the contract for parsing, validating, and normalizing
 * raw telephone numbers into canonical ITU-T E.164 format.
 *
 * <p>Implemented by infrastructure adapters utilizing telephone libraries
 * (such as Google libphonenumber) to prevent third-party technical dependencies
 * from leaking into the domain core.</p>
 */
public interface PhoneNumberValidator {

    /**
     * Formats and validates a raw telephone number against an ISO country code into E.164.
     *
     * @param rawNumber raw telephone number string
     * @param countryCode ISO-3166-1 alpha-2 country code (e.g. PE, CL, ES)
     * @return normalized E.164 formatted string (e.g. {@code +51987654321})
     * @throws IllegalArgumentException if the phone number cannot be parsed or is invalid for the region
     */
    String formatToE164(String rawNumber, String countryCode);
}
