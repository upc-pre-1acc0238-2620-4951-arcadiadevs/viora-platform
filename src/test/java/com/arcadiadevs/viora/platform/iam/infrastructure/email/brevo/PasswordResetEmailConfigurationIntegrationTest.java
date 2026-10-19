package com.arcadiadevs.viora.platform.iam.infrastructure.email.brevo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The password reset email as the platform configures it: a subject with its accents intact and the images the
 * template links to, served by the platform itself.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:resetemail;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@DisplayName("Password reset email configuration")
class PasswordResetEmailConfigurationIntegrationTest {

    @Autowired WebApplicationContext context;
    @Value("${viora.brevo.reset-subject}") String resetSubject;

    @Test
    @DisplayName("The default subject keeps its accents and the em dash")
    void subjectKeepsItsAccents() {
        assertThat(resetSubject).isEqualTo("Restablece tu contraseña — Viora");
    }

    @Test
    @DisplayName("The images of the email are served without a session")
    void imagesAreServed() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();

        mvc.perform(get("/email/olive-sprig.gif"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/gif"));
        mvc.perform(get("/email/viora-logo.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
        mvc.perform(get("/email/copy.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"));
    }

    @Test
    @DisplayName("The Android asset links file is served without a session, with the app package and fingerprint")
    void assetLinksAreServed() throws Exception {
        var mvc = MockMvcBuilders.webAppContextSetup(context).build();

        mvc.perform(get("/.well-known/assetlinks.json"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        [{"target": {"namespace": "android_app", "package_name": "pe.edu.upc.viora",
                          "sha256_cert_fingerprints": ["8C:B1:E1:62:88:15:5E:46:61:0D:A7:C8:60:BA:E7:2B:A2:42:48:49:C4:2C:C3:88:C6:02:21:FA:04:B4:F4:12"]}}]
                        """));
    }
}
