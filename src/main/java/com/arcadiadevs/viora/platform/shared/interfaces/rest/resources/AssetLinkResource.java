package com.arcadiadevs.viora.platform.shared.interfaces.rest.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * One statement of the Digital Asset Links file, declaring that an Android app may open the links of this domain.
 *
 * @param relation the granted relation
 * @param target   the Android app the relation is granted to
 */
public record AssetLinkResource(List<String> relation, Target target) {

    /**
     * The Android app a statement refers to.
     *
     * @param namespace              always {@code android_app}
     * @param packageName            application id of the app
     * @param sha256CertFingerprints SHA-256 fingerprints of the certificates that sign the app
     */
    public record Target(
            String namespace,
            @JsonProperty("package_name") String packageName,
            @JsonProperty("sha256_cert_fingerprints") List<String> sha256CertFingerprints
    ) {
    }
}
