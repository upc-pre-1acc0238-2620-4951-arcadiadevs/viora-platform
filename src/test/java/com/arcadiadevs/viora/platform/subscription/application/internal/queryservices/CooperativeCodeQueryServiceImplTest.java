package com.arcadiadevs.viora.platform.subscription.application.internal.queryservices;

import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalIamService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.acl.ExternalProfileService;
import com.arcadiadevs.viora.platform.subscription.application.internal.outboundservices.ratelimit.RedemptionAttemptLimiter;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicenseSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.InvitationCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.queries.GetCooperativeCodePreviewQuery;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeCode;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.InvitationCodeRepository;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.OptionalLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Cooperative code preview query service unit tests")
class CooperativeCodeQueryServiceImplTest {

    private static final Instant NOW = Instant.parse("2026-11-06T15:00:00Z");
    private static final Instant LICENSE_END = Instant.parse("2027-03-31T00:00:00Z");
    private static final String CODE = "COOP-7K2Q";

    private final InvitationCodeRepository codes = mock(InvitationCodeRepository.class);
    private final CooperativeLicenseRepository licenses = mock(CooperativeLicenseRepository.class);
    private final SubscriptionRepository subscriptions = mock(SubscriptionRepository.class);
    private final ExternalIamService iam = mock(ExternalIamService.class);
    private final ExternalProfileService profiles = mock(ExternalProfileService.class);
    private final ExternalOrchardService orchards = mock(ExternalOrchardService.class);
    private final RedemptionAttemptLimiter limiter = mock(RedemptionAttemptLimiter.class);
    private final CooperativeCodeQueryServiceImpl service = new CooperativeCodeQueryServiceImpl(codes, licenses,
            subscriptions, iam, profiles, orchards, limiter, Clock.fixed(NOW, ZoneOffset.UTC), "2026-11-v1");

    private final ProducerId producer = new ProducerId();
    private final ManagerId manager = new ManagerId();
    private CooperativeLicense license;
    private InvitationCode code;

    @BeforeEach
    void setUp() {
        license = CooperativeLicense.reconstitute(new CooperativeLicenseSnapshot(new CooperativeLicenseId(),
                new CooperativeId(), "Coop. Olivarera La Yarada", 60, new HectaresQuota(new BigDecimal("200")),
                BigDecimal.ZERO, new SubscriptionPeriod(NOW.minus(Duration.ofDays(30)), LICENSE_END), manager, 0L));
        code = license.issueCode(new CooperativeCode(CODE).fingerprint(), new HectaresQuota(new BigDecimal("12")),
                NOW.plus(Duration.ofDays(7)));
        when(iam.mayRedeemCooperativeCodes(any())).thenReturn(true);
        when(limiter.tryAcquire(any(ProducerId.class), any())).thenReturn(OptionalLong.empty());
        when(subscriptions.existsActiveByProducerId(any(), any())).thenReturn(false);
        when(codes.findByFingerprint(new CooperativeCode(CODE).fingerprint())).thenReturn(Optional.of(code));
        when(licenses.findById(license.snapshot().id())).thenReturn(Optional.of(license));
        when(profiles.findFullName(manager)).thenReturn(Optional.of("Rubén Ticona"));
    }

    @Test
    @DisplayName("A good code shows the organization, the manager, the quota, the end, the hectares and the text")
    void previewsAGoodCode() {
        when(orchards.registeredAreaHa(producer)).thenReturn(new BigDecimal("8.50"));

        var preview = service.handle(new GetCooperativeCodePreviewQuery(producer, CODE)).success().orElseThrow();

        assertThat(preview.organizationName()).isEqualTo("Coop. Olivarera La Yarada");
        assertThat(preview.managerName()).isEqualTo("Rubén Ticona");
        assertThat(preview.quotaHa()).isEqualByComparingTo("12");
        assertThat(preview.planEndsAt()).isEqualTo(LICENSE_END);
        assertThat(preview.registeredAreaHa()).isEqualByComparingTo("8.50");
        assertThat(preview.exceedsQuota()).isFalse();
        assertThat(preview.consentTextVersion()).isEqualTo("2026-11-v1");
        verify(limiter).release(producer, NOW);
    }

    @Test
    @DisplayName("More registered hectares than the quota is flagged, not refused")
    void flagsHectaresOverTheQuota() {
        when(orchards.registeredAreaHa(producer)).thenReturn(new BigDecimal("12.01"));

        var preview = service.handle(new GetCooperativeCodePreviewQuery(producer, CODE)).success().orElseThrow();

        assertThat(preview.exceedsQuota()).isTrue();
    }

    @Test
    @DisplayName("A code that cannot be redeemed keeps the failed attempt and carries the reason")
    void unknownCodeUsesAnAttempt() {
        when(codes.findByFingerprint(any())).thenReturn(Optional.empty());

        var error = service.handle(new GetCooperativeCodePreviewQuery(producer, "COOP-ZZZZ")).failure().orElseThrow();

        assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
        assertThat(error.properties()).containsEntry("reason", "subscription.invitation_code.not_redeemable");
        verify(limiter, never()).release(any(ProducerId.class), any());
    }

    @Test
    @DisplayName("A producer with an active plan gets the 409 with its reason, and the attempt is given back")
    void producerAlreadyActive() {
        when(subscriptions.existsActiveByProducerId(any(), any())).thenReturn(true);

        var error = service.handle(new GetCooperativeCodePreviewQuery(producer, CODE)).failure().orElseThrow();

        assertThat(error.properties()).containsEntry("reason", "subscription.producer.already_active");
        verify(limiter).release(producer, NOW);
    }

    @Test
    @DisplayName("A user who is not a producer gets 403 without using an attempt")
    void onlyProducers() {
        when(iam.mayRedeemCooperativeCodes(any())).thenReturn(false);

        var error = service.handle(new GetCooperativeCodePreviewQuery(producer, CODE)).failure().orElseThrow();

        assertThat(error.code()).isEqualTo("COOPERATIVE_CODE_PREVIEW_FORBIDDEN");
        verify(limiter, never()).tryAcquire(any(ProducerId.class), any());
    }
}
