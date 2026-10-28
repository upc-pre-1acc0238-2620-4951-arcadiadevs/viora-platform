package com.arcadiadevs.viora.platform.subscription.infrastructure;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import com.arcadiadevs.viora.platform.subscription.infrastructure.mercadopago.MercadoPagoWebhookAuthenticator;
import static org.junit.jupiter.api.Assertions.*;
class MercadoPagoWebhookAuthenticatorTest {
    @Test void verifiesSignatureAndRejectsTampering() throws Exception {
        var clock = java.time.Clock.fixed(java.time.Instant.ofEpochSecond(123456), java.time.ZoneOffset.UTC);
        var auth = new MercadoPagoWebhookAuthenticator("test-secret", clock);
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var hash = HexFormat.of().formatHex(mac.doFinal("id:123;request-id:req-1;ts:123456;".getBytes(StandardCharsets.UTF_8)));
        var signature = "ts=123456,v1=" + hash;
        assertTrue(auth.valid("123", "req-1", signature));
        assertFalse(auth.valid("124", "req-1", signature));
        assertFalse(auth.valid("123", "req-2", signature));
        assertFalse(auth.valid("123", "req-1", signature + ",ts=123456"));
        assertFalse(new MercadoPagoWebhookAuthenticator("", clock).valid("123", "req-1", signature));
        assertFalse(auth.valid("123", "req-1", null));
    }
    @Test void rejectsASignatureOutsideTheTimeWindowAndAcceptsMilliseconds() throws Exception {
        var now = java.time.Instant.parse("2026-10-28T12:00:00Z");
        var auth = new MercadoPagoWebhookAuthenticator("test-secret",
                java.time.Clock.fixed(now, java.time.ZoneOffset.UTC));
        assertTrue(auth.valid("123", "req-1", sign(Long.toString(now.minusSeconds(60).getEpochSecond()))));
        assertTrue(auth.valid("123", "req-1", sign(Long.toString(now.minusSeconds(60).toEpochMilli()))));
        assertFalse(auth.valid("123", "req-1", sign(Long.toString(now.minusSeconds(3600).getEpochSecond()))));
        assertFalse(auth.valid("123", "req-1", sign("123")));
    }
    private static String sign(String ts) throws Exception {
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "ts=" + ts + ",v1=" + HexFormat.of().formatHex(
                mac.doFinal(("id:123;request-id:req-1;ts:" + ts + ";").getBytes(StandardCharsets.UTF_8)));
    }
}
