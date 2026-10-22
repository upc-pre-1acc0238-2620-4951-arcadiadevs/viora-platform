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
        var auth = new MercadoPagoWebhookAuthenticator("test-secret");
        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        var hash = HexFormat.of().formatHex(mac.doFinal("id:123;request-id:req-1;ts:123456;".getBytes(StandardCharsets.UTF_8)));
        var signature = "ts=123456,v1=" + hash;
        assertTrue(auth.valid("123", "req-1", signature));
        assertFalse(auth.valid("124", "req-1", signature));
        assertFalse(auth.valid("123", "req-2", signature));
        assertFalse(auth.valid("123", "req-1", signature + ",ts=123456"));
        assertFalse(new MercadoPagoWebhookAuthenticator("").valid("123", "req-1", signature));
        assertFalse(auth.valid("123", "req-1", null));
    }
}
