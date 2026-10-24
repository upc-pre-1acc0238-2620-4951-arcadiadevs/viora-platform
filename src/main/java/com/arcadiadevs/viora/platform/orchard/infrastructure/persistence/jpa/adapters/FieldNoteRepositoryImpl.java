package com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.adapters;

import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.FieldNote;
import com.arcadiadevs.viora.platform.orchard.domain.model.aggregates.FieldNoteSnapshot;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNoteId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNotePhoto;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNoteText;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.FieldNoteType;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.orchard.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.orchard.domain.repositories.FieldNoteRepository;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.entities.FieldNotePersistenceEntity;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.entities.FieldNotePhotoPersistenceEntity;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.repositories.FieldNotePersistenceRepository;
import com.arcadiadevs.viora.platform.orchard.infrastructure.persistence.jpa.repositories.FieldNotePhotoPersistenceRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JPA adapter of the field notes: the note and its photo bytes in two tables. */
@Repository
public class FieldNoteRepositoryImpl implements FieldNoteRepository {

    private final FieldNotePersistenceRepository notes;
    private final FieldNotePhotoPersistenceRepository photos;

    public FieldNoteRepositoryImpl(FieldNotePersistenceRepository notes, FieldNotePhotoPersistenceRepository photos) {
        this.notes = notes;
        this.photos = photos;
    }

    @Override
    public void save(FieldNote note) {
        notes.saveAndFlush(toEntity(note.snapshot()));
    }

    @Override
    public void savePhoto(FieldNote note, FieldNotePhoto photo) {
        var id = UUID.fromString(note.snapshot().id().value());
        var entity = notes.findById(id).orElseThrow();
        entity.setPhotoMediaType(photo.mediaType());
        notes.saveAndFlush(entity);

        var bytes = photos.findById(id).orElseGet(() -> {
            var created = new FieldNotePhotoPersistenceEntity();
            created.setNoteId(id);
            return created;
        });
        bytes.setContent(photo.content());
        bytes.setMediaType(photo.mediaType());
        photos.saveAndFlush(bytes);
    }

    @Override
    public Optional<FieldNote> findById(FieldNoteId id) {
        return notes.findById(UUID.fromString(id.value())).map(FieldNoteRepositoryImpl::toDomain);
    }

    @Override
    public List<FieldNote> findByPlotId(PlotId plotId) {
        return notes.findByPlotIdOrderByObservedOnDescRecordedAtDesc(plotId.plotId()).stream()
                .map(FieldNoteRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public Optional<FieldNotePhoto> findPhoto(FieldNoteId id) {
        return photos.findById(UUID.fromString(id.value()))
                .map(entity -> new FieldNotePhoto(entity.getContent(), entity.getMediaType()));
    }

    private static FieldNotePersistenceEntity toEntity(FieldNoteSnapshot note) {
        var entity = new FieldNotePersistenceEntity();
        entity.setId(UUID.fromString(note.id().value()));
        entity.setPlotId(note.plotId().plotId());
        entity.setProducerId(note.producerId().producerId());
        entity.setType(note.type().name());
        entity.setText(note.text().value());
        entity.setObservedOn(note.observedOn());
        entity.setRecordedAt(note.recordedAt());
        entity.setPhotoMediaType(note.photoMediaType());
        return entity;
    }

    private static FieldNote toDomain(FieldNotePersistenceEntity entity) {
        return FieldNote.reconstitute(new FieldNoteSnapshot(
                new FieldNoteId(entity.getId().toString()),
                new PlotId(entity.getPlotId()),
                new ProducerId(entity.getProducerId()),
                FieldNoteType.of(entity.getType()),
                new FieldNoteText(entity.getText()),
                entity.getObservedOn(),
                entity.getRecordedAt(),
                entity.getPhotoMediaType()));
    }
}
