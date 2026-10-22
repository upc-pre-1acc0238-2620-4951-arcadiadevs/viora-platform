package com.arcadiadevs.viora.platform.subscription.domain;

import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.PaymentAttempt;
import static org.junit.jupiter.api.Assertions.*;

class PaymentAttemptDomainTest {
    private final Instant now = Instant.parse("2026-10-21T12:00:00Z");

    private PaymentAttempt attempt() {
        return new PaymentAttempt(UUID.randomUUID(), UUID.randomUUID(), "fixture", "OPEN",
                "preference", "https://example.test/checkout", now, now.plusSeconds(86400));
    }

    @Test
    void unrelatedRejectionCannotHidePendingAndApprovalCannotRegress() {
        var pending = attempt().withPaymentState("123", "pending").withPaymentState("124", "rejected");
        assertEquals("PENDING", pending.status());
        var approved = pending.withPaymentState("123", "approved").withPaymentState("123", "rejected");
        assertEquals("APPROVED", approved.status());
        assertEquals("approved", approved.paymentStates().get("123"));
    }

    @Test
    void observationsAreImmutable() {
        var original = new HashMap<String, String>();
        original.put("123", "pending");
        var a = new PaymentAttempt(UUID.randomUUID(), UUID.randomUUID(), "fixture", "PENDING",
                "preference", "https://example.test/checkout", now, now.plusSeconds(60), original);
        original.clear();
        assertEquals("pending", a.paymentStates().get("123"));
        assertThrows(UnsupportedOperationException.class, () -> a.paymentStates().put("123", "approved"));
    }

    @Test
    void rejectsInvalidLifetimeAndUnknownState() {
        assertThrows(IllegalArgumentException.class, () -> new PaymentAttempt(UUID.randomUUID(), UUID.randomUUID(),
                "fixture", "OPEN", null, null, now, now));
        assertThrows(IllegalArgumentException.class, () -> attempt().withStatus("UNKNOWN"));
    }

    @Test
    void linkExpirationCannotDiscardPendingOrApprovedPaymentEvidence() {
        var pending = attempt().withPaymentState("123", "pending");
        assertThrows(IllegalStateException.class, () -> pending.withStatus("EXPIRED"));
        var approved = pending.withPaymentState("123", "approved");
        assertThrows(IllegalStateException.class, () -> approved.withStatus("EXPIRED"));
        assertEquals("EXPIRED", attempt().withStatus("EXPIRED").status());
    }
}
