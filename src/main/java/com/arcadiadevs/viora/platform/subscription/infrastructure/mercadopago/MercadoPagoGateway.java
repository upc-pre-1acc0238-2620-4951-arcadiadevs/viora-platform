package com.arcadiadevs.viora.platform.subscription.infrastructure.mercadopago;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import com.arcadiadevs.viora.platform.subscription.application.ports.PaymentGateway;
import com.arcadiadevs.viora.platform.subscription.domain.model.valueobjects.*;
/** Checkout Pro Preferences adapter. Disabled until explicitly configured; test mode is the default. */
@Component
public class MercadoPagoGateway implements PaymentGateway {
    private final RestClient client;
    private final boolean enabled;
    private final boolean sandbox;
    private final String token;
    private final String notificationUrl;
    private final String returnUrl;
    private record Merchant(String id, boolean testAccount) { }
    private volatile Merchant merchantIdentity;
    @org.springframework.beans.factory.annotation.Autowired
    public MercadoPagoGateway(@Value("${viora.payments.enabled:false}") boolean enabled,
            @Value("${viora.payments.sandbox:true}") boolean sandbox,
            @Value("${viora.payments.access-token:}") String token,
            @Value("${viora.payments.notification-url:}") String notificationUrl,
            @Value("${viora.payments.return-url:}") String returnUrl) {
        this(enabled, sandbox, token, notificationUrl, returnUrl, defaultClient(token));
    }
    MercadoPagoGateway(boolean enabled, boolean sandbox, String token, String notificationUrl, String returnUrl, RestClient client) {
        this.enabled = enabled; this.sandbox = sandbox; this.token = token;
        this.notificationUrl = notificationUrl; this.returnUrl = returnUrl; this.client = client;
    }
    private static RestClient defaultClient(String token) {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().baseUrl("https://api.mercadopago.com").requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + token).build();
    }
    @Override public boolean configured() {
        return enabled && !token.isBlank() && https(notificationUrl) && https(returnUrl);
    }
    private static boolean https(String url) {
        try { var uri = URI.create(url); return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null; }
        catch (IllegalArgumentException ex) { return false; }
    }
    @Override public Checkout create(PaymentAttempt attempt, SubscriptionPrice price) {
        requireConfiguration();
        Map<String, Object> body = Map.of("external_reference", attempt.id().toString(),
                "items", List.of(Map.of("id", attempt.subscriptionId().toString(), "title", "Viora annual producer plan",
                    "quantity", 1, "currency_id", "PEN", "unit_price", price.total().amount())),
                "expires", true, "expiration_date_from", attempt.createdAt().toString(), "expiration_date_to", attempt.expiresAt().toString(),
                "notification_url", notificationUrl, "back_urls", Map.of("success", returnUrl, "failure", returnUrl, "pending", returnUrl));
        try {
            var result = client.post().uri("/checkout/preferences").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(Map.class);
            return checkout(result);
        } catch (RuntimeException ex) { throw new Unavailable(); }
    }
    @Override public Optional<Checkout> recover(UUID intentId) {
        requireConfiguration();
        try {
            var response = get("/checkout/preferences/search?external_reference=" + intentId);
            var list = (List<?>) response.get("elements");
            if (list == null || list.isEmpty()) { return Optional.empty(); }
            if (list.size() != 1) { throw new Unavailable(); }
            var entry = (Map<?, ?>) list.getFirst();
            var id = required(entry, "id");
            if (!id.matches("[a-zA-Z0-9-]+")) { throw new Unavailable(); }
            var preference = get("/checkout/preferences/" + id);
            if (!intentId.toString().equals(required(preference, "external_reference"))) { throw new Unavailable(); }
            return Optional.of(checkout(preference));
        } catch (RuntimeException ex) { throw new Unavailable(); }
    }
    private Checkout checkout(Map<?, ?> map) {
        if (!collector().equals(required(map, "collector_id"))) { throw new Unavailable(); }
        var url = required(map, sandbox ? "sandbox_init_point" : "init_point");
        var uri = URI.create(url);
        var host = uri.getHost();
        if (!https(url) || !("www.mercadopago.com.pe".equals(host) || "sandbox.mercadopago.com.pe".equals(host))) {
            throw new Unavailable();
        }
        return new Checkout(required(map, "id"), url);
    }
    @Override public VerifiedPayment verify(String id) {
        requireConfiguration();
        if (id == null || !id.matches("[0-9]{1,30}")) { throw new Unavailable(); }
        try {
            var map = get("/v1/payments/" + id);
            if (!id.equals(required(map, "id")) || !collector().equals(required(map, "collector_id"))
                    || !(map.get("live_mode") instanceof Boolean live) || !validEnvironment(live)) { throw new Unavailable(); }
            return new VerifiedPayment(id, UUID.fromString(required(map, "external_reference")), required(map, "status"),
                    new Money(new BigDecimal(required(map, "transaction_amount"))), required(map, "currency_id"),
                    Instant.parse(required(map, "date_created")), map.get("date_approved") == null ? null : Instant.parse(required(map, "date_approved")));
        } catch (RuntimeException ex) { throw new Unavailable(); }
    }
    @Override public List<VerifiedPayment> findPayments(UUID intentId) {
        requireConfiguration();
        try {
            var response = get("/v1/payments/search?external_reference=" + intentId + "&limit=100");
            var results = (List<?>) response.get("results");
            var paging = (Map<?, ?>) response.get("paging");
            if (results == null || paging == null || Integer.parseInt(required(paging, "total")) > results.size()) { throw new Unavailable(); }
            var payments = new ArrayList<VerifiedPayment>();
            for (var item : results) {
                var payment = verify(required((Map<?, ?>) item, "id"));
                if (!payment.intentId().equals(intentId)) { throw new Unavailable(); }
                payments.add(payment);
            }
            return List.copyOf(payments);
        } catch (RuntimeException ex) { throw new Unavailable(); }
    }
    private String collector() {
        return merchant().id();
    }
    private Merchant merchant() {
        if (merchantIdentity == null) {
            var account = get("/users/me");
            var testAccount = account.get("tags") instanceof List<?> tags && tags.contains("test_user");
            merchantIdentity = new Merchant(required(account, "id"), testAccount);
        }
        return merchantIdentity;
    }
    private boolean validEnvironment(boolean live) {
        // Test sellers can report live_mode=true; only the authenticated account can establish this exception.
        return sandbox ? !live || merchant().testAccount() : live && !merchant().testAccount();
    }
    private Map<?, ?> get(String path) {
        var response = client.get().uri(path).retrieve().body(Map.class);
        if (response == null) { throw new Unavailable(); }
        return response;
    }
    private static String required(Map<?, ?> map, String key) {
        if (map == null || map.get(key) == null) { throw new Unavailable(); }
        return map.get(key).toString();
    }
    private void requireConfiguration() { if (!configured()) { throw new Unavailable(); } }
}
