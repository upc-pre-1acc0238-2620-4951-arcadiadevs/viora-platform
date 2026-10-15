package com.arcadiadevs.viora.platform.profiles.infrastructure.phone.google;

import com.arcadiadevs.viora.platform.profiles.domain.model.ports.PhoneNumberValidator;
import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;
import org.springframework.stereotype.Component;

/**
 * Infrastructure adapter utilizing Google libphonenumber to parse, validate,
 * and format incoming raw telephone inputs into canonical ITU-T E.164 strings.
 *
 * <p>Implements the {@link PhoneNumberValidator} domain port, satisfying
 * Hexagonal Architecture and the Dependency Inversion Principle.</p>
 */
@Component
public class LibphonenumberAdapter implements PhoneNumberValidator {

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    @Override
    public String formatToE164(String rawNumber, String countryCode) {
        if (rawNumber == null || rawNumber.isBlank()) {
            throw new IllegalArgumentException("profile.phone.null_or_empty");
        }
        try {
            String region = countryCode != null ? countryCode.trim().toUpperCase() : null;
            Phonenumber.PhoneNumber parsed = phoneNumberUtil.parse(rawNumber.trim(), region);
            if (!phoneNumberUtil.isValidNumber(parsed)) {
                throw new IllegalArgumentException("profile.phone.invalid_e164");
            }
            return phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException ex) {
            throw new IllegalArgumentException("profile.phone.invalid_e164", ex);
        }
    }
}
