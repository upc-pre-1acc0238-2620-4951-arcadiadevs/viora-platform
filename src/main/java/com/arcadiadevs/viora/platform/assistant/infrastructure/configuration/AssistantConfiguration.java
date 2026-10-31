package com.arcadiadevs.viora.platform.assistant.infrastructure.configuration;

import com.arcadiadevs.viora.platform.assistant.domain.services.FactsAssemblerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires the Spring-free domain services of the assistant.
 */
@Configuration
public class AssistantConfiguration {

    /**
     * The facts assembler of each HOME, ALTERNATION, PLAN, ALERT, CLIMATE, WINTER_CHILL, PLOT_DETAIL, SENSORS, SAMPLING_SUMMARY and THINNING_DONE question.
     *
     * @return the registry
     */
    @Bean
    public FactsAssemblerRegistry factsAssemblerRegistry() {
        return FactsAssemblerRegistry.standard();
    }
}
