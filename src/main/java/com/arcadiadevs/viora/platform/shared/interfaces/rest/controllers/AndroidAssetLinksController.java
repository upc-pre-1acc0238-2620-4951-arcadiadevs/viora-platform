package com.arcadiadevs.viora.platform.shared.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.infrastructure.deeplinks.configuration.DeepLinkProperties;
import com.arcadiadevs.viora.platform.shared.interfaces.rest.resources.AssetLinkResource;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Stream;

/**
 * Serves the Digital Asset Links file that lets Android open the links of this domain in the Viora app.
 *
 * <p>Android downloads {@code /.well-known/assetlinks.json} when the app is installed and opens the app directly for
 * the matching links, such as the password reset link. While the app identity is not configured it answers an
 * empty list, which grants nothing.</p>
 */
@Hidden
@RestController
public class AndroidAssetLinksController {

    private static final String HANDLE_ALL_URLS = "delegate_permission/common.handle_all_urls";

    private final DeepLinkProperties deepLinkProperties;

    /**
     * Creates the controller.
     *
     * @param deepLinkProperties the configured app identity
     */
    public AndroidAssetLinksController(DeepLinkProperties deepLinkProperties) {
        this.deepLinkProperties = deepLinkProperties;
    }

    /**
     * Answers the asset links of the configured Android apps.
     *
     * @return the statements, empty while no app identity is configured
     */
    @GetMapping(value = "/.well-known/assetlinks.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<AssetLinkResource> assetLinks() {
        return Stream.of(deepLinkProperties.android(), deepLinkProperties.gestor().android())
                .filter(DeepLinkProperties.Android::isConfigured)
                .map(android -> new AssetLinkResource(
                        List.of(HANDLE_ALL_URLS),
                        new AssetLinkResource.Target("android_app", android.packageName(),
                                android.sha256CertFingerprints())))
                .toList();
    }
}
