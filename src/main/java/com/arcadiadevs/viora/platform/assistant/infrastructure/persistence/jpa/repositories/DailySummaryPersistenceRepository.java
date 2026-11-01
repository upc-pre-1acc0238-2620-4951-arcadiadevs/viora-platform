package com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.entities.DailySummaryPersistenceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Spring Data repository of the stored daily summaries. */
@Repository
public interface DailySummaryPersistenceRepository extends JpaRepository<DailySummaryPersistenceEntity, UUID> {

    /** Newest summary of a plot, written in a language from the same data, not older than a moment. */
    @Query("""
            select s from DailySummaryPersistenceEntity s
            where s.producerId = :producerId and s.plotId = :plotId and s.language = :language
              and s.dataVersion = :dataVersion and s.createdAt >= :since
            order by s.createdAt desc
            """)
    List<DailySummaryPersistenceEntity> findCurrent(@Param("producerId") String producerId,
                                                    @Param("plotId") String plotId,
                                                    @Param("language") String language,
                                                    @Param("dataVersion") String dataVersion,
                                                    @Param("since") Instant since,
                                                    Pageable page);

    /** Newest summary of the producer's whole field (no plot); it never matches a summary of a plot. */
    @Query("""
            select s from DailySummaryPersistenceEntity s
            where s.producerId = :producerId and s.plotId is null and s.language = :language
              and s.dataVersion = :dataVersion and s.createdAt >= :since
            order by s.createdAt desc
            """)
    List<DailySummaryPersistenceEntity> findCurrentOfField(@Param("producerId") String producerId,
                                                           @Param("language") String language,
                                                           @Param("dataVersion") String dataVersion,
                                                           @Param("since") Instant since,
                                                           Pageable page);

    @Modifying
    @Query("delete from DailySummaryPersistenceEntity s where s.producerId = :producerId")
    void deleteAllByProducerId(@Param("producerId") String producerId);

    @Modifying
    @Query("delete from DailySummaryPersistenceEntity s where s.createdAt < :before")
    int deleteCreatedBefore(@Param("before") Instant before);
}
