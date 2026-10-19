package com.arcadiadevs.viora.platform.iam.application.commandservices;

import com.arcadiadevs.viora.platform.iam.domain.model.commands.*;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/**
 * Application service port for mutating commands on user accounts.
 */
public interface UserAccountCommandService {

    /**
     * Handles sign-up: creates a pending account, or reuses a pending one, and emails its verification code.
     *
     * @param command registration command
     * @return the account identifier or application error
     */
    Result<String, ApplicationError> handle(RegisterUserAccountCommand command);

    /**
     * Verifies the email address of a pending account and activates it.
     *
     * @param command verification command
     * @return the activated account identifier or application error
     */
    Result<String, ApplicationError> handle(VerifyEmailCommand command);

    /**
     * Emails a new verification code to a pending account, respecting the resend cooldown.
     *
     * @param command resend command
     * @return empty success or application error
     */
    Result<Void, ApplicationError> handle(ResendEmailVerificationCommand command);

    /**
     * Handles authenticated password change.
     *
     * @param command password change command
     * @return success with updated user identifier or application error
     */
    Result<String, ApplicationError> handle(ChangeUserPasswordCommand command);

    /**
     * Handles password recovery request by generating a reset token and triggering notifications.
     *
     * @param command recovery request command
     * @return empty success or application error
     */
    Result<Void, ApplicationError> handle(RequestPasswordResetCommand command);

    /**
     * Consumes an active recovery token to set a new password.
     *
     * @param command reset password command
     * @return empty success or application error
     */
    Result<Void, ApplicationError> handle(ResetUserPasswordCommand command);

    /**
     * Discards the pending accounts abandoned before verifying their email, together with what other contexts
     * created for them.
     *
     * @param command discard command
     * @return how many accounts were discarded
     */
    Result<Integer, ApplicationError> handle(DiscardUnverifiedUserAccountsCommand command);
}
