package com.arcadiadevs.viora.platform.orchard.infrastructure.configuration;

import com.arcadiadevs.viora.platform.orchard.domain.services.CadastralGeometryService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration class for orchard domain services.
 */
@Configuration
public class OrchardDomainServicesConfiguration {

    /**
     * Creates and registers the {@link CadastralGeometryService} domain service bean.
     *
     * @return The {@link CadastralGeometryService} instance
     */
    @Bean
    public CadastralGeometryService cadastralGeometryService() {
        return new CadastralGeometryService();
    }
}

