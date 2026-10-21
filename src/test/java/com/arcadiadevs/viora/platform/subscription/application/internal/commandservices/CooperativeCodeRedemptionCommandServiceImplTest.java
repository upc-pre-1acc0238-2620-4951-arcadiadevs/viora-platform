package com.arcadiadevs.viora.platform.subscription.application.internal.commandservices;

import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.ratelimit.RedemptionAttemptLimiter;
import com.arcadiadevs.viora.platform.subscription.domain.model.commands.RedeemCooperativeCodeCommand;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DisplayName("Cooperative code redemption command service unit tests")
class CooperativeCodeRedemptionCommandServiceImplTest {

    private final InvitationCodeRepository codes = mock(InvitationCodeRepository.class);
    private final CooperativeLicenseRepository licenses = mock(CooperativeLicenseRepository.class);
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final ExternalIamService iam = mock(ExternalIamService.class);
    private final RedemptionAttemptLimiter limiter = mock(RedemptionAttemptLimiter.class);
    private final CooperativeCodeRedemptionCommandServiceImpl service = new CooperativeCodeRedemptionCommandServiceImpl(
            codes, licenses, subscriptions, iam, limiter, mock(ApplicationEventPublisher.class),
            Clock.fixed(Instant.parse("2026-10-20T15:00:00Z"), ZoneOffset.UTC));

    @Test
    @DisplayName("A user IAM knows with another role than producer gets 403 and no code is looked up")
    void onlyProducersRedeem() {
        when(iam.mayRedeemCooperativeCodes(any())).thenReturn(false);

        var result = service.handle(new RedeemCooperativeCodeCommand(UUID.randomUUID().toString(), "COOP-7K2Q"));

        assertThat(result.failure()).hasValueSatisfying(error ->
                assertThat(error.code()).isEqualTo("COOPERATIVE_CODE_REDEMPTION_FORBIDDEN"));
        verifyNoInteractions(codes, licenses, subscriptions, limiter);
    }

    @Test
    @DisplayName("A null command is a validation error")
    void nullCommand() {
        assertThat(service.handle(null).failure()).hasValueSatisfying(error ->
                assertThat(error.code()).isEqualTo("VALIDATION_ERROR"));
    }
}
