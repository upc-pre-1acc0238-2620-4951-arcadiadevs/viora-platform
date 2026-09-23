package com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.fixtures;

import com.arcadiadevs.viora.platform.shared.infrastructure.persistence.jpa.entities.AuditableAbstractPersistenceEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "test_parent_entities")
public class TestParentEntity extends AuditableAbstractPersistenceEntity {

    @Column(nullable = false)
    private String name;

    public TestParentEntity(String name) {
        this.name = name;
    }
}
