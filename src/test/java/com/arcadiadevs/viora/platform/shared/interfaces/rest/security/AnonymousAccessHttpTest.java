package com.arcadiadevs.viora.platform.shared.interfaces.rest.security;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/** Deny by default through the real filter chain: without a token only the public routes answer. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:anonymousaccess;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AnonymousAccessHttpTest {

    private static final String PLOT = "/api/v1/plots/7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final String PRESCRIPTION = "/api/v1/thinning-prescriptions/7c9e6679-7425-40de-944b-e07fc1f90ae7";
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @ParameterizedTest
    @CsvSource({
            "GET, /api/v1/plots",
            "GET, " + PLOT + "/samplings",
            "POST, " + PLOT + "/samplings",
            "POST, " + PLOT + "/certifications",
            "GET, " + PLOT + "/iot-devices",
            "GET, " + PRESCRIPTION,
            "GET, " + PRESCRIPTION + "/execution-confirmations",
            "POST, " + PRESCRIPTION + "/execution-confirmations",
            "GET, /api/v1/thinning-events",
            "GET, /api/v1/subscriptions",
            "GET, /api/v1/profiles/7c9e6679-7425-40de-944b-e07fc1f90ae7",
            "GET, /an-unmapped-route"
    })
    void protectedRoutesAnswer401WithoutAToken(String method, String path) throws Exception {
        assertEquals(401, send(method, path));
    }

    @ParameterizedTest
    @CsvSource({
            "GET, /api/v1/subscription-plans",
            "GET, /.well-known/assetlinks.json",
            "GET, /api/v1/agronomic-dossiers/verifications/unknown-hash",
            "GET, /payments/return",
            "GET, /password-resets/unknown-token",
            "GET, /v3/api-docs",
            "POST, /api/v1/payment-notifications/mercado-pago",
            "POST, /api/v1/auth/sign-in"
    })
    void publicRoutesNeverAsk401ForAToken(String method, String path) throws Exception {
        assertNotEquals(401, send(method, path));
    }

    private int send(String method, String path) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/json")
                .method(method, "POST".equals(method)
                        ? HttpRequest.BodyPublishers.ofString("{}")
                        : HttpRequest.BodyPublishers.noBody())
                .build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }
}
