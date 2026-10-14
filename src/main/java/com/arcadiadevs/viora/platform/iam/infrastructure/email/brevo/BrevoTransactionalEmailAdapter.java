package com.arcadiadevs.viora.platform.iam.infrastructure.email.brevo;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Infrastructure adapter integrating with Brevo REST API v3 for transactional email delivery.
 *
 * <p>Implements the {@link TransactionalEmailService} outbound application port, satisfying
 * Hexagonal Architecture and the Dependency Inversion Principle. The email body is read from the
 * {@code templates/email/password-reset-code.html} classpath template.</p>
 */
@Component
public class BrevoTransactionalEmailAdapter implements TransactionalEmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String RESET_CODE_TEMPLATE = "templates/email/password-reset-code.html";
    private static final String CODE_PLACEHOLDER = "{{code}}";

    private final RestClient restClient;
    private final boolean enabled;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final String resetSubject;
    private final String resetCodeTemplate;

    /**
     * Primary constructor for Spring injection, building a {@link RestClient} with connection and read timeouts.
     *
     * @param enabled      whether emails are actually sent; when false every call is a no-op
     * @param apiKey       the Brevo API key; when blank every call is a no-op
     * @param senderEmail  the sender address
     * @param senderName   the sender display name
     * @param resetSubject the subject of the password reset email
     */
    @Autowired
    public BrevoTransactionalEmailAdapter(
            @Value("${viora.brevo.enabled:false}") boolean enabled,
            @Value("${viora.brevo.api-key:}") String apiKey,
            @Value("${viora.brevo.sender-email}") String senderEmail,
            @Value("${viora.brevo.sender-name}") String senderName,
            @Value("${viora.brevo.reset-subject}") String resetSubject
    ) {
        this(defaultRestClient(), enabled, apiKey, senderEmail, senderName, resetSubject);
    }

    /**
     * Constructor accepting a preconfigured {@link RestClient}, used by tests to stub the Brevo API.
     *
     * @param restClient   the HTTP client used to reach Brevo
     * @param enabled      whether emails are actually sent
     * @param apiKey       the Brevo API key
     * @param senderEmail  the sender address
     * @param senderName   the sender display name
     * @param resetSubject the subject of the password reset email
     */
    public BrevoTransactionalEmailAdapter(
            RestClient restClient,
            boolean enabled,
            String apiKey,
            String senderEmail,
            String senderName,
            String resetSubject
    ) {
        this.restClient = restClient;
        this.enabled = enabled;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.senderEmail = senderEmail;
        this.senderName = senderName;
        this.resetSubject = resetSubject;
        this.resetCodeTemplate = loadTemplate(RESET_CODE_TEMPLATE);
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
                "htmlContent", resetCodeTemplate.replace(CODE_PLACEHOLDER, resetToken)
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
        } catch (RestClientException ex) {
            // Not logged here: the async uncaught exception handler logs it once, with the cause attached
            throw new IllegalStateException("email.password_reset.dispatch_failed", ex);
        }
    }

    private static RestClient defaultRestClient() {
        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(15).toMillis());
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
