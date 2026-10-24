package com.arcadiadevs.viora.platform.subscription.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.infrastructure.deeplinks.configuration.DeepLinkProperties;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Page Mercado Pago sends the producer back to after the hosted checkout ({@code /payments/return}, the
 * {@code back_urls} of every preference).
 *
 * <p>When Android opens the link in the app (verified App Link) this page is never reached. Otherwise it opens the
 * app with an Android intent URL that names the package. Nothing from the query string is used: the app reads the
 * payment state from the API, which only trusts the signed provider notifications.</p>
 */
@Hidden
@RestController
public class CheckoutReturnPageController {

    /** Path of the return link; the Android app declares the same one in its manifest. */
    public static final String PATH = "/payments/return";

    private static final String TEMPLATE = "templates/web/payment-return-open.html";

    private final DeepLinkProperties deepLinkProperties;
    private final String template;

    /**
     * Creates the controller.
     *
     * @param deepLinkProperties the link base URL and app package
     */
    public CheckoutReturnPageController(DeepLinkProperties deepLinkProperties) {
        this.deepLinkProperties = deepLinkProperties;
        this.template = readTemplate();
    }

    /**
     * Answers the page that takes the producer back to the app.
     *
     * @return 200 with the page
     */
    @GetMapping(value = PATH, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page() {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .body(template.replace("{{intentUrl}}", intentUrl()));
    }

    /** {@code intent://<host>/payments/return#Intent;scheme=https;package=<app>;end}. */
    private String intentUrl() {
        var link = URI.create(deepLinkProperties.baseUrl() + PATH);
        var packageName = deepLinkProperties.android().packageName();
        var intent = "intent://" + link.getHost() + link.getRawPath() + "#Intent;scheme=" + link.getScheme();
        if (!packageName.isEmpty()) {
            intent += ";package=" + packageName;
        }
        return intent + ";end";
    }

    private static String readTemplate() {
        try (var input = new ClassPathResource(TEMPLATE).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("subscription.checkout_return_page.template_missing", ex);
        }
    }
}
