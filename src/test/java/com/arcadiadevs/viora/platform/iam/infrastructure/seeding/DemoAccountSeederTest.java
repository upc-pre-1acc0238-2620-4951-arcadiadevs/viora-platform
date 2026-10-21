package com.arcadiadevs.viora.platform.iam.infrastructure.seeding;

import com.arcadiadevs.viora.platform.iam.application.internal.outboundservices.hashing.HashingService;
import com.arcadiadevs.viora.platform.iam.domain.model.aggregates.UserAccount;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserAccountStatus;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserId;
import com.arcadiadevs.viora.platform.iam.domain.model.valueobjects.UserRole;
import com.arcadiadevs.viora.platform.iam.domain.repositories.UserAccountRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("DemoAccountSeeder Unit Tests")
class DemoAccountSeederTest {

    private static final String DEMO_PRODUCER = "550e8400-e29b-41d4-a716-446655440000";

    private final UserAccountRepository repository = mock(UserAccountRepository.class);
    private final HashingService hashing = mock(HashingService.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-21T15:00:00Z"), ZoneOffset.UTC);

    private DemoAccountSeeder seeder(String email, String password) {
        return new DemoAccountSeeder(repository, hashing, clock, DEMO_PRODUCER, email, password, "2026-10");
    }

    @Test
    @DisplayName("Creates a verified producer account on the demo producer, with the hashed password")
    void createsDemoAccount() {
        when(repository.findById(any())).thenReturn(Optional.empty());
        when(hashing.encode("Clave-Demo-1")).thenReturn("hashed");

        seeder(" Demo@Viora.pe ", "Clave-Demo-1").run(null);

        var saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(repository).save(saved.capture());
        var snapshot = saved.getValue().snapshot();
        assertThat(snapshot.id()).isEqualTo(new UserId(DEMO_PRODUCER));
        assertThat(snapshot.email().value()).isEqualTo("demo@viora.pe");
        assertThat(snapshot.passwordHash().value()).isEqualTo("hashed");
        assertThat(snapshot.status()).isEqualTo(UserAccountStatus.ACTIVE);
        assertThat(snapshot.role()).isEqualTo(UserRole.ROLE_PRODUCER);
        assertThat(snapshot.termsAcceptance().version()).isEqualTo("2026-10");
    }

    @Test
    @DisplayName("Does nothing until both the email and the password are configured")
    void disabledWithoutCredentials() {
        seeder("", "Clave-Demo-1").run(null);
        seeder("demo@viora.pe", " ").run(null);

        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("Never changes an existing demo account or takes an email in use")
    void keepsExistingAccounts() {
        when(repository.findById(new UserId(DEMO_PRODUCER))).thenReturn(Optional.of(mock(UserAccount.class)));
        seeder("demo@viora.pe", "Clave-Demo-1").run(null);

        when(repository.findById(any())).thenReturn(Optional.empty());
        when(repository.existsByEmail(any())).thenReturn(true);
        seeder("demo@viora.pe", "Clave-Demo-1").run(null);

        verify(repository, never()).save(any());
    }
}
