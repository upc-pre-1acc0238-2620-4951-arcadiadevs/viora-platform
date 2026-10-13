package com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email;

/**
 * Outbound application port for delivering transactional system emails.
 *
 * <p>Enforces the Dependency Inversion Principle, decoupling the application layer
 * from specific third-party email delivery providers.</p>
 */
public interface TransactionalEmailService {

    /**
     * Sends an email with the ephemeral password recovery link/token.
     *
     * @param recipientEmail destination email address
     * @param resetToken ephemeral cryptographic recovery token
     */
    void sendPasswordResetEmail(String recipientEmail, String resetToken);
}
