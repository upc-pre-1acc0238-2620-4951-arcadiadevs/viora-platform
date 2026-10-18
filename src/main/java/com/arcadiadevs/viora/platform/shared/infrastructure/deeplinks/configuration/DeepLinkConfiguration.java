package com.arcadiadevs.viora.platform.shared.infrastructure.deeplinks.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the configuration of the links that open the mobile app. */
@Configuration
@EnableConfigurationProperties(DeepLinkProperties.class)
public class DeepLinkConfiguration {
}
