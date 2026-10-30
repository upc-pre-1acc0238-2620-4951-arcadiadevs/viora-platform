package com.arcadiadevs.viora.platform.thinning.application.acl;

import com.arcadiadevs.viora.platform.thinning.PrescriptionTestData;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.ExecutionConfirmationSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescriptionSnapshot;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberProjection;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CaliberProjectionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionStatus;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SustainableCropLoad;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CommercialSizeScale;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PrescriptionOutlook;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.YieldEstimate;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.YieldEstimateBasis;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.YieldEstimateStatus;
import com.arcadiadevs.viora.platform.thinning.application.internal.queryservices.ThinningPrescriptionOutlookService;
import com.arcadiadevs.viora.platform.thinning.domain.repositories.FruitThinningPrescriptionRepository;
import com.arcadiadevs.viora.platform.thinning.interfaces.acl.ThinningContextFacade.PrescriptionAvailability;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ThinningContextFacadeImpl prescription availability")
class ThinningContextFacadeImplTest {

    @Mock
    private FruitThinningPrescriptionRepository prescriptionRepository;
    @Mock
    private ThinningPrescriptionOutlookService outlookService;

    private ThinningContextFacadeImpl facade;
    private final PlotId plotId = PrescriptionTestData.newPlot();
    private final int campaign = PrescriptionTestData.CAMPAIGN.value();

    @BeforeEach
    void setUp() {
        facade = new ThinningContextFacadeImpl(prescriptionRepository, outlookService);
    }

    @Test
    @DisplayName("Reports an issued prescription with its percentage and the caliber projection status")
    void reportsAnIssuedPrescription() {
        var base = PrescriptionTestData.representative(plotId).snapshot();
        var confirmation = new ExecutionConfirmationSnapshot(null, LocalDate.now(), 30.0, 4, null, null, null, null,
                null, CaliberProjection.withoutEstimate(CaliberProjectionStatus.NOT_CALIBRATED, 0));
        given(withState(base, PrescriptionStatus.EXECUTED, 30.0, confirmation));

        assertThat(facade.findPrescriptionAvailability(plotId.plotId(), campaign))
                .contains(new PrescriptionAvailability(true, 30.0, 5, 0, "NOT_CALIBRATED"));
    }

    @Test
    @DisplayName("Reports a prescription still sampling with the trees it still needs")
    void reportsAnIncompleteSampling() {
        var prescription = PrescriptionTestData.sampling(plotId);
        prescription.ingestSamplingsBatch(
                new com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.UserId(
                        java.util.UUID.randomUUID().toString()),
                new com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingBatchId("batch-1"),
                java.util.List.of(
                        com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord
                                .create("T-01", 20, 12, 150.0, LocalDate.now()),
                        com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord
                                .create("T-02", 20, 12, 150.0, LocalDate.now())));
        when(prescriptionRepository.findByPlotIdAndCampaignYear(any(), any())).thenReturn(Optional.of(prescription));

        assertThat(facade.findPrescriptionAvailability(plotId.plotId(), campaign))
                .contains(new PrescriptionAvailability(false, null, 2, 3, null));
    }

    @Test
    @DisplayName("A representative sampling without a calculated load is not issued")
    void representativeSamplingIsNotIssuedYet() {
        when(prescriptionRepository.findByPlotIdAndCampaignYear(any(), any()))
                .thenReturn(Optional.of(PrescriptionTestData.representative(plotId)));

        assertThat(facade.findPrescriptionAvailability(plotId.plotId(), campaign))
                .contains(new PrescriptionAvailability(false, null, 5, 0, null));
    }

    @Test
    @DisplayName("Returns nothing without a prescription or for a malformed plot id")
    void returnsNothingWithoutPrescription() {
        when(prescriptionRepository.findByPlotIdAndCampaignYear(any(), any())).thenReturn(Optional.empty());

        assertThat(facade.findPrescriptionAvailability(plotId.plotId(), campaign)).isEmpty();
        assertThat(facade.findPrescriptionAvailability("not-a-uuid", campaign)).isEmpty();
        verify(prescriptionRepository, times(1)).findByPlotIdAndCampaignYear(any(), any());
    }

    @Test
    @DisplayName("Plan detail: the issued plan with the sampled load, the window, the caliber and the yield after thinning")
    void reportsThePlanDetail() {
        var base = PrescriptionTestData.representative(plotId).snapshot();
        var executedOn = LocalDate.of(2026, 11, 3);
        var projection = new CaliberProjection(CaliberProjectionStatus.ESTIMATED, 180.0, 160.0, 200.0, 0.8, 12,
                CaliberProjection.MODEL_VERSION, 0.2, 0.8);
        var confirmation = new ExecutionConfirmationSnapshot(null, executedOn, 30.0, 4, null, null, null, null,
                null, projection);
        var snapshot = withState(base, PrescriptionStatus.EXECUTED, 30.0, confirmation);
        given(snapshot);
        var sampledOn = LocalDate.of(2026, 10, 29);
        when(outlookService.outlookOf(snapshot)).thenReturn(new PrescriptionOutlook(0.57, 0.4, 1.43, null, 5, 0,
                sampledOn, 14, 49, YieldEstimate.unavailable(YieldEstimateStatus.INSUFFICIENT_HISTORY, 0)));
        when(outlookService.yieldAfterThinning(snapshot)).thenReturn(new YieldEstimate(YieldEstimateStatus.AVAILABLE,
                9000.0, 8200.0, YieldEstimateBasis.FRUIT_COUNT_ONLY, 3, 0.0));

        var detail = facade.findThinningPlanDetail(plotId.plotId(), campaign).orElseThrow();

        assertThat(detail.issued()).isTrue();
        assertThat(detail.percentageToRemove()).isEqualTo(30.0);
        assertThat(detail.preThinningFruitsPerShoot()).isEqualTo(0.57);
        assertThat(detail.targetFruitsPerShoot()).isEqualTo(0.4);
        assertThat(detail.windowOpensOn()).isEqualTo(snapshot.sustainableLoad().windowOpensOn());
        assertThat(detail.windowClosesOn()).isEqualTo(snapshot.sustainableLoad().windowClosesOn());
        assertThat(detail.sampledTrees()).isEqualTo(5);
        assertThat(detail.lastSampledOn()).isEqualTo(sampledOn);
        assertThat(detail.executedOn()).isEqualTo(executedOn);
        assertThat(detail.caliberProjectionStatus()).isEqualTo("ESTIMATED");
        assertThat(detail.mostLikelyFruitsPerKg()).isEqualTo(180.0);
        assertThat(detail.commercialSizeGrade()).isEqualTo(CommercialSizeScale.gradeOf(180.0));
        assertThat(detail.estimatedKgPerHaAfterThinning()).isEqualTo(8200.0);
    }

    @Test
    @DisplayName("Plan detail: a prescription not issued yet carries no plan figures nor caliber")
    void planDetailOfAPrescriptionNotIssued() {
        var prescription = PrescriptionTestData.representative(plotId);
        when(prescriptionRepository.findByPlotIdAndCampaignYear(any(), any())).thenReturn(Optional.of(prescription));
        when(outlookService.outlookOf(any())).thenReturn(new PrescriptionOutlook(0.6, 0.4, 1.5, null, 5, 0,
                LocalDate.now(), 14, 49, YieldEstimate.unavailable(YieldEstimateStatus.INSUFFICIENT_HISTORY, 0)));

        var detail = facade.findThinningPlanDetail(plotId.plotId(), campaign).orElseThrow();

        assertThat(detail.issued()).isFalse();
        assertThat(detail.percentageToRemove()).isNull();
        assertThat(detail.preThinningFruitsPerShoot()).isEqualTo(0.6);
        assertThat(detail.executedOn()).isNull();
        assertThat(detail.caliberProjectionStatus()).isNull();
        assertThat(detail.mostLikelyFruitsPerKg()).isNull();
        assertThat(detail.commercialSizeGrade()).isNull();
        assertThat(detail.estimatedKgPerHaAfterThinning()).isNull();
        verify(outlookService, never()).yieldAfterThinning(any());
    }

    @Test
    @DisplayName("Plan detail: nothing without a prescription or for a malformed plot id")
    void planDetailWithoutPrescription() {
        when(prescriptionRepository.findByPlotIdAndCampaignYear(any(), any())).thenReturn(Optional.empty());

        assertThat(facade.findThinningPlanDetail(plotId.plotId(), campaign)).isEmpty();
        assertThat(facade.findThinningPlanDetail("not-a-uuid", campaign)).isEmpty();
        verify(outlookService, never()).outlookOf(any());
    }

    private void given(FruitThinningPrescriptionSnapshot snapshot) {
        var prescription = mock(FruitThinningPrescription.class);
        when(prescription.snapshot()).thenReturn(snapshot);
        when(prescriptionRepository.findByPlotIdAndCampaignYear(any(), any())).thenReturn(Optional.of(prescription));
    }

    private static FruitThinningPrescriptionSnapshot withState(FruitThinningPrescriptionSnapshot base,
            PrescriptionStatus status, Double percentage, ExecutionConfirmationSnapshot confirmation) {
        var load = new SustainableCropLoad(0.4, percentage, LocalDate.now(), LocalDate.now().plusDays(10),
                "test-1", "SYNTHETIC_DEMO");
        return new FruitThinningPrescriptionSnapshot(base.id(), base.plotId(), base.campaignYear(),
                base.observedPlotRevision(), status, load, null, base.samplingRounds(), confirmation, 0L, null);
    }
}
