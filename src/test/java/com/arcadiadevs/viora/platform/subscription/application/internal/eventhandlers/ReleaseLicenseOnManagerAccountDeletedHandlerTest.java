package com.arcadiadevs.viora.platform.subscription.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.iam.domain.model.events.UserAccountDeletedEvent;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicense;
import com.arcadiadevs.viora.platform.subscription.domain.model.aggregates.CooperativeLicenseSnapshot;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.CooperativeLicenseId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.HectaresQuota;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.ManagerId;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.SubscriptionPeriod;
import com.arcadiadevs.viora.platform.subscription.domain.repositories.CooperativeLicenseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("Releasing the license of a deleted manager")
class ReleaseLicenseOnManagerAccountDeletedHandlerTest {

    private final CooperativeLicenseRepository licenses = mock(CooperativeLicenseRepository.class);
    private final ReleaseLicenseOnManagerAccountDeletedHandler handler =
            new ReleaseLicenseOnManagerAccountDeletedHandler(licenses);

    @Test
    @DisplayName("The license of the deleted manager is left without a manager and stored")
    void releasesTheLicense() {
        var manager = new ManagerId();
        var license = licenseOf(manager);
        when(licenses.findByManagerIdForUpdate(manager)).thenReturn(Optional.of(license));

        handler.on(new UserAccountDeletedEvent(manager.managerId(), "gone@viora.ag", Instant.now()));

        assertThat(license.snapshot().managerId()).isNull();
        verify(licenses).save(license);
    }

    @Test
    @DisplayName("An account in charge of no license changes nothing")
    void ignoresOtherAccounts() {
        when(licenses.findByManagerIdForUpdate(any())).thenReturn(Optional.empty());

        handler.on(new UserAccountDeletedEvent(new ManagerId().managerId(), "producer@viora.ag", Instant.now()));

        verify(licenses, never()).save(any());
    }

    private static CooperativeLicense licenseOf(ManagerId manager) {
        var now = Instant.now();
        return CooperativeLicense.reconstitute(new CooperativeLicenseSnapshot(new CooperativeLicenseId(),
                new CooperativeId(), "Cooperativa Olivar del Sur", 60, new HectaresQuota(new BigDecimal("200")),
                BigDecimal.ZERO, new SubscriptionPeriod(now.minus(Duration.ofDays(1)), now.plus(Duration.ofDays(365))),
                manager, 0L));
    }
}
