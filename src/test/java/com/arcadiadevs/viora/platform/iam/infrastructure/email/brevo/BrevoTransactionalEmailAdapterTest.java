package com.arcadiadevs.viora.platform.iam.infrastructure.email.brevo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.sun.net.httpserver.HttpServer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

@DisplayName("BrevoTransactionalEmailAdapter Infrastructure Adapter Unit Tests")
class BrevoTransactionalEmailAdapterTest {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";
    private static final String ASSETS = "https://viora.example/";
    private static final String UNRECOGNISED_IP = "{\"code\":\"unauthorized\",\"message\":\"We have detected you are "
            + "using an unrecognised IP address 203.0.113.7. If you performed this action make sure to add the new IP "
            + "address in this link: https://app.brevo.com/security/authorised_ips\"}";

    @Test
    @DisplayName("Should return silently when API key is blank in local/test environment")
    void shouldReturnSilentlyWhenApiKeyIsBlank() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new BrevoTransactionalEmailAdapter(builder.build(), true, "", "viora@arcadiadevs.com", "Viora", "Subject", ASSETS);

        assertThatCode(() -> adapter.sendPasswordResetEmail("user@viora.ag", "123456"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("Should return silently when enabled is false")
    void shouldReturnSilentlyWhenDisabled() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new BrevoTransactionalEmailAdapter(builder.build(), false, "any-api-key", "viora@arcadiadevs.com", "Viora", "Subject", ASSETS);

        assertThatCode(() -> adapter.sendPasswordResetEmail("user@viora.ag", "123456"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("Should post the recovery code to Brevo rendered inside the HTML template")
    void shouldPostCodeToBrevo() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BREVO_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("api-key", "test-key"))
                .andExpect(jsonPath("$.to[0].email").value("user@viora.ag"))
                .andExpect(jsonPath("$.subject").value("Código — Viora"))
                .andExpect(jsonPath("$.htmlContent").value(containsString("123456")))
                .andExpect(jsonPath("$.htmlContent").value(containsString("src=\"https://viora.example/email/olive-sprig.gif\"")))
                .andExpect(jsonPath("$.htmlContent").value(not(containsString("{{"))))
                .andRespond(withSuccess("{\"messageId\":\"abc\"}", MediaType.APPLICATION_JSON));
        var adapter = new BrevoTransactionalEmailAdapter(builder.build(), true, "test-key", "viora@arcadiadevs.com", "Viora", "Código — Viora", ASSETS);

        adapter.sendPasswordResetEmail("user@viora.ag", "123456");

        server.verify();
    }

    @Test
    @DisplayName("Should fail with an i18n key when Brevo rejects the request")
    void shouldFailWhenBrevoRejects() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BREVO_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(UNRECOGNISED_IP));
        var adapter = new BrevoTransactionalEmailAdapter(builder.build(), true, "bad-key", "viora@arcadiadevs.com", "Viora", "Subject", ASSETS);

        assertThatThrownBy(() -> adapter.sendPasswordResetEmail("user@viora.ag", "123456"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("email.password_reset.dispatch_failed")
                .cause()
                .isInstanceOfSatisfying(RestClientResponseException.class,
                        cause -> assertThat(cause.getResponseBodyAsString()).contains("unrecognised IP address"));
    }

    @Test
    @DisplayName("The default HTTP client keeps the body Brevo sends with a 401, where it names the blocked IP")
    void defaultClientKeepsTheBodyOfARejection() throws Exception {
        var brevo = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        brevo.createContext("/v3/smtp/email", exchange -> {
            exchange.getRequestBody().readAllBytes();
            var body = UNRECOGNISED_IP.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(401, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        brevo.start();
        try {
            var defaultClient = ReflectionTestUtils.invokeMethod(BrevoTransactionalEmailAdapter.class, "defaultRestClient");
            RestClient client = (RestClient) defaultClient;

            assertThatThrownBy(() -> client.post()
                    .uri("http://127.0.0.1:" + brevo.getAddress().getPort() + "/v3/smtp/email")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{}")
                    .retrieve()
                    .toBodilessEntity())
                    .isInstanceOfSatisfying(RestClientResponseException.class,
                            ex -> assertThat(ex.getResponseBodyAsString()).contains("unrecognised IP address 203.0.113.7"));
        } finally {
            brevo.stop(0);
        }
    }
}
