package com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.HourlyTelemetryReadingSnapshot;
import com.arcadiadevs.viora.platform.telemetry.domain.model.aggregates.TelemetrySeries;
import com.arcadiadevs.viora.platform.telemetry.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.telemetry.domain.repositories.TelemetrySeriesRepository;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.assemblers.TelemetrySeriesPersistenceAssembler;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.entities.HourlyTelemetryReadingPersistenceEntity;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.repositories.HourlyTelemetryReadingPersistenceRepository;
import com.arcadiadevs.viora.platform.telemetry.infrastructure.persistence.jpa.repositories.TelemetrySeriesPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Secondary adapter implementing {@link TelemetrySeriesRepository} with Spring Data JPA.
 */
@Repository
public class TelemetrySeriesRepositoryImpl implements TelemetrySeriesRepository {

    private final TelemetrySeriesPersistenceRepository seriesRepository;
    private final HourlyTelemetryReadingPersistenceRepository readingRepository;

    /**
     * Constructs the repository adapter injecting required JPA repositories.
     *
     * @param seriesRepository  the series JPA repository
     * @param readingRepository the hourly reading JPA repository
     */
    public TelemetrySeriesRepositoryImpl(
            TelemetrySeriesPersistenceRepository seriesRepository,
            HourlyTelemetryReadingPersistenceRepository readingRepository
    ) {
        this.seriesRepository = seriesRepository;
        this.readingRepository = readingRepository;
    }

    @Override
    public Optional<TelemetrySeries> findByPlotId(PlotId plotId) {
        if (plotId == null) {
            return Optional.empty();
        }
        var plotUuid = UUID.fromString(plotId.plotId());
        return seriesRepository.findByPlotId(plotUuid)
                .map(TelemetrySeriesPersistenceAssembler::toDomainFromPersistence);
    }

    @Override
    public List<HourlyTelemetryReadingSnapshot> findReadingsByPlotIdAndDateRange(PlotId plotId, Instant startDate, Instant endDate) {
        if (plotId == null) {
            return List.of();
        }
        var plotUuid = UUID.fromString(plotId.plotId());
        List<HourlyTelemetryReadingPersistenceEntity> entities;

        if (startDate != null && endDate != null) {
            entities = readingRepository.findAllByPlotIdAndReadingTimestampBetweenOrderByReadingTimestampAsc(plotUuid, startDate, endDate);
        } else if (startDate != null) {
            entities = readingRepository.findAllByPlotIdAndReadingTimestampGreaterThanEqualOrderByReadingTimestampAsc(plotUuid, startDate);
        } else if (endDate != null) {
            entities = readingRepository.findAllByPlotIdAndReadingTimestampLessThanEqualOrderByReadingTimestampAsc(plotUuid, endDate);
        } else {
            entities = readingRepository.findAllByPlotIdOrderByReadingTimestampAsc(plotUuid);
        }

        return entities.stream()
                .map(TelemetrySeriesPersistenceAssembler::toReadingSnapshot)
                .toList();
    }

    @Override
    public TelemetrySeries save(TelemetrySeries series) {
        if (series == null) {
            throw new IllegalArgumentException("telemetry.series.snapshot.null");
        }
        var seriesUuid = UUID.fromString(series.id().seriesId());
        var existingOpt = seriesRepository.findById(seriesUuid);

        if (existingOpt.isPresent()) {
            var managed = existingOpt.get();
            TelemetrySeriesPersistenceAssembler.updateEntityFromDomain(managed, series);
            var saved = seriesRepository.save(managed);
            return TelemetrySeriesPersistenceAssembler.toDomainFromPersistence(saved);
        } else {
            var newEntity = TelemetrySeriesPersistenceAssembler.toPersistenceFromDomain(series);
            var saved = seriesRepository.save(newEntity);
            return TelemetrySeriesPersistenceAssembler.toDomainFromPersistence(saved);
        }
    }

    @Override
    public boolean existsByPlotId(PlotId plotId) {
        if (plotId == null) {
            return false;
        }
        return seriesRepository.existsByPlotId(UUID.fromString(plotId.plotId()));
    }
}
