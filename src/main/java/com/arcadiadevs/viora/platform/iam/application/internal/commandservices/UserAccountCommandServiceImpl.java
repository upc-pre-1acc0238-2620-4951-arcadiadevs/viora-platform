package com.arcadiadevs.viora.platform.iam.application.internal.commandservices;

import com.arcadiadevs.viora.platform.iam.application.commandservices.UserAccountCommandService;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.codes.VerificationCodeGenerator;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.tokens.SecureTokenGenerator;
import com.arcadiadevs.viora.platform.iam.domain.exceptions.EmailVerificationCooldownException;
import com.arcadiadevs.viora.platform.iam.domain.exceptions.InvalidEmailVerificationCodeException;
import com.arcadiadevs.viora.platform.iam.domain.exceptions.WeakPasswordException;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ChangeUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RequestPasswordResetCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResendEmailVerificationCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResetUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.VerifyEmailCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.PasswordHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.VerificationCodeHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.NewPassword;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.OnboardingProfileDetails;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.TermsAcceptance;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserAccountStatus;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.VerificationCode;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import com.arcadiadevs.viora.platform.shared.domain.model.exceptions.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Application service implementation orchestrating mutating commands on user accounts.
 */
@Service
@Transactional
public class UserAccountCommandServiceImpl implements UserAccountCommandService {

    private final UserAccountRepository userAccountRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final VerificationCodeGenerator codeGenerator;
    private final VerificationCodeHasher codeHasher;
    private final PasswordHasher passwordHasher;
    private final SecureTokenGenerator tokenGenerator;
    private final TransactionalEmailService emailService;
    private final String termsVersion;

    /**
     * Creates the command service.
     *
     * @param userAccountRepository the user account repository port
     * @param eventPublisher        publisher of the domain events raised by the aggregate
     * @param codeGenerator         generator of email verification codes
     * @param codeHasher            hasher of those codes
     * @param passwordHasher        hasher of passwords
     * @param tokenGenerator        generator and hasher of the tokens carried by password reset links
     * @param emailService          transactional email port delivering the codes
     * @param termsVersion          version of the terms that sign-up records as accepted
     * @throws IllegalArgumentException if any dependency is null or the terms version is blank
     */
    public UserAccountCommandServiceImpl(
            UserAccountRepository userAccountRepository,
            ApplicationEventPublisher eventPublisher,
            VerificationCodeGenerator codeGenerator,
            VerificationCodeHasher codeHasher,
            PasswordHasher passwordHasher,
            SecureTokenGenerator tokenGenerator,
            TransactionalEmailService emailService,
            @Value("${viora.iam.terms-version}") String termsVersion
    ) {
        if (userAccountRepository == null) {
            throw new IllegalArgumentException("user.repository.null");
        }
        if (eventPublisher == null) {
            throw new IllegalArgumentException("user.event_publisher.null");
        }
        if (codeGenerator == null) {
            throw new IllegalArgumentException("user.verification_code_generator.null");
        }
        if (codeHasher == null) {
            throw new IllegalArgumentException("user.verification_code_hasher.null");
        }
        if (passwordHasher == null) {
            throw new IllegalArgumentException("user.password_hasher.null");
        }
        if (tokenGenerator == null) {
            throw new IllegalArgumentException("user.token_generator.null");
        }
        if (emailService == null) {
            throw new IllegalArgumentException("email.service.null");
        }
        if (termsVersion == null || termsVersion.isBlank()) {
            throw new IllegalArgumentException("user.terms.version.null_or_empty");
        }
        this.userAccountRepository = userAccountRepository;
        this.eventPublisher = eventPublisher;
        this.codeGenerator = codeGenerator;
        this.codeHasher = codeHasher;
        this.passwordHasher = passwordHasher;
        this.tokenGenerator = tokenGenerator;
        this.emailService = emailService;
        this.termsVersion = termsVersion.trim();
    }

    /**
     * Signs up a pending account and emails its verification code. Signing up again over a pending account that was
     * never verified reuses it, with a fresh code.
     *
     * @param command registration command
     * @return the account identifier, or a conflict when the email already belongs to an active account
     */
    @Override
    public Result<String, ApplicationError> handle(RegisterUserAccountCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "user.command.null"));
        }
        try {
            var email = new EmailAddress(command.email());
            var existingAccount = userAccountRepository.findByEmail(email);
            if (existingAccount.isPresent()
                    && existingAccount.get().snapshot().status() == UserAccountStatus.ACTIVE) {
                return Result.failure(ApplicationError.conflict("user", "user.email.already_exists"));
            }

            var now = Instant.now();
            var passwordHash = passwordHasher.hash(new NewPassword(command.password()));
            var role = UserRole.from(command.role());
            var termsAcceptance = command.termsAccepted() ? new TermsAcceptance(termsVersion, now) : null;
            var profileDetails = command.hasProfileDetails()
                    ? new OnboardingProfileDetails(command.fullName(), command.country(), command.phoneNumber())
                    : null;
            var code = codeGenerator.generate();
            var codeHash = codeHasher.hash(code);

            UserAccount account;
            if (existingAccount.isPresent()) {
                account = existingAccount.get();
                if (role != account.snapshot().role()) {
                    return Result.failure(ApplicationError.validationError("argument", "user.role.sign_up_not_allowed"));
                }
                try {
                    account.restartSignUp(passwordHash, termsAcceptance, profileDetails, codeHash, now);
                } catch (EmailVerificationCooldownException ex) {
                    return Result.failure(ApplicationError.tooManyRequests("email-verification", ex.getMessage(),
                            ex.retryAfterSeconds()));
                } catch (BusinessRuleException ex) {
                    return Result.failure(ApplicationError.conflict("user", ex.getMessage()));
                }
            } else {
                account = UserAccount.signUp(email, passwordHash, role, termsAcceptance, profileDetails, codeHash, now);
            }
            var savedAccount = userAccountRepository.save(account);
            publishDomainEvents(account);
            afterCommit(() -> emailService.sendEmailVerificationEmail(email.value(), code.value()));

            // Not caught here: a profile rejected by the Profiles context while the events are published rolls the
            // whole sign-up back, so no account is left without its profile
            return Result.success(savedAccount.snapshot().id().userId());
        } catch (WeakPasswordException ex) {
            return Result.failure(weakPasswordError(ex));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    /**
     * Verifies the email address of a pending account and activates it.
     *
     * @param command verification command
     * @return the activated account identifier, or why the code was rejected
     */
    @Override
    public Result<String, ApplicationError> handle(VerifyEmailCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "user.command.null"));
        }
        try {
            var code = new VerificationCode(command.code());
            var optionalAccount = userAccountRepository.findByEmail(new EmailAddress(command.email()));
            if (optionalAccount.isEmpty()) {
                return Result.failure(emailVerificationError("user.email_verification.no_active_code", Map.of()));
            }

            var account = optionalAccount.get();
            try {
                account.verifyEmail(code, codeHasher, Instant.now());
            } catch (InvalidEmailVerificationCodeException ex) {
                // The wrong code is counted on the aggregate, so it must be persisted even though the check failed
                userAccountRepository.save(account);
                return Result.failure(emailVerificationError(ex.getMessage(),
                        Map.of("remainingAttempts", ex.remainingAttempts())));
            } catch (BusinessRuleException ex) {
                return Result.failure(emailVerificationError(ex.getMessage(), Map.of()));
            }
            var savedAccount = userAccountRepository.save(account);
            publishDomainEvents(account);

            return Result.success(savedAccount.snapshot().id().userId());
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    /**
     * Emails a new verification code to a pending account. Unknown and already active emails get the same empty
     * success, so the endpoint does not reveal which addresses are pending.
     *
     * @param command resend command
     * @return empty success, or a too many requests error carrying the seconds to wait
     */
    @Override
    public Result<Void, ApplicationError> handle(ResendEmailVerificationCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "user.command.null"));
        }
        try {
            var email = new EmailAddress(command.email());
            var optionalAccount = userAccountRepository.findByEmail(email);
            if (optionalAccount.isEmpty()
                    || optionalAccount.get().snapshot().status() != UserAccountStatus.PENDING_VERIFICATION) {
                return Result.success(null);
            }

            var account = optionalAccount.get();
            var code = codeGenerator.generate();
            try {
                account.resendEmailVerification(codeHasher.hash(code), Instant.now());
            } catch (EmailVerificationCooldownException ex) {
                return Result.failure(ApplicationError.tooManyRequests("email-verification", ex.getMessage(),
                        ex.retryAfterSeconds()));
            }
            userAccountRepository.save(account);
            publishDomainEvents(account);
            afterCommit(() -> emailService.sendEmailVerificationEmail(email.value(), code.value()));

            return Result.success(null);
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
            var newPassword = new NewPassword(command.newPassword());
            try {
                account.changePassword(command.currentRawPassword(), newPassword, passwordHasher, Instant.now());
            } catch (BusinessRuleException ex) {
                return Result.failure(passwordRuleError(ex.getMessage()));
            }
            var savedAccount = userAccountRepository.save(account);
            publishDomainEvents(account);

            return Result.success(savedAccount.snapshot().id().userId());
        } catch (WeakPasswordException ex) {
            return Result.failure(weakPasswordError(ex));
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
                var resetToken = tokenGenerator.generate();
                try {
                    account.requestPasswordReset(tokenGenerator.hash(resetToken), Instant.now());
                } catch (BusinessRuleException ex) {
                    // A pending account cannot recover a password it never activated; it answers like any other
                    return Result.success(null);
                }
                userAccountRepository.save(account);
                publishDomainEvents(account);
                afterCommit(() -> emailService.sendPasswordResetEmail(email.value(), resetToken));
            }

            // Always respond success to prevent user enumeration
            return Result.success(null);
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    /**
     * Consumes a password reset link to set a new password. An unknown, expired or already used link gets the
     * same answer, so the client shows that the link is no longer valid.
     *
     * @param command reset command carrying the token of the link
     * @return empty success, or why the link or the new password was rejected
     */
    @Override
    public Result<Void, ApplicationError> handle(ResetUserPasswordCommand command) {
        if (command == null) {
            return Result.failure(ApplicationError.validationError("command", "user.command.null"));
        }
        try {
            var newPassword = new NewPassword(command.newPassword());
            var tokenHash = tokenGenerator.hash(command.resetToken().trim());
            var optionalAccount = userAccountRepository.findByResetTokenHash(tokenHash);
            if (optionalAccount.isEmpty()) {
                return Result.failure(expiredResetLinkError());
            }

            var account = optionalAccount.get();
            try {
                account.resetPassword(tokenHash, newPassword, passwordHasher, Instant.now());
            } catch (BusinessRuleException ex) {
                return Result.failure("user.password.reused".equals(ex.getMessage())
                        ? passwordRuleError(ex.getMessage())
                        : expiredResetLinkError());
            }
            userAccountRepository.save(account);
            publishDomainEvents(account);

            return Result.success(null);
        } catch (WeakPasswordException ex) {
            return Result.failure(weakPasswordError(ex));
        } catch (IllegalArgumentException ex) {
            return Result.failure(ApplicationError.validationError("argument", ex.getMessage()));
        }
    }

    private static ApplicationError expiredResetLinkError() {
        return ApplicationError.notFound("password_reset_token", "user.password_reset.link_expired");
    }

    private static ApplicationError weakPasswordError(WeakPasswordException ex) {
        return ApplicationError.validationError("password", ex.getMessage(), Map.of("violations", ex.violations()));
    }

    private static ApplicationError passwordRuleError(String reason) {
        return ApplicationError.businessRuleViolation("password", reason, Map.of("reason", reason));
    }

    private void publishDomainEvents(UserAccount account) {
        for (var event : account.domainEvents()) {
            eventPublisher.publishEvent(event);
        }
        account.clearDomainEvents();
    }

    private static ApplicationError emailVerificationError(String reason, Map<String, Object> extra) {
        var properties = new HashMap<String, Object>(extra);
        properties.put("reason", reason);
        return ApplicationError.businessRuleViolation("email_verification", reason, properties);
    }

    /**
     * Runs the email delivery only once the transaction storing the code hash commits, so a rolled back request
     * never emails a code that does not exist. Outside a transaction it runs right away.
     */
    private static void afterCommit(Runnable delivery) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            delivery.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                delivery.run();
            }
        });
    }
}
