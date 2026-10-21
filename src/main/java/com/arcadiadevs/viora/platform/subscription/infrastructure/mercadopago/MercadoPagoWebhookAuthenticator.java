package com.arcadiadevs.viora.platform.subscription.infrastructure.mercadopago;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
/** Verifies the provider signature over the canonical data identifier, request identifier and timestamp. */
@Component
public class MercadoPagoWebhookAuthenticator implements com.arcadiadevs.viora.platform.subscription.application.ports.PaymentNotificationAuthenticator {
    private final String secret;
    /** @param secret configured provider signing secret */
    public MercadoPagoWebhookAuthenticator(@Value("${viora.payments.webhook-secret}") String secret) { this.secret = secret; }
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
            if (ts == null || !ts.matches("[0-9]{1,20}") || hash == null || !hash.matches("[a-fA-F0-9]{64}")) { return false; }
            var mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            var manifest = "id:" + dataId + ";request-id:" + requestId + ";ts:" + ts + ";";
            return MessageDigest.isEqual(mac.doFinal(manifest.getBytes(StandardCharsets.UTF_8)), HexFormat.of().parseHex(hash));
        } catch (Exception ex) { return false; }
    }
}
