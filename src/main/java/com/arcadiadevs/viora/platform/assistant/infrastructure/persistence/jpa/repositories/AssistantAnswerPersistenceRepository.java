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

@Repository
public interface AssistantAnswerPersistenceRepository extends JpaRepository<AssistantAnswerPersistenceEntity, UUID> {

    Optional<AssistantAnswerPersistenceEntity> findByProducerIdAndClientRequestId(String producerId,
            UUID clientRequestId);

    Optional<AssistantAnswerPersistenceEntity>
            findFirstByProducerIdAndQuestionIdAndPlotIdAndLocaleAndDataVersionAndCreatedAtGreaterThanOrderByCreatedAtDesc(
                    String producerId, String questionId, String plotId, String locale, String dataVersion,
                    Instant since);

    List<AssistantAnswerPersistenceEntity> findByProducerIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
            String producerId, Instant since, Pageable page);

    Optional<AssistantAnswerPersistenceEntity>
            findFirstByProducerIdAndPlotIdAndQuestionIdStartingWithAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                    String producerId, String plotId, String questionPrefix, Instant since);

    /** Everything of a deleted account, for the account purge, in one statement. */
    @Modifying
    @Query("delete from AssistantAnswerPersistenceEntity a where a.producerId = :producerId")
    int deleteAllByProducerIdInBulk(@Param("producerId") String producerId);

    /** The answers past the retention, in one statement. */
    @Modifying
    @Query("delete from AssistantAnswerPersistenceEntity a where a.createdAt < :cutoff")
    int deleteCreatedBefore(@Param("cutoff") Instant cutoff);
}
