package com.arcadiadevs.viora.platform.iam.infrastructure.email.brevo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.containsString;

@DisplayName("BrevoTransactionalEmailAdapter Infrastructure Adapter Unit Tests")
class BrevoTransactionalEmailAdapterTest {

    private static final String BREVO_URL = "https://api.brevo.com/v3/smtp/email";

    @Test
    @DisplayName("Should return silently when API key is blank in local/test environment")
    void shouldReturnSilentlyWhenApiKeyIsBlank() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new BrevoTransactionalEmailAdapter(builder.build(), true, "", "viora@arcadiadevs.com", "Viora", "Subject");

        assertThatCode(() -> adapter.sendPasswordResetEmail("user@viora.ag", "123456"))
                .doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("Should return silently when enabled is false")
    void shouldReturnSilentlyWhenDisabled() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var adapter = new BrevoTransactionalEmailAdapter(builder.build(), false, "any-api-key", "viora@arcadiadevs.com", "Viora", "Subject");

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
                .andRespond(withSuccess("{\"messageId\":\"abc\"}", MediaType.APPLICATION_JSON));
        var adapter = new BrevoTransactionalEmailAdapter(builder.build(), true, "test-key", "viora@arcadiadevs.com", "Viora", "Código — Viora");

        adapter.sendPasswordResetEmail("user@viora.ag", "123456");

        server.verify();
    }

    @Test
    @DisplayName("Should fail with an i18n key when Brevo rejects the request")
    void shouldFailWhenBrevoRejects() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(BREVO_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED));
        var adapter = new BrevoTransactionalEmailAdapter(builder.build(), true, "bad-key", "viora@arcadiadevs.com", "Viora", "Subject");

        assertThatThrownBy(() -> adapter.sendPasswordResetEmail("user@viora.ag", "123456"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("email.password_reset.dispatch_failed");
    }
}
