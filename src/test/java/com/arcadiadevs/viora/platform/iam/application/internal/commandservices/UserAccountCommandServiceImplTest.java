package com.arcadiadevs.viora.platform.iam.application.internal.commandservices;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.codes.PasswordResetCodeGenerator;
import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.email.TransactionalEmailService;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RegisterUserAccountCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.RequestPasswordResetCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.commands.ResetUserPasswordCommand;
import com.arcadiadevs.viora.platform.iam.domain.model.events.PasswordResetRequestedEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.events.UserSignedUpWithProfileEvent;
import com.arcadiadevs.viora.platform.iam.domain.model.ports.PasswordResetCodeHasher;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.EmailAddress;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordHash;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.PasswordResetCode;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import com.arcadiadevs.viora.platform.shared.application.result.ApplicationError;
import com.arcadiadevs.viora.platform.shared.application.result.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserAccountCommandService password reset and sign-up Application Unit Tests")
class UserAccountCommandServiceImplTest {

    private static final String EMAIL = "productor@viora.ag";
    private static final String NEW_HASH = "$argon2id$v=19$m=65536,t=3,p=4$newhash";
    private static final PasswordResetCode CODE = new PasswordResetCode("482913");

    /** Deterministic stand-in for the Argon2 adapter. */
    private static final PasswordResetCodeHasher HASHER = new PasswordResetCodeHasher() {
        @Override
        public String hash(PasswordResetCode code) {
            return "hashed:" + code.value();
        }

        @Override
        public boolean matches(PasswordResetCode code, String codeHash) {
            return hash(code).equals(codeHash);
        }
    };

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private TransactionalEmailService emailService;

    private UserAccountCommandServiceImpl service;

    @BeforeEach
    void setUp() {
        PasswordResetCodeGenerator fixedGenerator = () -> CODE;
        service = new UserAccountCommandServiceImpl(userAccountRepository, eventPublisher, fixedGenerator, HASHER, emailService);
    }

    @Test
    @DisplayName("Stores only the code hash, publishes the domain event and emails the plain code")
    void requestStoresHashAndEmailsCode() {
        var account = registeredAccount();
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(account));

        var result = service.handle(new RequestPasswordResetCommand(EMAIL));

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(account.snapshot().resetToken().codeHash()).isEqualTo("hashed:482913");
        verify(eventPublisher).publishEvent(any(PasswordResetRequestedEvent.class));
        // No transaction is active in a unit test, so the code is sent right away
        verify(emailService).sendPasswordResetEmail(EMAIL, "482913");
    }

    @Test
    @DisplayName("Answers an unknown email exactly like a wrong code")
    void unknownEmailLooksLikeWrongCode() {
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.empty());

        var result = service.handle(new ResetUserPasswordCommand(EMAIL, "123456", NEW_HASH));

        assertThat(result).isInstanceOf(Result.Failure.class);
        assertThat(((Result.Failure<Void, ApplicationError>) result).error().details())
                .isEqualTo("user.password_reset.invalid_token");
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Persists the failed attempt when the code is wrong")
    void wrongCodeIsPersisted() {
        var account = accountWithActiveCode();
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(account));

        var result = service.handle(new ResetUserPasswordCommand(EMAIL, "000000", NEW_HASH));

        assertThat(result).isInstanceOf(Result.Failure.class);
        verify(userAccountRepository).save(account);
        assertThat(account.snapshot().resetToken().failedAttempts()).isEqualTo(1);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("Resets the password with the right code for the right email")
    void rightCodeResetsPassword() {
        var account = accountWithActiveCode();
        when(userAccountRepository.findByEmail(any(EmailAddress.class))).thenReturn(Optional.of(account));

        var result = service.handle(new ResetUserPasswordCommand(EMAIL, CODE.value(), NEW_HASH));

        assertThat(result).isInstanceOf(Result.Success.class);
        assertThat(account.snapshot().passwordHash().value()).isEqualTo(NEW_HASH);
        assertThat(account.snapshot().resetToken()).isNull();
        verify(eventPublisher).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("Publishes UserSignedUpWithProfileEvent when sign-up carries profile details")
    void signUpWithProfilePublishesDomainEvent() {
        when(userAccountRepository.existsByEmail(any(EmailAddress.class))).thenReturn(false);
        when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.handle(new RegisterUserAccountCommand(
                EMAIL, NEW_HASH, "ROLE_PRODUCER", "Juan Perez", "PE", "+51987654321"));

        assertThat(result).isInstanceOf(Result.Success.class);
        var captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, org.mockito.Mockito.times(2)).publishEvent(captor.capture());
        assertThat(captor.getAllValues()).anyMatch(UserSignedUpWithProfileEvent.class::isInstance);
    }

    private static UserAccount registeredAccount() {
        var account = UserAccount.register(new EmailAddress(EMAIL),
                new PasswordHash("$argon2id$v=19$m=65536,t=3,p=4$oldhash"), UserRole.ROLE_PRODUCER);
        account.clearDomainEvents();
        return account;
    }

    private static UserAccount accountWithActiveCode() {
        var account = registeredAccount();
        account.requestPasswordReset(HASHER.hash(CODE), Instant.now());
        account.clearDomainEvents();
        return account;
    }
}
