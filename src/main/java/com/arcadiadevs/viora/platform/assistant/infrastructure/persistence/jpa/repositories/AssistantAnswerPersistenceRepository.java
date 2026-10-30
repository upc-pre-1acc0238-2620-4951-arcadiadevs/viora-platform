package com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.entities.AssistantAnswerPersistenceEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Spring Data repository of the saved assistant answers. */
@Repository
public interface AssistantAnswerPersistenceRepository extends JpaRepository<AssistantAnswerPersistenceEntity, UUID> {

    Optional<AssistantAnswerPersistenceEntity> findByIdAndProducerId(UUID id, String producerId);

    Optional<AssistantAnswerPersistenceEntity> findByProducerIdAndClientRequestId(String producerId,
                                                                                  String clientRequestId);

    /** Newest answer to a question about a plot, written in a language from the same data, not older than a moment. */
    @Query("""
            select a from AssistantAnswerPersistenceEntity a
            where a.producerId = :producerId and a.plotId = :plotId and a.questionId = :questionId
              and a.language = :language and a.dataVersion = :dataVersion and a.createdAt >= :since
            order by a.createdAt desc
            """)
    List<AssistantAnswerPersistenceEntity> findReusable(@Param("producerId") String producerId,
                                                        @Param("plotId") String plotId,
                                                        @Param("questionId") String questionId,
                                                        @Param("language") String language,
                                                        @Param("dataVersion") String dataVersion,
                                                        @Param("since") Instant since,
                                                        Pageable page);

    List<AssistantAnswerPersistenceEntity> findByProducerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String producerId, Instant since, Pageable page);

    /** Newest answer of a producer about a plot to a question whose id starts with the prefix of a screen. */
    @Query("""
            select a from AssistantAnswerPersistenceEntity a
            where a.producerId = :producerId and a.plotId = :plotId and a.questionId like concat(:prefix, '%')
              and a.createdAt >= :since
            order by a.createdAt desc
            """)
    List<AssistantAnswerPersistenceEntity> findLatestOfScreen(@Param("producerId") String producerId,
                                                              @Param("plotId") String plotId,
                                                              @Param("prefix") String prefix,
                                                              @Param("since") Instant since,
                                                              Pageable page);

    @Modifying
    @Query("delete from AssistantAnswerPersistenceEntity a where a.producerId = :producerId")
    void deleteAllByProducerId(@Param("producerId") String producerId);
}
