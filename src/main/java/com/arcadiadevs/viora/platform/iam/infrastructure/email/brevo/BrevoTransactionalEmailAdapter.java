package com.arcadiadevs.viora.platform.iam.infrastructure.email.brevo;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

/**
 * Infrastructure adapter integrating with Brevo REST API v3 for transactional email delivery.
 *
 * <p>Implements the {@link TransactionalEmailService} outbound application port, satisfying
 * Hexagonal Architecture and the Dependency Inversion Principle.</p>
 */
@Component
public class BrevoTransactionalEmailAdapter implements TransactionalEmailService {

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private final boolean enabled;
    private final String apiKey;
    private final String senderEmail;
    private final String senderName;
    private final String resetSubject;
    private final RestClient restClient;

    @Autowired
    public BrevoTransactionalEmailAdapter(
            @Value("${viora.brevo.enabled:true}") boolean enabled,
            @Value("${viora.brevo.api-key:}") String apiKey,
            @Value("${viora.brevo.sender-email:viora@arcadiadevs.com}") String senderEmail,
            @Value("${viora.brevo.sender-name:Viora}") String senderName,
            @Value("${viora.brevo.reset-subject:Código de recuperación de contraseña — Viora}") String resetSubject
    ) {
        this.enabled = enabled;
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.senderEmail = senderEmail != null ? senderEmail.trim() : "viora@arcadiadevs.com";
        this.senderName = senderName != null ? senderName.trim() : "Viora";
        this.resetSubject = resetSubject != null ? resetSubject.trim() : "Código de recuperación de contraseña — Viora";

        var requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        requestFactory.setReadTimeout((int) Duration.ofSeconds(15).toMillis());

        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    /**
     * Overloaded constructor maintaining backwards compatibility for existing tests.
     */
    public BrevoTransactionalEmailAdapter(
            String apiKey,
            String senderEmail,
            String senderName,
            String resetBaseUrl,
            String resetSubject
    ) {
        this(true, apiKey, senderEmail, senderName, resetSubject);
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, String resetToken) {
        if (!enabled || apiKey.isBlank()) {
            return;
        }

        try {
            String htmlContent = buildOtpHtmlContent(resetToken);
            var request = new BrevoEmailRequest(
                    new BrevoSender(senderName, senderEmail),
                    List.of(new BrevoRecipient(recipientEmail)),
                    resetSubject,
                    htmlContent
            );

            restClient.post()
                    .uri(BREVO_API_URL)
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to dispatch transactional password reset email via Brevo: " + ex.getMessage(), ex);
        }
    }

    private String buildOtpHtmlContent(String otpCode) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Restablecimiento de contraseña</title>
                </head>
                <body style="margin:0;padding:24px;background-color:#f8fafc;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#1e293b;">
                  <div style="max-width:520px;margin:0 auto;background-color:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 6px -1px rgba(0,0,0,0.05),0 2px 4px -2px rgba(0,0,0,0.05);border:1px solid #e2e8f0;">
                    <div style="background-color:#0f5132;padding:28px 24px;text-align:center;">
                      <h1 style="margin:0;color:#ffffff;font-size:26px;font-weight:700;letter-spacing:-0.5px;">Viora</h1>
                      <p style="margin:4px 0 0 0;color:#d1e7dd;font-size:12px;text-transform:uppercase;letter-spacing:1.5px;">Plataforma Agronómica</p>
                    </div>
                    <div style="padding:32px 24px;">
                      <h2 style="margin:0 0 14px 0;font-size:20px;font-weight:600;color:#0f172a;">Recuperación de contraseña</h2>
                      <p style="margin:0 0 20px 0;font-size:15px;line-height:1.6;color:#475569;">
                        Hemos recibido una solicitud para restablecer la contraseña de tu cuenta en la aplicación móvil de <strong>Viora</strong>.
                      </p>
                      <p style="margin:0 0 10px 0;font-size:14px;color:#64748b;text-align:center;">
                        Ingresa el siguiente código de verificación en tu aplicación:
                      </p>
                      <div style="background-color:#f1f5f9;border:2px dashed #cbd5e1;border-radius:12px;padding:16px 20px;text-align:center;margin:18px 0;">
                        <span style="font-family:'Courier New',Courier,monospace;font-size:36px;font-weight:800;letter-spacing:8px;color:#0f5132;display:inline-block;">%s</span>
                      </div>
                      <p style="margin:14px 0 0 0;font-size:13px;line-height:1.5;color:#64748b;text-align:center;">
                        ⏱ Este código es de un solo uso y vencerá en <strong>15 minutos</strong>.
                      </p>
                      <hr style="border:none;border-top:1px solid #e2e8f0;margin:28px 0 20px 0;">
                      <p style="margin:0;font-size:12px;line-height:1.5;color:#94a3b8;text-align:center;">
                        Si tú no realizaste esta solicitud, puedes ignorar este mensaje de forma segura. Tu contraseña no cambiará.
                      </p>
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(otpCode);
    }

    private record BrevoSender(String name, String email) {}
    private record BrevoRecipient(String email) {}
    private record BrevoEmailRequest(
            BrevoSender sender,
            List<BrevoRecipient> to,
            String subject,
            String htmlContent
    ) {}
}
