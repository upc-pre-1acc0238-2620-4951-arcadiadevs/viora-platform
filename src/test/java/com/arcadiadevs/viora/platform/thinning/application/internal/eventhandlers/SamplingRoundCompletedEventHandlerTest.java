package com.arcadiadevs.viora.platform.thinning.application.internal.eventhandlers;

import com.arcadiadevs.viora.platform.thinning.PrescriptionTestData;
import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.ThinningPrescriptionIssuer;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.events.SamplingRoundCompletedEvent;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionId;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SamplingRoundCompletedEventHandlerTest {

    @Mock
    private FruitThinningPrescriptionRepository repository;

    @Mock
    private ThinningPrescriptionIssuer issuer;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SamplingRoundCompletedEvent eventOf(FruitThinningPrescription prescription) {
        var snapshot = prescription.snapshot();
        return new SamplingRoundCompletedEvent(
                snapshot.id().prescriptionId(), snapshot.plotId().plotId(), 5, Instant.now());
    }

    @Test
    @DisplayName("Saves the prescription when the sampling completes and it can be issued")
    void savesWhenIssued() {
        var prescription = PrescriptionTestData.representative(PrescriptionTestData.newPlot());
        when(repository.findByIdForUpdate(any(PrescriptionId.class))).thenReturn(Optional.of(prescription));
        when(issuer.issueIfReady(prescription, false)).thenReturn(true);

        new SamplingRoundCompletedEventHandler(repository, issuer, eventPublisher).on(eventOf(prescription));

        verify(repository).save(prescription);
    }

    @Test
    @DisplayName("Saves nothing when an input is still missing")
    void savesNothingWhenNotReady() {
        var prescription = PrescriptionTestData.representative(PrescriptionTestData.newPlot());
        when(repository.findByIdForUpdate(any(PrescriptionId.class))).thenReturn(Optional.of(prescription));
        when(issuer.issueIfReady(prescription, false)).thenReturn(false);

        new SamplingRoundCompletedEventHandler(repository, issuer, eventPublisher).on(eventOf(prescription));

        verify(repository, never()).save(any(FruitThinningPrescription.class));
    }

    @Test
    @DisplayName("A failure while issuing never breaks the sampling that triggered it")
    void swallowsIssuingFailures() {
        var prescription = PrescriptionTestData.representative(PrescriptionTestData.newPlot());
        when(repository.findByIdForUpdate(any(PrescriptionId.class))).thenReturn(Optional.of(prescription));
        when(issuer.issueIfReady(prescription, false)).thenThrow(new IllegalStateException("thinning.sampling.not_representative"));

        new SamplingRoundCompletedEventHandler(repository, issuer, eventPublisher).on(eventOf(prescription));

        verify(repository, never()).save(any(FruitThinningPrescription.class));
    }
}
