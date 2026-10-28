package com.arcadiadevs.viora.platform.subscription.infrastructure.mercadopago;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
/**
 * Verifies the provider signature over the canonical data identifier, request identifier and timestamp, and that the
 * timestamp is recent, so a captured notification cannot be replayed forever.
 */
@Component
public class MercadoPagoWebhookAuthenticator implements com.arcadiadevs.viora.platform.subscription.application.ports.PaymentNotificationAuthenticator {
    /** How far the signed timestamp may be from now; the provider signs each delivery, retries included. */
    static final Duration TOLERANCE = Duration.ofMinutes(15);
    private final String secret;
    private final Clock clock;
    /** @param secret configured provider signing secret @param clock application clock */
    @Autowired
    public MercadoPagoWebhookAuthenticator(@Value("${viora.payments.webhook-secret}") String secret, Clock clock) {
        this.secret = secret;
        this.clock = clock;
    }
    /** @return whether notification authentication can be performed */
    public boolean configured() { return !secret.isBlank(); }
    /** @param dataId payment identifier @param requestId provider request @param signature signed header @return signature validity */
    public boolean valid(String dataId, String requestId, String signature) {
        if (!configured() || dataId == null || !dataId.matches("[0-9]{1,30}") || requestId == null
                || !requestId.matches("[a-zA-Z0-9-]{1,128}") || signature == null || signature.length() > 512) { return false; }
        try {
            var parts = new HashMap<String, String>();
            for (var part : signature.split(",")) {
                var pair = part.trim().split("=", 2);
                if (pair.length == 2 && parts.put(pair[0], pair[1]) != null) { return false; }
            }
            var ts = parts.get("ts"); var hash = parts.get("v1");
            if (ts == null || !ts.matches("[0-9]{1,15}") || hash == null || !hash.matches("[a-fA-F0-9]{64}")) { return false; }
            if (!isRecent(Long.parseLong(ts))) { return false; }
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
            return MessageDigest.isEqual(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)), HexFormat.of().parseHex(hash));
        } catch (Exception ex) { return false; }
    }
    /** The provider sends seconds or milliseconds since the epoch; both are accepted. */
    private boolean isRecent(long ts) {
        var signedAt = ts >= 100_000_000_000L ? Instant.ofEpochMilli(ts) : Instant.ofEpochSecond(ts);
        return Duration.between(signedAt, clock.instant()).abs().compareTo(TOLERANCE) <= 0;
    }
}
