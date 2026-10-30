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
     * The facts assembler of each ALTERNATION, PLAN, ALERT and CLIMATE question.
     *
     * @return the registry
     */
    @Bean
    public FactsAssemblerRegistry factsAssemblerRegistry() {
        return FactsAssemblerRegistry.standard();
    }
}
