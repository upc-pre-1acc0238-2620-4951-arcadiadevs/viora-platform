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
 * <p>Only mobile numbers of the chosen country are accepted: the app contacts producers on their cell phone, so a
 * landline, or a number of another country, is rejected.</p>
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
            if (region != null && !phoneNumberUtil.isValidNumberForRegion(parsed, region)) {
                throw new IllegalArgumentException("profile.phone.country_mismatch");
            }
            var type = phoneNumberUtil.getNumberType(parsed);
            // Some countries (e.g. the US) cannot tell mobile and fixed lines apart, so that ambiguous type is accepted
            if (type != PhoneNumberUtil.PhoneNumberType.MOBILE
                    && type != PhoneNumberUtil.PhoneNumberType.FIXED_LINE_OR_MOBILE) {
                throw new IllegalArgumentException("profile.phone.not_mobile");
            }
            return phoneNumberUtil.format(parsed, PhoneNumberUtil.PhoneNumberFormat.E164);
        } catch (NumberParseException ex) {
            throw new IllegalArgumentException("profile.phone.invalid_e164", ex);
        }
    }
}
