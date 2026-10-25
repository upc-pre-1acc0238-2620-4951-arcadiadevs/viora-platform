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
    private final String itemTitle;
    private record Merchant(String id, boolean testAccount) { }
    private volatile Merchant merchantIdentity;
    /** Appended to the return URL for extension payments; the app declares the same path. */
    public static final String EXTENSION_RETURN_SUFFIX = "/extension";
    @org.springframework.beans.factory.annotation.Autowired
    public MercadoPagoGateway(@Value("${viora.payments.enabled}") boolean enabled,
            @Value("${viora.payments.sandbox}") boolean sandbox,
            @Value("${viora.payments.access-token}") String token,
            @Value("${viora.payments.notification-url}") String notificationUrl,
            @Value("${viora.payments.return-url}") String returnUrl,
            @Value("${viora.payments.item-title}") String itemTitle,
            @Value("${viora.payments.connect-timeout}") Duration connectTimeout,
            @Value("${viora.payments.read-timeout}") Duration readTimeout) {
        this(enabled, sandbox, token, notificationUrl, returnUrl, itemTitle,
                defaultClient(token, connectTimeout, readTimeout));
    }
    MercadoPagoGateway(boolean enabled, boolean sandbox, String token, String notificationUrl, String returnUrl, String itemTitle, RestClient client) {
        this.enabled = enabled; this.sandbox = sandbox; this.token = token;
        this.notificationUrl = notificationUrl; this.returnUrl = returnUrl; this.client = client; this.itemTitle = itemTitle;
    }
    private static RestClient defaultClient(String token, Duration connectTimeout, Duration readTimeout) {
        if (connectTimeout == null || readTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()
                || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("subscription.gateway.configuration.invalid");
        }
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder().connectTimeout(connectTimeout).build());
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl("https://api.mercadopago.com").requestFactory(factory)
                .defaultHeader("Authorization", "Bearer " + token).build();
    }
    @Override public boolean configured() {
        return enabled && itemTitle != null && !itemTitle.isBlank() && !token.isBlank() && https(notificationUrl) && https(returnUrl);
    }
    private static boolean https(String url) {
        try { var uri = URI.create(url); return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null; }
        catch (IllegalArgumentException ex) { return false; }
    }
    @Override public Checkout create(PaymentAttempt attempt, SubscriptionPrice price) {
        requireConfiguration();
        var back = returnUrlFor(attempt);
        Map<String, Object> body = Map.of("external_reference", attempt.id().toString(),
                "items", List.of(Map.of("id", attempt.subscriptionId().toString(), "title", itemTitle,
                    "quantity", 1, "currency_id", "PEN", "unit_price", price.total().amount())),
                "expires", true, "expiration_date_from", attempt.createdAt().toString(), "expiration_date_to", attempt.expiresAt().toString(),
                "notification_url", notificationUrl, "back_urls", Map.of("success", back, "failure", back, "pending", back));
        try {
            var result = client.post().uri("/checkout/preferences").contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(body).retrieve().body(Map.class);
            return checkout(result);
        } catch (RuntimeException ex) { throw new Unavailable(); }
    }
    /**
     * The payment of a plan extension returns to its own path under the return URL, so the app opens the extension
     * result instead of the plan purchase.
     */
    private String returnUrlFor(PaymentAttempt attempt) {
        if (attempt.extensionId() == null) { return returnUrl; }
        var query = returnUrl.indexOf('?');
        var path = query < 0 ? returnUrl : returnUrl.substring(0, query);
        var extension = path.replaceFirst("/+$", "") + EXTENSION_RETURN_SUFFIX;
        return query < 0 ? extension : extension + returnUrl.substring(query);
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
    @Override public void close(String preferenceId, Instant at) {
        requireConfiguration();
        if (preferenceId == null || !preferenceId.matches("[a-zA-Z0-9-]+") || at == null) { throw new Unavailable(); }
        try {
            var result = client.put().uri("/checkout/preferences/" + preferenceId)
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(Map.of("expires", true, "expiration_date_to", at.toString())).retrieve().body(Map.class);
            if (result == null || !preferenceId.equals(required(result, "id"))) { throw new Unavailable(); }
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
