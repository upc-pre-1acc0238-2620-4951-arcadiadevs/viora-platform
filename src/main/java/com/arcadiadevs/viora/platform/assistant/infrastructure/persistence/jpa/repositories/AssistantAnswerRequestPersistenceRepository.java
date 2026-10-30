package com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.repositories;

import com.arcadiadevs.viora.platform.assistant.infrastructure.persistence.jpa.entities.AssistantAnswerRequestPersistenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssistantAnswerRequestPersistenceRepository
        extends JpaRepository<AssistantAnswerRequestPersistenceEntity, UUID> {

    Optional<AssistantAnswerRequestPersistenceEntity> findByProducerIdAndClientRequestId(String producerId,
            UUID clientRequestId);

    /** Every key of a deleted account, for the account purge, in one statement. */
    @Modifying
    @Query("delete from AssistantAnswerRequestPersistenceEntity r where r.producerId = :producerId")
    int deleteAllByProducerIdInBulk(@Param("producerId") String producerId);

    /**
     * The keys past the retention: created before the cutoff, bound to an answer created before it, or bound to an
     * answer that no longer exists. Runs before the answers are deleted.
     */
    @Modifying
    @Query("""
            delete from AssistantAnswerRequestPersistenceEntity r
            where r.createdAt < :cutoff
               or r.answerId in (select a.id from AssistantAnswerPersistenceEntity a where a.createdAt < :cutoff)
               or not exists (select 1 from AssistantAnswerPersistenceEntity a where a.id = r.answerId)
            """)
    int deleteExpired(@Param("cutoff") Instant cutoff);
}
