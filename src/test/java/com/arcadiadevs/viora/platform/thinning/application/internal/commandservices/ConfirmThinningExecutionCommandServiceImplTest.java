package com.arcadiadevs.viora.platform.thinning.application.internal.commandservices;

import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.acl.ExternalOrchardService;
import com.arcadiadevs.viora.platform.thinning.domain.model.commands.ConfirmThinningExecutionCommand;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.ThinningExecutionConfirmedEvent;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberProjectionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.CaliberCalibrationObservationRepository;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import static com.arcadiadevs.viora.platform.thinning.ThinningExecutionFixtures.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfirmThinningExecutionCommandServiceImplTest {
    private final FruitThinningPrescriptionRepository repository = mock(FruitThinningPrescriptionRepository.class);
    private final CaliberCalibrationObservationRepository observations = mock(CaliberCalibrationObservationRepository.class);
    private final ExternalOrchardService orchard = mock(ExternalOrchardService.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final ConfirmThinningExecutionCommandServiceImpl service =
            new ConfirmThinningExecutionCommandServiceImpl(repository, observations, orchard, publisher);
    private final LocalDate date = LocalDate.of(2026, 1, 1);
    private ConfirmThinningExecutionCommand command;

    @BeforeEach
    void setUp() {
        command = new ConfirmThinningExecutionCommand(UUID.randomUUID().toString(), date, 25.0, 420.0, 4, null);
    }

    @Test
    void savesBeforePublishingAndClearsEvents() {
        var prescription = prescribed(date);
        when(repository.findByIdForUpdate(any())).thenReturn(Optional.of(prescription));
        when(repository.save(prescription)).thenReturn(prescription);
        assertTrue(service.handle(command).isSuccess());
        var order = inOrder(repository, publisher);
        order.verify(repository).findByIdForUpdate(any());
        order.verify(repository).save(prescription);
        order.verify(publisher).publishEvent(any(ThinningExecutionConfirmedEvent.class));
        assertTrue(prescription.domainEvents().isEmpty());
    }

    @Test
    void returnsNotFound() {
        when(repository.findByIdForUpdate(any())).thenReturn(Optional.empty());
        assertEquals("THINNINGPRESCRIPTION_NOT_FOUND", service.handle(command).failure().orElseThrow().code());
        verify(repository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void rejectsRepeatWithoutSaveOrEvent() {
        var prescription = prescribed(date);
        prescription.confirmExecution(date, 1, 25, 1, null);
        prescription.clearDomainEvents();
        when(repository.findByIdForUpdate(any())).thenReturn(Optional.of(prescription));
        assertTrue(service.handle(command).failure().orElseThrow().code().endsWith("_CONFLICT"));
        verify(repository, never()).save(any());
        verifyNoInteractions(publisher);
    }

    @Test
    void projectsCaliberWithTheObservationsOfThePlotVariety() {
        var prescription = prescribed(date);
        when(repository.findByIdForUpdate(any())).thenReturn(Optional.of(prescription));
        when(repository.save(prescription)).thenReturn(prescription);
        when(orchard.findPlotVariety(prescription.snapshot().plotId())).thenReturn(Optional.of("SEVILLANA"));
        when(observations.findByVariety("SEVILLANA")).thenReturn(exactObservations("SEVILLANA"));
        var projection = service.handle(command).success().orElseThrow()
                .snapshot().executionConfirmation().caliberProjection();
        assertEquals(CaliberProjectionStatus.ESTIMATED, projection.status());
        assertEquals(103.0, projection.mostLikelyFruitsPerKg(), 0.05);
    }

    @Test
    void leavesCaliberUncalibratedWhenThePlotVarietyIsUnknown() {
        var prescription = prescribed(date);
        when(repository.findByIdForUpdate(any())).thenReturn(Optional.of(prescription));
        when(repository.save(prescription)).thenReturn(prescription);
        when(orchard.findPlotVariety(any())).thenReturn(Optional.empty());
        var projection = service.handle(command).success().orElseThrow()
                .snapshot().executionConfirmation().caliberProjection();
        assertEquals(CaliberProjectionStatus.NOT_CALIBRATED, projection.status());
        assertEquals(0, projection.calibrationObservations());
        verifyNoInteractions(observations);
    }

    @Test
    void doesNotPublishWhenSavingFails() {
        var prescription = prescribed(date);
        when(repository.findByIdForUpdate(any())).thenReturn(Optional.of(prescription));
        when(repository.save(any())).thenThrow(new org.springframework.dao.DataIntegrityViolationException("failure"));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () -> service.handle(command));
        verifyNoInteractions(publisher);
    }
}
