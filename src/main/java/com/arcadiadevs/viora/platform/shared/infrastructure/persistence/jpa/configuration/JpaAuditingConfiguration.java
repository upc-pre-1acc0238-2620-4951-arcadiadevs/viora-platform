package com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Enables persistence auditing without coupling the application bootstrap
 * or non-persistence test slices to JPA infrastructure.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {
}
