package com.arcadiadevs.viora.platform.iam.domain.model.valueobjects;

/**
 * Value object grouping the optional profile details a user submits while signing up.
 *
 * <p>IAM does not store them: {@code UserAccount.register} only carries them in
 * {@code UserSignedUpWithProfileEvent} for the Profiles bounded context, which owns their full validation.</p>
 *
 * @param fullName    full civil name
 * @param country     country ISO alpha-2 code
 * @param phoneNumber telephone number as submitted
 */
public record OnboardingProfileDetails(String fullName, String country, String phoneNumber) {

    /**
     * Compact constructor validating that every detail is present.
     *
     * @throws IllegalArgumentException if any detail is null or blank
     */
    public OnboardingProfileDetails {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("profile.full_name.blank");
        }
        if (country == null || country.isBlank()) {
            throw new IllegalArgumentException("profile.country.blank");
        }
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("profile.phone_number.blank");
        }
    }
}
