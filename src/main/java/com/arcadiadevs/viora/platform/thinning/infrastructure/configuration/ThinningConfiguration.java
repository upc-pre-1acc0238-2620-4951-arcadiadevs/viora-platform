package com.arcadiadevs.viora.platform.thinning.infrastructure.configuration;

import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.ThinningProfiles;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Registers the configuration of the thinning technical profiles. */
@Configuration
@EnableConfigurationProperties(ThinningProfiles.class)
public class ThinningConfiguration {
}
