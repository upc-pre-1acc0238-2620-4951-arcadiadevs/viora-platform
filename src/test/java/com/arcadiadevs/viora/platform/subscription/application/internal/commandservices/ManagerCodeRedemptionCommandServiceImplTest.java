package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.ratelimit.RedemptionAttemptLimiter;
import com.arcadiadevs.viora.platform.subscription.application.ports.SubscriptionUnitOfWork;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemManagerCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.ManagerActivationCodeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Manager code redemption command service unit tests")
class ManagerCodeRedemptionCommandServiceImplTest {

    private final ManagerActivationCodeRepository codes = mock(ManagerActivationCodeRepository.class);
    private final CooperativeLicenseRepository licenses = mock(CooperativeLicenseRepository.class);
    private final ExternalIamService iam = mock(ExternalIamService.class);
    private final RedemptionAttemptLimiter limiter = mock(RedemptionAttemptLimiter.class);
    private final ManagerCodeRedemptionCommandServiceImpl service = new ManagerCodeRedemptionCommandServiceImpl(
            codes, licenses, iam, limiter, Clock.fixed(Instant.parse("2026-11-05T15:00:00Z"), ZoneOffset.UTC),
            new SubscriptionUnitOfWork() {
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

    @Test
    @DisplayName("A user IAM knows with another role than technical manager gets 403 and no code is looked up")
    void onlyManagersRedeem() {
        when(iam.isTechnicalManager(any())).thenReturn(false);

        var result = service.handle(new RedeemManagerCodeCommand(UUID.randomUUID().toString(), "GEST-7K2Q"));

        assertThat(result.failure()).hasValueSatisfying(error ->
                assertThat(error.code()).isEqualTo("MANAGER_CODE_REDEMPTION_FORBIDDEN"));
        verifyNoInteractions(codes, licenses, limiter);
    }

    @Test
    @DisplayName("A null command or an invalid manager is a validation error")
    void invalidRequests() {
        assertThat(service.handle(null).failure()).hasValueSatisfying(error ->
                assertThat(error.code()).isEqualTo("VALIDATION_ERROR"));
        assertThat(service.handle(new RedeemManagerCodeCommand("not-a-uuid", "GEST-7K2Q")).failure())
                .hasValueSatisfying(error -> assertThat(error.code()).isEqualTo("VALIDATION_ERROR"));
    }
}
