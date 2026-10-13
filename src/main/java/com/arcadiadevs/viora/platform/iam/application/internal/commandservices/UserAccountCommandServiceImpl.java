package com.arcadiadevs.viora.platform.iam.application.internal.commandservices;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ChangeUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RequestPasswordResetCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResetUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Application service implementation orchestrating mutating commands on user accounts.
 */
@Service
@Transactional
public class UserAccountCommandServiceImpl implements UserAccountCommandService {

    private final UserAccountRepository userAccountRepository;
    private final ApplicationEventPublisher eventPublisher;

    public UserAccountCommandServiceImpl(
            UserAccountRepository userAccountRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        if (userAccountRepository == null) {
            throw new IllegalArgumentException("user.repository.null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("user.event_publisher.null");
        }
        this.userAccountRepository = userAccountRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public Result<String, ApplicationError> handle(RegisterUserAccountCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "user.command.null"));
        }
        try {
            var email = new EmailAddress(command.email());
            if (userAccountRepository.existsByEmail(email)) {
                return Result.failure(ApplicationError.conflict("user", "user.email.already_exists"));
            }

            var passwordHash = new PasswordHash(command.passwordHash());
            var role = UserRole.from(command.role());

            var account = UserAccount.register(email, passwordHash, role);
            var savedAccount = userAccountRepository.save(account);

            for (var event : account.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            account.clearDomainEvents();

            return Result.success(savedAccount.snapshot().id().userId());
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    @Override
    public Result<String, ApplicationError> handle(ChangeUserPasswordCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "user.command.null"));
        }
        try {
            var userId = new UserId(command.userId());
            var optionalAccount = userAccountRepository.findById(userId);
            if (optionalAccount.isEmpty()) {
                return Result.failure(ApplicationError.notFound("user", command.userId()));
            }

            var account = optionalAccount.get();
            account.changePassword(new PasswordHash(command.newPasswordHash()));
            var savedAccount = userAccountRepository.save(account);

            for (var event : account.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            account.clearDomainEvents();

            return Result.success(savedAccount.snapshot().id().userId());
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    @Override
    public Result<Void, ApplicationError> handle(RequestPasswordResetCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "user.command.null"));
        }
        try {
            var email = new EmailAddress(command.email());
            var optionalAccount = userAccountRepository.findByEmail(email);

            if (optionalAccount.isPresent()) {
                var account = optionalAccount.get();
                account.requestPasswordReset();
                userAccountRepository.save(account);

                for (var event : account.domainEvents()) {
                    eventPublisher.publishEvent(event);
                }
                account.clearDomainEvents();
            }

            // Always respond success to prevent user enumeration
            return Result.success(null);
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    @Override
    public Result<Void, ApplicationError> handle(ResetUserPasswordCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "user.command.null"));
        }
        try {
            var optionalAccount = userAccountRepository.findByResetToken(command.resetToken().trim());
            if (optionalAccount.isEmpty()) {
                return Result.failure(ApplicationError.businessRuleViolation("password_reset", "user.password_reset.invalid_token"));
            }

            var account = optionalAccount.get();
            account.resetPassword(command.resetToken(), new PasswordHash(command.newPasswordHash()), Instant.now());
            userAccountRepository.save(account);

            for (var event : account.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            account.clearDomainEvents();

            return Result.success(null);
        } catch (BusinessRuleException ex) {
            return Result.failure(ApplicationError.businessRuleViolation("password_reset", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }
}
