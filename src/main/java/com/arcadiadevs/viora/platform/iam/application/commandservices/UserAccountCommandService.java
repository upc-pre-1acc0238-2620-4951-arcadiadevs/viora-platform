package com.arcadiadevs.viora.platform.iam.application.commandservices;

import com.arcadiadevs.viora.platform.iam.domain.model.commands.*;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;

/**
 * Application service port for mutating commands on user accounts.
 */
public interface UserAccountCommandService {

    /**
     * Handles new user account registration.
     *
     * @param command registration command
     * @return created user account identifier or application error
     */
    Result<String, ApplicationError> handle(RegisterUserAccountCommand command);

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
}
