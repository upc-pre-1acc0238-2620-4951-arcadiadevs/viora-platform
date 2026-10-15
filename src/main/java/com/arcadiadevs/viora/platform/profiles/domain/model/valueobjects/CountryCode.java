package com.arcadiadevs.viora.platform.profiles.domain.model.valueobjects;

import java.util.Locale;
import java.util.Set;

/**
 * Value object representing an ISO-3166-1 alpha-2 country code.
 *
 * @param code the two-letter uppercase country code
 */
public record CountryCode(String code) {

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());

    /**
     * Compact constructor validating that the country code is an existing ISO-3166-1 alpha-2 code.
     *
     * @throws IllegalArgumentException if code is null, blank, or not recognized in ISO-3166-1
     */
    public CountryCode {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("profile.country.null_or_empty");
        }
        code = code.trim().toUpperCase();
        if (code.length() != 2 || !ISO_COUNTRIES.contains(code)) {
            throw new IllegalArgumentException("profile.country.invalid_iso_alpha2");
        }
    }
}
