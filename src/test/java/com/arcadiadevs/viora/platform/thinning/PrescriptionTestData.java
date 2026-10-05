package com.arcadiadevs.viora.platform.thinning;

import com.arcadiadevs.viora.platform.thinning.application.internal.outboundservices.ThinningProfile;
import com.arcadiadevs.viora.platform.thinning.domain.model.aggregates.FruitThinningPrescription;
import com.arcadiadevs.viora.platform.thinning.domain.model.entities.TreeSamplingRecord;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.CampaignYear;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.SamplingBatchId;
import com.arcadiadevs.viora.platform.thinning.domain.model.valueobjects.UserId;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Small builders shared by the prescription tests. */
public final class PrescriptionTestData {

    /** Campaign every test prescription belongs to. */
    public static final CampaignYear CAMPAIGN = new CampaignYear(2026);

    private PrescriptionTestData() { }

    /** A technical profile with synthetic values, used only by tests. */
    public static ThinningProfile profile() {
        return new ThinningProfile("SEVILLANA", 0.4, 14, 49, "SYNTHETIC_DEMO", "test-1", "unit test", "tests");
    }

    /** A prescription that is still gathering samples. */
    public static FruitThinningPrescription sampling(PlotId plotId) {
        return FruitThinningPrescription.createForPlot(plotId, CAMPAIGN, 1L);
    }

    /** A prescription whose sampling is representative: five trees, 12 fruits on 20 shoots each (0.6 per shoot). */
    public static FruitThinningPrescription representative(PlotId plotId) {
        var prescription = sampling(plotId);
        prescription.ingestSamplingsBatch(
                new UserId(UUID.randomUUID().toString()),
                new SamplingBatchId("batch-" + UUID.randomUUID()),
                List.of(
                        TreeSamplingRecord.create("T-01", 20, 12, 150.0, LocalDate.now()),
                        TreeSamplingRecord.create("T-02", 20, 12, 150.0, LocalDate.now()),
                        TreeSamplingRecord.create("T-03", 20, 12, 150.0, LocalDate.now()),
                        TreeSamplingRecord.create("T-04", 20, 12, 150.0, LocalDate.now()),
                        TreeSamplingRecord.create("T-05", 20, 12, 150.0, LocalDate.now())
                )
        );
        prescription.clearDomainEvents();
        return prescription;
    }

    public static PlotId newPlot() {
        return new PlotId(UUID.randomUUID().toString());
    }
}
