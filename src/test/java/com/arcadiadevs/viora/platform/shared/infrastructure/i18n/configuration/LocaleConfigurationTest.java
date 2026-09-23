package com.arcadiadevs.viora.platform.shared.infrastructure.i18n.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocaleConfigurationTest {

    @Test
    void localeResolverUsesExpectedDefaultAndSupportedLocales() {
        var configuration = new LocaleConfiguration();
        var resolver = (AcceptHeaderLocaleResolver) configuration.localeResolver();
        var request = new MockHttpServletRequest();

        assertEquals(Locale.ENGLISH, resolver.resolveLocale(request));
        assertTrue(resolver.getSupportedLocales().contains(Locale.ENGLISH));
        assertTrue(resolver.getSupportedLocales().contains(Locale.forLanguageTag("es")));
    }
}

