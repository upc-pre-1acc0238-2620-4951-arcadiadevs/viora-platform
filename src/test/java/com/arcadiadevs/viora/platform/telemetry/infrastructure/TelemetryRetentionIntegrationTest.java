package com.arcadiadevs.viora.platform.telemetry.infrastructure;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.TelemetrySeries;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReading;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.*;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** The retention deletes the readings older than the cutoff and keeps the rest of the series. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:telemetryretention;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class TelemetryRetentionIntegrationTest {

    @Autowired TelemetrySeriesRepository repository;
    @Autowired PlatformTransactionManager transactionManager;

    @Test
    void deletesOnlyTheReadingsOlderThanTheCutoff() {
        var plotId = new PlotId(UUID.randomUUID().toString());
        var now = Instant.now().truncatedTo(ChronoUnit.HOURS);
        var transactions = new TransactionTemplate(transactionManager);
        transactions.executeWithoutResult(status -> {
            var series = TelemetrySeries.create(plotId, null);
            series.ingestReading(reading(now.minus(Duration.ofDays(120))));
            series.ingestReading(reading(now.minus(Duration.ofDays(100))));
            series.ingestReading(reading(now.minus(Duration.ofDays(2))));
            repository.save(series);
        });

        int deleted = transactions.execute(status -> repository.deleteReadingsOlderThan(now.minus(Duration.ofDays(90))));

        assertThat(deleted).isEqualTo(2);
        assertThat(repository.findReadingsByPlotIdAndDateRange(plotId, null, null)).hasSize(1);
    }

    private static HourlyTelemetryReading reading(Instant hour) {
        return HourlyTelemetryReading.create(new ReadingTimestamp(hour), new AmbientTemperature(20.0),
                new RelativeHumidity(50.0), new SoilMoisture(25.0), new SolarRadiation(300.0), null);
    }
}
