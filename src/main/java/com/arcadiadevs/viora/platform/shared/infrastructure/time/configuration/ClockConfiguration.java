package com.arcadiadevs.viora.platform.shared.infrastructure.time.configuration;

import com.arcadiadevs.viora.platform.shared.domain.model.valueobjects.ProducerTimeZone;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/** Exposes the single application clock so time-dependent services can be tested with a fixed one. */
@Configuration
public class ClockConfiguration {

    /**
     * Provides the system clock in the producers' time zone. Its instants are the same as UTC's; only the dates
     * derived from it ({@code LocalDate.now(clock)}, {@code Year.now(clock)}) follow Peru's calendar.
     *
     * @return the application {@link Clock}
     */
    @Bean
    public Clock clock() {
        return Clock.system(ProducerTimeZone.ZONE);
    }
}
