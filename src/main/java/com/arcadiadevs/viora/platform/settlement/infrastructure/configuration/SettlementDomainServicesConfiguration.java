package com.arcadiadevs.viora.platform.settlement.infrastructure.configuration;

import com.arcadiadevs.viora.platform.settlement.domain.services.CryptographicHashService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Registers the annotation-free Settlement domain services as Spring beans. */
@Configuration
public class SettlementDomainServicesConfiguration {

    /**
     * Provides the SHA-256 hashing domain service.
     *
     * @return the {@link CryptographicHashService}
     */
    @Bean
    public CryptographicHashService cryptographicHashService() {
        return new CryptographicHashService();
    }
}
