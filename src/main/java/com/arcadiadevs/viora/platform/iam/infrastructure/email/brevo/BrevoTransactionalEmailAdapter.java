package com.arcadiadevs.viora.platform.iam.infrastructure.email.brevo;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
import com.arcadiadevs.viora.platform.shared.infrastructure.deeplinks.configuration.DeepLinkProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.http.HttpClient;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Infrastructure adapter integrating with Brevo REST API v3 for transactional email delivery.
 *
 * <p>Implements the {@link TransactionalEmailService} outbound application port, satisfying
 * Hexagonal Architecture and the Dependency Inversion Principle. Each email body is read from a classpath template
 * under {@code templates/email/}; the password reset link is built from {@link DeepLinkProperties}; its images are served by the platform itself
 * from {@code static/email}, so they are linked with the public base URL of the platform.</p>
 */
@Component
public class BrevoTransactionalEmailAdapter implements TransactionalEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(BrevoTransactionalEmailAdapter.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String RESET_LINK_TEMPLATE = "templates/email/password-reset-link.html";
    private static final String VERIFICATION_CODE_TEMPLATE = "templates/email/email-verification-code.html";
    private static final String CODE_PLACEHOLDER = "{{code}}";
    private static final String RESET_LINK_PLACEHOLDER = "{{resetLink}}";
    private static final String ASSETS_BASE_URL_PLACEHOLDER = "{{assetsBaseUrl}}";

    private final RestClient restClient;
    private final boolean enabled;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final String resetSubject;
    private final String verificationSubject;
    private final String assetsBaseUrl;
    private final DeepLinkProperties deepLinks;
    private final String resetLinkTemplate;
    private final String verificationCodeTemplate;

    /**
     * Primary constructor for Spring injection, building a {@link RestClient} with connection and read timeouts.
     *
     * @param enabled      whether emails are actually sent; when false every call is a no-op
     * @param apiKey       the Brevo API key; when blank every call is a no-op
     * @param senderEmail  the sender address
     * @param senderName   the sender display name
     * @param resetSubject        the subject of the password reset email
     * @param verificationSubject the subject of the email verification email
     * @param assetsBaseUrl public base URL of the platform, where the email images are served
     * @param deepLinks     builds the password reset link that opens the app
     */
    @Autowired
    public BrevoTransactionalEmailAdapter(
            @Value("${viora.brevo.enabled:false}") boolean enabled,
            @Value("${viora.brevo.api-key:}") String apiKey,
            @Value("${viora.brevo.sender-email}") String senderEmail,
            @Value("${viora.brevo.sender-name}") String senderName,
            @Value("${viora.brevo.reset-subject}") String resetSubject,
            @Value("${viora.brevo.verification-subject}") String verificationSubject,
            @Value("${viora.brevo.assets-base-url}") String assetsBaseUrl,
            DeepLinkProperties deepLinks
    ) {
        this(defaultRestClient(), enabled, apiKey, senderEmail, senderName, resetSubject, verificationSubject,
                assetsBaseUrl, deepLinks);
    }

    /**
     * Constructor accepting a preconfigured {@link RestClient}, used by tests to stub the Brevo API.
     *
     * @param restClient   the HTTP client used to reach Brevo
     * @param enabled      whether emails are actually sent
     * @param apiKey       the Brevo API key
     * @param senderEmail  the sender address
     * @param senderName   the sender display name
     * @param resetSubject        the subject of the password reset email
     * @param verificationSubject the subject of the email verification email
     * @param assetsBaseUrl public base URL of the platform, where the email images are served
     * @param deepLinks     builds the password reset link that opens the app
     */
    public BrevoTransactionalEmailAdapter(
            RestClient restClient,
            boolean enabled,
            String apiKey,
            String senderEmail,
            String senderName,
            String resetSubject,
            String verificationSubject,
            String assetsBaseUrl,
            DeepLinkProperties deepLinks
    ) {
        this.restClient = restClient;
        this.enabled = enabled;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.resetSubject = resetSubject;
        this.verificationSubject = verificationSubject;
        this.assetsBaseUrl = assetsBaseUrl != null ? assetsBaseUrl.trim().replaceAll("/+$", "") : "";
        this.deepLinks = deepLinks;
        this.resetLinkTemplate = loadTemplate(RESET_LINK_TEMPLATE);
        this.verificationCodeTemplate = loadTemplate(VERIFICATION_CODE_TEMPLATE);
        if (!enabled || this.apiKey.isBlank()) {
            LOG.warn("Transactional emails are not sent: Brevo needs BREVO_ENABLED=true and BREVO_API_KEY (enabled={}, api key set={})",
                    enabled, !this.apiKey.isBlank());
        }
        if (deepLinks.baseUrl().isEmpty()) {
            LOG.warn("Password reset links have no domain: set VIORA_DEEP_LINK_BASE_URL so the reset email opens the app");
        }
    }

    /**
     * Sends the password reset email, whose button carries the link that opens the app, on the async executor, so
     * the HTTP request that asked for the link does not wait for Brevo.
     *
     * @param recipientEmail destination email address
     * @param resetToken     the token of the reset link
     * @throws IllegalStateException if Brevo rejects the request or cannot be reached
     */
    @Async
    @Override
    public void sendPasswordResetEmail(String recipientEmail, String resetToken) {
        var resetLink = deepLinks.passwordResetLink(URLEncoder.encode(resetToken, StandardCharsets.UTF_8));
        send(recipientEmail, resetSubject, resetLinkTemplate.replace(RESET_LINK_PLACEHOLDER, resetLink), "",
                "password reset", "email.password_reset.dispatch_failed");
    }

    /**
     * Sends the email verification email carrying the 6-digit code on the async executor, so the sign-up request
     * does not wait for Brevo.
     *
     * @param recipientEmail   destination email address
     * @param verificationCode the 6-digit verification code
     * @throws IllegalStateException if Brevo rejects the request or cannot be reached
     */
    @Async
    @Override
    public void sendEmailVerificationEmail(String recipientEmail, String verificationCode) {
        send(recipientEmail, verificationSubject, verificationCodeTemplate, verificationCode, "email verification",
                "email.email_verification.dispatch_failed");
    }

    private void send(String recipientEmail, String subject, String template, String code, String kind,
                      String failureKey) {
        if (!enabled || apiKey.isBlank()) {
            return;
        }

        var payload = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", recipientEmail)),
                "subject", subject,
                "htmlContent", template
                        .replace(CODE_PLACEHOLDER, code)
                        .replace(ASSETS_BASE_URL_PLACEHOLDER, assetsBaseUrl)
        );

        try {
            restClient.post()
                    .uri(BREVO_API_URL)
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            LOG.info("The {} email was accepted by Brevo", kind);
        } catch (RestClientResponseException ex) {
            // Brevo says why in the body, e.g. an API key used from an IP that is not in its authorized IPs list
            LOG.warn("Brevo rejected the {} email with {}: {}",
                    kind, ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new IllegalStateException(failureKey, ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException(failureKey, ex);
        }
    }

    /**
     * Uses the JDK HTTP client: unlike {@code HttpURLConnection} it keeps the body of a 401 answer, which is where
     * Brevo explains the rejection.
     */
    private static RestClient defaultRestClient() {
        var httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private static String loadTemplate(String path) {
        try (var input = new ClassPathResource(path).getInputStream()) {
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException("email.template.unreadable", ex);
        }
    }
}
