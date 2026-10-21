package com.arcadiadevs.viora.platform.iam.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.infrastructure.deeplinks.configuration.DeepLinkProperties;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/**
 * Web page behind the password reset link of the email ({@code /password-resets/{token}}).
 *
 * <p>When Android opens the link in the app (verified App Link) this page is never reached. It is reached when the
 * phone opens the link in the browser instead: the installed app was signed with a certificate missing from
 * {@code assetlinks.json}, the link verification has not run yet, or the email app keeps links in its own browser.
 * The page then opens the app explicitly with an Android intent URL, which names the package and so does not depend
 * on the link verification. The token is only passed along; it is checked by the app through the IAM API.</p>
 */
@Hidden
@RestController
public class PasswordResetLinkPageController {

    private static final String TEMPLATE = "templates/web/password-reset-open.html";
    /** Reset tokens are URL-safe Base64; anything else is not a link the platform sent. */
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z0-9_-]{16,128}");

    private final DeepLinkProperties deepLinkProperties;
    private final String template;

    /**
     * Creates the controller.
     *
     * @param deepLinkProperties the link base URL, path and app package
     */
    public PasswordResetLinkPageController(DeepLinkProperties deepLinkProperties) {
        this.deepLinkProperties = deepLinkProperties;
        this.template = readTemplate();
    }

    /**
     * Answers the page that opens the reset link in the app.
     *
     * @param token the reset token, last segment of the link
     * @return 200 with the page, or 404 for something that is not a reset token
     */
    @GetMapping(value = "${viora.deep-link.password-reset-path:/password-resets}/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(@PathVariable String token) {
        if (!TOKEN.matcher(token).matches()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        var html = template.replace("{{intentUrl}}", intentUrl(token));
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
                .body(html);
    }

    /**
     * {@code intent://<host>/password-resets/<token>#Intent;scheme=https;package=<app>;end}: Chrome hands it to the
     * named app, which opens the same link it would have received from the email.
     */
    private String intentUrl(String token) {
        var link = URI.create(deepLinkProperties.passwordResetLink(token));
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
            throw new UncheckedIOException("iam.password_reset_page.template_missing", ex);
        }
    }
}
