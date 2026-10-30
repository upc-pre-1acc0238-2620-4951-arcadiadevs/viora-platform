package com.arcadiadevs.viora.platform.iam.infrastructure.authorization.sfs.pipeline;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Goes through the real security filter chain, which MockMvc tests skip, to check that a 401 is a problem detail
 * like every other error of the API.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UnauthorizedRequestHttpTest {
    private static final HttpClient CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).build();
    private static final String PROTECTED_PATH = "/api/v1/plots";

    @LocalServerPort
    private int port;

    @ParameterizedTest(name = "{0} token, {1}")
    @CsvSource({
            "no,      es, Necesitas iniciar sesión para acceder a este recurso",
            "no,      en, You need to sign in to access this resource",
            "invalid, es, Necesitas iniciar sesión para acceder a este recurso",
            "invalid, en, You need to sign in to access this resource"
    })
    void answersAProblemDetailInTheRequestedLanguage(String token, String language, String detail) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + PROTECTED_PATH))
                .header("Accept-Language", language)
                .GET();
        if (token.equals("invalid")) {
            request.header("Authorization", "Bearer not-a-valid-token");
        }

        var response = CLIENT.send(request.build(), HttpResponse.BodyHandlers.ofString());

        assertEquals(401, response.statusCode());
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/problem+json"),
                () -> "Content-Type was " + response.headers().firstValue("Content-Type"));
        assertEquals(401, (int) JsonPath.read(response.body(), "$.status"));
        assertEquals("Unauthorized", JsonPath.read(response.body(), "$.title"));
        assertEquals("USER_UNAUTHORIZED", JsonPath.read(response.body(), "$.code"));
        assertEquals(PROTECTED_PATH, JsonPath.read(response.body(), "$.instance"));
        assertEquals(detail, JsonPath.read(response.body(), "$.detail"));
    }
}
