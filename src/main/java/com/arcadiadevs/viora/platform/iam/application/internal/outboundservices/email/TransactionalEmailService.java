package com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email;

/**
 * Outbound application port for delivering transactional system emails.
 *
 * <p>Enforces the Dependency Inversion Principle, decoupling the application layer
 * from specific third-party email delivery providers.</p>
 */
public interface TransactionalEmailService {

    /**
     * Sends an email with the 6-digit password recovery code.
     *
     * <p>Implementations may deliver asynchronously, so callers must not rely on the email being sent when the
     * method returns.</p>
     *
     * @param recipientEmail destination email address
     * @param resetToken     the 6-digit recovery code
     */
    void sendPasswordResetEmail(String recipientEmail, String resetToken);

    /**
     * Sends an email with the 6-digit code that verifies the address of a new account.
     *
     * <p>Implementations may deliver asynchronously, so callers must not rely on the email being sent when the
     * method returns.</p>
     *
     * @param recipientEmail   destination email address
     * @param verificationCode the 6-digit verification code
     */
    void sendEmailVerificationEmail(String recipientEmail, String verificationCode);
}
