package com.arcadiadevs.viora.platform.shared.interfaces.rest.controllers;

import com.arcadiadevs.viora.platform.shared.infrastructure.deeplinks.configuration.DeepLinkProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AndroidAssetLinksController and DeepLinkProperties Unit Tests")
class AndroidAssetLinksControllerTest {

    @Test
    @DisplayName("Publishes the app package and fingerprints so Android opens the links in the app")
    void publishesConfiguredApp() throws Exception {
        var properties = new DeepLinkProperties("https://viora.example", "/reset-password",
                new DeepLinkProperties.Android("com.arcadiadevs.viora", List.of("AB:CD:EF", " ")));
        var mockMvc = MockMvcBuilders.standaloneSetup(new AndroidAssetLinksController(properties)).build();

        mockMvc.perform(get("/.well-known/assetlinks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].relation[0]").value("delegate_permission/common.handle_all_urls"))
                .andExpect(jsonPath("$[0].target.namespace").value("android_app"))
                .andExpect(jsonPath("$[0].target.package_name").value("com.arcadiadevs.viora"))
                .andExpect(jsonPath("$[0].target.sha256_cert_fingerprints.length()").value(1))
                .andExpect(jsonPath("$[0].target.sha256_cert_fingerprints[0]").value("AB:CD:EF"));
    }

    @Test
    @DisplayName("Answers an empty list while the app identity is not configured")
    void answersEmptyListWhenUnconfigured() throws Exception {
        var properties = new DeepLinkProperties("", null, null);
        var mockMvc = MockMvcBuilders.standaloneSetup(new AndroidAssetLinksController(properties)).build();

        mockMvc.perform(get("/.well-known/assetlinks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Builds the reset link from the base URL and path, without doubled slashes")
    void buildsResetLink() {
        var properties = new DeepLinkProperties("https://viora.example/", null,
                new DeepLinkProperties.Android(null, Arrays.asList((String) null)));

        assertThat(properties.passwordResetLink("Ab9_x-Z")).isEqualTo("https://viora.example/reset-password?token=Ab9_x-Z");
        assertThat(properties.android().isConfigured()).isFalse();
    }
}
