package com.arcadiadevs.viora.platform.shared.infrastructure.deeplinks.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration of the links that open the mobile app directly (Android App Links).
 *
 * <p>The password reset email carries {@code baseUrl + passwordResetPath + "/" + token}, such as
 * {@code https://viora.arcadiadevs.com/password-resets/Ab9...}: the link names the reset resource, as the API does.
 * Android opens the app for that URL only if {@code /.well-known/assetlinks.json}, served from the same domain, names the app package and the
 * SHA-256 fingerprint of the certificate that signs it.</p>
 *
 * @param baseUrl           public https URL of the domain the links point to, without trailing slash
 * @param passwordResetPath path of the password reset resource the app handles, without trailing slash
 * @param android           identity of the Android app allowed to open the links
 */
@ConfigurationProperties(prefix = "viora.deep-link")
public record DeepLinkProperties(String baseUrl, String passwordResetPath, Android android) {

    /**
     * Normalizes missing values to empty ones, so an unconfigured property never breaks the startup.
     */
    public DeepLinkProperties {
        baseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        passwordResetPath = passwordResetPath == null || passwordResetPath.isBlank()
                ? "/password-resets" : passwordResetPath.trim().replaceAll("/+$", "");
        android = android == null ? new Android("", List.of()) : android;
    }

    /**
     * Builds the password reset link, with the token as the last path segment.
     *
     * @param resetToken the token of the reset link, already URL safe
     * @return the absolute link
     */
    public String passwordResetLink(String resetToken) {
        return baseUrl + passwordResetPath + "/" + resetToken;
    }

    /**
     * Identity of the Android app allowed to open the links.
     *
     * @param packageName            application id of the app, such as {@code com.arcadiadevs.viora}
     * @param sha256CertFingerprints SHA-256 fingerprints of the certificates that sign the app (debug, upload or
     *                               Play app signing), as {@code AB:CD:...}
     */
    public record Android(String packageName, List<String> sha256CertFingerprints) {

        /**
         * Normalizes missing values and drops blank fingerprints.
         */
        public Android {
            packageName = packageName == null ? "" : packageName.trim();
            sha256CertFingerprints = sha256CertFingerprints == null ? List.of() : sha256CertFingerprints.stream()
                    .filter(fingerprint -> fingerprint != null && !fingerprint.isBlank())
                    .map(String::trim)
                    .toList();
        }

        /**
         * Checks whether the app identity is complete enough to publish an asset link.
         *
         * @return true if the package name and at least one fingerprint are set
         */
        public boolean isConfigured() {
            return !packageName.isEmpty() && !sha256CertFingerprints.isEmpty();
        }
    }
}
