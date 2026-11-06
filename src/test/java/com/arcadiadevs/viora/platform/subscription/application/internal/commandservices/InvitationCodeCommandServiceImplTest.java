package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.subscription.application.ports.InvitationCodeCipher;
import com.arcadiadevs.viora.platform.subscription.application.ports.InvitationCodeGenerator;
import com.arcadiadevs.viora.platform.subscription.application.ports.SubscriptionUnitOfWork;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.IssueInvitationCodesCommand;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayDeque;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Invitation code command service unit tests")
class InvitationCodeCommandServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-06T15:00:00Z");

    private final InvitationCodeRepository codes = mock(InvitationCodeRepository.class);
    private final CooperativeLicenseRepository licenses = mock(CooperativeLicenseRepository.class);
    private final ExternalIamService iam = mock(ExternalIamService.class);
    private final InvitationCodeCipher cipher = mock(InvitationCodeCipher.class);
    private final ArrayDeque<String> draws = new ArrayDeque<>();
    private final InvitationCodeGenerator generator = draws::removeFirst;
    private final InvitationCodeCommandServiceImpl service = new InvitationCodeCommandServiceImpl(codes, licenses, iam,
            generator, cipher, Clock.fixed(NOW, ZoneOffset.UTC), new SubscriptionUnitOfWork() {
                @Override
                public <T> T exclusive(String producerId, Supplier<T> work) {
                    return work.get();
                }

                @Override
                public Optional<Operation> findOperation(String producerId, String key) {
                    return Optional.empty();
                }

                @Override
                public void recordOperation(String producerId, String key, String kind, String fingerprint,
                                            UUID resourceId) {
                }
            });
    private final String manager = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        when(iam.isTechnicalManager(any())).thenReturn(true);
        when(cipher.isAvailable()).thenReturn(true);
        when(cipher.encrypt(any())).thenAnswer(call -> "sealed:" + call.getArgument(0));
        when(licenses.findByManagerIdForUpdate(any())).thenReturn(Optional.of(CooperativeLicense.grant(
                new CooperativeId(), "Cooperativa Olivar del Sur", 60, new HectaresQuota(new BigDecimal("200")),
                new SubscriptionPeriod(NOW.minus(Duration.ofDays(1)), NOW.plus(Duration.ofDays(365))))));
        when(codes.findOverdueByLicenseId(any(), any())).thenReturn(List.of());
    }

    @Test
    @DisplayName("A drawn code already issued in any license, or repeated in the batch, is drawn again")
    void drawsAgainOnACollision() {
        when(codes.existsByFingerprint(new CooperativeCode("COOP-AAAA").fingerprint())).thenReturn(true);
        draws.addAll(List.of("COOP-AAAA", "COOP-BBBB", "COOP-BBBB", "COOP-CCCC"));

        var batch = service.handle(command(2)).success().orElseThrow();

        assertThat(batch.codes()).extracting(code -> code.code()).containsExactly("COOP-BBBB", "COOP-CCCC");
        assertThat(draws).isEmpty();
    }

    @Test
    @DisplayName("When every draw hits an issued code the batch is refused as unavailable, not looped forever")
    void stopsDrawing() {
        when(codes.existsByFingerprint(any())).thenReturn(true);
        for (var draw = 0; draw < 20; draw++) {
            draws.add("COOP-AAAA");
        }

        var error = service.handle(command(1)).failure().orElseThrow();

        assertThat(error.details()).isEqualTo("subscription.invitation_code.generation.exhausted");
        assertThat(draws).isEmpty();
    }

    @Test
    @DisplayName("Without an encryption key no code is drawn")
    void needsTheKey() {
        when(cipher.isAvailable()).thenReturn(false);

        var error = service.handle(command(1)).failure().orElseThrow();

        assertThat(error.details()).isEqualTo("subscription.invitation_code.cipher.unavailable");
        verifyNoInteractions(licenses, codes);
    }

    @Test
    @DisplayName("Only a technical manager issues codes")
    void onlyManagers() {
        when(iam.isTechnicalManager(any())).thenReturn(false);

        var error = service.handle(command(1)).failure().orElseThrow();

        assertThat(error.code()).isEqualTo("INVITATION_CODES_FORBIDDEN");
        verifyNoInteractions(licenses, codes);
    }

    private IssueInvitationCodesCommand command(int quantity) {
        return new IssueInvitationCodesCommand(manager, quantity, new BigDecimal("5"), 30);
    }
}
