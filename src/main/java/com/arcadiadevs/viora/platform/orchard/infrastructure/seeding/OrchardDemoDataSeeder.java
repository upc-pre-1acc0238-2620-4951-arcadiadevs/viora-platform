package com.arcadiadevs.viora.platform.orchard.infrastructure.seeding;

import com.arcadiadevs.viora.platform.orchard.application.commandservices.PlotCommandService;
import com.arcadiadevs.viora.platform.orchard.domain.model.commands.DelimitPlotCommand;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.PlotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Demo data seeder for the Orchard bounded context.
 *
 * <p>Ensures that the default test producer has baseline operational plots
 * so that downstream telemetry incidents and mobile views function immediately.</p>
 */
@Component
@Order(1)
public class OrchardDemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OrchardDemoDataSeeder.class);

    private final PlotCommandService plotCommandService;
    private final PlotRepository plotRepository;
    private final String defaultProducerId;

    private static final String GEOJSON_PLOT_A =
            "{\"type\":\"Polygon\",\"coordinates\":[[[-70.25,-18.05],[-70.24,-18.05],[-70.24,-18.06],[-70.25,-18.06],[-70.25,-18.05]]]}";

    private static final String GEOJSON_PLOT_B =
            "{\"type\":\"Polygon\",\"coordinates\":[[[-70.26,-18.07],[-70.25,-18.07],[-70.25,-18.08],[-70.26,-18.08],[-70.26,-18.07]]]}";

    public OrchardDemoDataSeeder(
            PlotCommandService plotCommandService,
            PlotRepository plotRepository,
            @Value("${viora.security.mock.default-producer-id:550e8400-e29b-41d4-a716-446655440000}") String defaultProducerId
    ) {
        this.plotCommandService = plotCommandService;
        this.plotRepository = plotRepository;
        this.defaultProducerId = defaultProducerId;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            var producerIdVo = new ProducerId(defaultProducerId);
            var existingPlots = plotRepository.findActiveByProducerId(producerIdVo);
            if (!existingPlots.isEmpty()) {
                log.info("Active plots already exist for default producer {}. Skipping Orchard demo seeding.", defaultProducerId);
                return;
            }

            var delimitPlotA = new DelimitPlotCommand(
                    defaultProducerId,
                    "La Yarada 02",
                    "SEVILLANA",
                    GEOJSON_PLOT_A,
                    7.0,
                    5.0
            );
            var resultA = plotCommandService.handle(delimitPlotA);
            resultA.failure().ifPresent(err -> log.warn("Failed to seed plot 'La Yarada 02': {}", err.message()));

            var delimitPlotB = new DelimitPlotCommand(
                    defaultProducerId,
                    "Lote Norte",
                    "CRIOLLA",
                    GEOJSON_PLOT_B,
                    6.0,
                    4.0
            );
            var resultB = plotCommandService.handle(delimitPlotB);
            resultB.failure().ifPresent(err -> log.warn("Failed to seed plot 'Lote Norte': {}", err.message()));

            log.info("OrchardDemoDataSeeder successfully initialized baseline plots for producer {}.", defaultProducerId);
        } catch (Exception ex) {
            log.error("Unexpected error during OrchardDemoDataSeeder execution: {}", ex.getMessage(), ex);
        }
    }
}
