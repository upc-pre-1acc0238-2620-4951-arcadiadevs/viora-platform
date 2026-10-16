package com.arcadiadevs.viora.platform.iam.infrastructure.email.brevo;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
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
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Infrastructure adapter integrating with Brevo REST API v3 for transactional email delivery.
 *
 * <p>Implements the {@link TransactionalEmailService} outbound application port, satisfying
 * Hexagonal Architecture and the Dependency Inversion Principle. The email body is read from the
 * {@code templates/email/password-reset-code.html} classpath template; its images are served by the platform itself
 * from {@code static/email}, so they are linked with the public base URL of the platform.</p>
 */
@Component
public class BrevoTransactionalEmailAdapter implements TransactionalEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(BrevoTransactionalEmailAdapter.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String RESET_CODE_TEMPLATE = "templates/email/password-reset-code.html";
    private static final String CODE_PLACEHOLDER = "{{code}}";
    private static final String ASSETS_BASE_URL_PLACEHOLDER = "{{assetsBaseUrl}}";

    private final RestClient restClient;
    private final boolean enabled;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final String resetSubject;
    private final String assetsBaseUrl;
    private final String resetCodeTemplate;

    /**
     * Primary constructor for Spring injection, building a {@link RestClient} with connection and read timeouts.
     *
     * @param enabled      whether emails are actually sent; when false every call is a no-op
     * @param apiKey       the Brevo API key; when blank every call is a no-op
     * @param senderEmail  the sender address
     * @param senderName   the sender display name
     * @param resetSubject  the subject of the password reset email
     * @param assetsBaseUrl public base URL of the platform, where the email images are served
     */
    @Autowired
    public BrevoTransactionalEmailAdapter(
            @Value("${viora.brevo.enabled:false}") boolean enabled,
            @Value("${viora.brevo.api-key:}") String apiKey,
            @Value("${viora.brevo.sender-email}") String senderEmail,
            @Value("${viora.brevo.sender-name}") String senderName,
            @Value("${viora.brevo.reset-subject}") String resetSubject,
            @Value("${viora.brevo.assets-base-url}") String assetsBaseUrl
    ) {
        this(defaultRestClient(), enabled, apiKey, senderEmail, senderName, resetSubject, assetsBaseUrl);
    }

    /**
     * Constructor accepting a preconfigured {@link RestClient}, used by tests to stub the Brevo API.
     *
     * @param restClient   the HTTP client used to reach Brevo
     * @param enabled      whether emails are actually sent
     * @param apiKey       the Brevo API key
     * @param senderEmail  the sender address
     * @param senderName   the sender display name
     * @param resetSubject  the subject of the password reset email
     * @param assetsBaseUrl public base URL of the platform, where the email images are served
     */
    public BrevoTransactionalEmailAdapter(
            RestClient restClient,
            boolean enabled,
            String apiKey,
            String senderEmail,
            String senderName,
            String resetSubject,
            String assetsBaseUrl
    ) {
        this.restClient = restClient;
        this.enabled = enabled;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.resetSubject = resetSubject;
        this.assetsBaseUrl = assetsBaseUrl != null ? assetsBaseUrl.trim().replaceAll("/+$", "") : "";
        this.resetCodeTemplate = loadTemplate(RESET_CODE_TEMPLATE);
        if (!enabled || this.apiKey.isBlank()) {
            LOG.warn("Password reset emails are not sent: Brevo needs BREVO_ENABLED=true and BREVO_API_KEY (enabled={}, api key set={})",
                    enabled, !this.apiKey.isBlank());
        }
    }

    /**
     * Sends the password reset email carrying the 6-digit recovery code on the async executor, so the HTTP
     * request that asked for the code does not wait for Brevo.
     *
     * @param recipientEmail destination email address
     * @param resetToken     the 6-digit recovery code
     * @throws IllegalStateException if Brevo rejects the request or cannot be reached
     */
    @Async
    @Override
    public void sendPasswordResetEmail(String recipientEmail, String resetToken) {
        if (!enabled || apiKey.isBlank()) {
            return;
        }

        var payload = Map.of(
                "sender", Map.of("name", senderName, "email", senderEmail),
                "to", List.of(Map.of("email", recipientEmail)),
                "subject", resetSubject,
                "htmlContent", resetCodeTemplate
                        .replace(CODE_PLACEHOLDER, resetToken)
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
            LOG.info("Password reset email accepted by Brevo");
        } catch (RestClientResponseException ex) {
            // Brevo says why in the body, e.g. an API key used from an IP that is not in its authorized IPs list
            LOG.warn("Brevo rejected the password reset email with {}: {}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw new IllegalStateException("email.password_reset.dispatch_failed", ex);
        } catch (RestClientException ex) {
            throw new IllegalStateException("email.password_reset.dispatch_failed", ex);
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
