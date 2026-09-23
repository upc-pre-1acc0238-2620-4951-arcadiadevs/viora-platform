package com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.fixtures.TestParentEntity;
import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.fixtures.TestRecordEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PersistenceSchemaAndPrecisionIntegrationTest {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void persistsAuditableTimestampsOnEntityCreation() {
        TestParentEntity parent = new TestParentEntity("Parent-1");
        entityManager.persist(parent);
        entityManager.flush();

        assertNotNull(parent.getId());
        assertNotNull(parent.getCreatedAt());
        assertNotNull(parent.getUpdatedAt());
        assertEquals(parent.getCreatedAt(), parent.getUpdatedAt());
    }

    @Test
    void preservesExactDecimalPrecisionAndScale() {
        TestParentEntity parent = new TestParentEntity("Parent-2");
        entityManager.persist(parent);
        entityManager.flush();

        BigDecimal exactMeasurement = new BigDecimal("12345678.1234");
        BigDecimal exactPercentage = new BigDecimal("99.75");

        TestRecordEntity record = new TestRecordEntity(exactMeasurement, exactPercentage, parent);
        entityManager.persist(record);
        entityManager.flush();
        entityManager.clear();

        TestRecordEntity reloaded = entityManager.find(TestRecordEntity.class, record.getId());

        assertNotNull(reloaded);
        assertEquals(0, exactMeasurement.compareTo(reloaded.getMeasurementValue()));
        assertEquals(4, reloaded.getMeasurementValue().scale());
        assertEquals(exactMeasurement.setScale(4, RoundingMode.UNNECESSARY), reloaded.getMeasurementValue());

        assertEquals(0, exactPercentage.compareTo(reloaded.getPercentageScore()));
        assertEquals(2, reloaded.getPercentageScore().scale());
    }

    @Test
    void supportsForeignKeysAndRelationalLookups() {
        TestParentEntity parent = new TestParentEntity("Parent-Relational");
        entityManager.persist(parent);
        entityManager.flush();

        TestRecordEntity record = new TestRecordEntity(new BigDecimal("10.5000"), new BigDecimal("50.00"), parent);
        entityManager.persist(record);
        entityManager.flush();
        entityManager.clear();

        List<TestRecordEntity> found = entityManager.createQuery(
                "SELECT r FROM TestRecordEntity r WHERE r.parent.id = :parentId",
                TestRecordEntity.class
        ).setParameter("parentId", parent.getId()).getResultList();

        assertFalse(found.isEmpty());
        assertEquals(1, found.size());
        assertEquals(parent.getId(), found.get(0).getParent().getId());
    }
}
