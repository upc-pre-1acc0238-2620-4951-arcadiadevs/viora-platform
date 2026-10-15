package com.arcadiadevs.viora.platform.iam.application.internal.commandservices;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.codes.PasswordResetCodeGenerator;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ChangeUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RequestPasswordResetCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResetUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.PasswordResetCodeHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.OnboardingProfileDetails;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordResetCode;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

/**
 * Application service implementation orchestrating mutating commands on user accounts.
 */
@Service
@Transactional
public class UserAccountCommandServiceImpl implements UserAccountCommandService {

    private final UserAccountRepository userAccountRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PasswordResetCodeGenerator resetCodeGenerator;
    private final PasswordResetCodeHasher resetCodeHasher;
    private final TransactionalEmailService emailService;

    /**
     * Creates the command service.
     *
     * @param userAccountRepository the user account repository port
     * @param eventPublisher        publisher of the domain events raised by the aggregate
     * @param resetCodeGenerator    generator of password recovery codes
     * @param resetCodeHasher       hasher of password recovery codes
     * @param emailService          transactional email port delivering recovery codes
     * @throws IllegalArgumentException if any dependency is null
     */
    public UserAccountCommandServiceImpl(
            UserAccountRepository userAccountRepository,
            ApplicationEventPublisher eventPublisher,
            PasswordResetCodeGenerator resetCodeGenerator,
            PasswordResetCodeHasher resetCodeHasher,
            TransactionalEmailService emailService
    ) {
        if (userAccountRepository == null) {
            throw new IllegalArgumentException("user.repository.null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("user.event_publisher.null");
        }
        if (resetCodeGenerator == null) {
            throw new IllegalArgumentException("user.reset_code_generator.null");
        }
        if (resetCodeHasher == null) {
            throw new IllegalArgumentException("user.reset_code_hasher.null");
        }
        if (emailService == null) {
            throw new IllegalArgumentException("email.service.null");
        }
        this.userAccountRepository = userAccountRepository;
        this.eventPublisher = eventPublisher;
        this.resetCodeGenerator = resetCodeGenerator;
        this.resetCodeHasher = resetCodeHasher;
        this.emailService = emailService;
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

            var profileDetails = command.hasProfileDetails()
                    ? new OnboardingProfileDetails(command.fullName(), command.country(), command.phoneNumber())
                    : null;
            var account = UserAccount.register(email, passwordHash, role, profileDetails);
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
                var code = resetCodeGenerator.generate();
                account.requestPasswordReset(resetCodeHasher.hash(code), Instant.now());
                userAccountRepository.save(account);

                for (var event : account.domainEvents()) {
                    eventPublisher.publishEvent(event);
                }
                account.clearDomainEvents();

                sendResetCodeAfterCommit(email.value(), code);
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
            var optionalAccount = userAccountRepository.findByEmail(new EmailAddress(command.email()));
            if (optionalAccount.isEmpty()) {
                // Same answer as a wrong code, so the endpoint does not reveal which emails are registered
                return Result.failure(ApplicationError.businessRuleViolation("password_reset", "user.password_reset.invalid_token"));
            }

            var account = optionalAccount.get();
            try {
                account.resetPassword(new PasswordResetCode(command.resetToken()), resetCodeHasher,
                        new PasswordHash(command.newPasswordHash()), Instant.now());
            } catch (BusinessRuleException ex) {
                // A wrong code is counted on the aggregate, so it must be persisted even though the reset failed
                userAccountRepository.save(account);
                return Result.failure(ApplicationError.businessRuleViolation("password_reset", ex.getMessage()));
            }
            userAccountRepository.save(account);

            for (var event : account.domainEvents()) {
                eventPublisher.publishEvent(event);
            }
            account.clearDomainEvents();

            return Result.success(null);
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    /**
     * Delivers the plain code only once the transaction storing its hash commits, so a rolled back request never
     * emails a code that does not exist. Outside a transaction the code is sent right away.
     */
    private void sendResetCodeAfterCommit(String recipientEmail, PasswordResetCode code) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            emailService.sendPasswordResetEmail(recipientEmail, code.value());
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                emailService.sendPasswordResetEmail(recipientEmail, code.value());
            }
        });
    }
}
