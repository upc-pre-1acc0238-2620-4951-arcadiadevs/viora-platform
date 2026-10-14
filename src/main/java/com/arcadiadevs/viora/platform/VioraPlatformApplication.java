package com.arcadiadevs.viora.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Main application bootstrap class for the Viora Platform backend service.
 */
@EnableAsync
@EnableJpaAuditing
@SpringBootApplication
public class VioraPlatformApplication {

    /**
     * Application entry point.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        SpringApplication.run(VioraPlatformApplication.class, args);
    }

}
