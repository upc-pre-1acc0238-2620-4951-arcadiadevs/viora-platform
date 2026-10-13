package com.arcadiadevs.viora.platform.iam.infrastructure.email.brevo;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Infrastructure adapter integrating with Brevo REST API v3 for transactional email delivery.
 *
 * <p>Implements the {@link TransactionalEmailService} outbound application port, satisfying
 * Hexagonal Architecture and the Dependency Inversion Principle.</p>
 */
@Component
public class BrevoTransactionalEmailAdapter implements TransactionalEmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final String resetBaseUrl;
    private final String resetSubject;
    private final RestClient restClient;

    public BrevoTransactionalEmailAdapter(
            @Value("${viora.brevo.api-key:}") String apiKey,
            @Value("${viora.brevo.sender-email:no-reply@viora.ag}") String senderEmail,
            @Value("${viora.brevo.sender-name:Viora Platform}") String senderName,
            @Value("${viora.brevo.reset-base-url:https://app.viora.ag/reset-password?token=}") String resetBaseUrl,
            @Value("${viora.brevo.reset-subject:Restablecimiento de contraseña — Viora}") String resetSubject
    ) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.senderEmail = senderEmail != null ? senderEmail.trim() : "no-reply@viora.ag";
        this.senderName = senderName != null ? senderName.trim() : "Viora Platform";
        this.resetBaseUrl = resetBaseUrl != null ? resetBaseUrl.trim() : "https://app.viora.ag/reset-password?token=";
        this.resetSubject = resetSubject != null ? resetSubject.trim() : "Restablecimiento de contraseña — Viora";

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(15).toMillis());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String resetToken) {
        if (apiKey.isBlank()) {
            return;
        }

        try {
            String resetLink = resetBaseUrl + resetToken;
            String payload = """
                    {
                      "sender": { "name": "%s", "email": "%s" },
                      "to": [ { "email": "%s" } ],
                      "subject": "%s",
                      "htmlContent": "<p>Hola,</p><p>Has solicitado restablecer tu contraseña en Viora.</p><p>Tu enlace de recuperación es: <a href=\\"%s\\">%s</a></p><p>Este enlace vence en 15 minutos y es de un solo uso.</p><p>Si no solicitaste este cambio, puedes ignorar este mensaje.</p>"
                    }
                    """.formatted(senderName, senderEmail, recipientEmail, resetSubject, resetLink, resetLink);

            restClient.post()
                    .uri(BREVO_API_URL)
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to dispatch transactional password reset email via Brevo: " + ex.getMessage(), ex);
        }
    }
}
