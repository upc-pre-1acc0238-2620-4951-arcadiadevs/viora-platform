package com.arcadiadevs.viora.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application bootstrap class for the Viora Platform backend service.
 */
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
