package com.arcadiadevs.viora.platform.shared.infrastructure.time.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Exposes the single application clock so time-dependent services can be tested with a fixed one. */
@Configuration
public class ClockConfiguration {

    /**
     * Provides the UTC system clock.
     *
     * @return the application {@link Clock}
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
