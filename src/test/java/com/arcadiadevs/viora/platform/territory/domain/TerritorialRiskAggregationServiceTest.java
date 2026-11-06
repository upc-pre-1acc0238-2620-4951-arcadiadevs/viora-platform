package com.arcadiadevs.viora.platform.territory.domain;

import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.DailyMinimum;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.GeoPoint;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotRiskAssessment;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotRiskSignals;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskLevel;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.RiskReasonType;
import com.arcadiadevs.viora.platform.territory.domain.services.TerritorialRiskAggregationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Territorial traffic light unit tests")
class TerritorialRiskAggregationServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 6);

    @Test
    @DisplayName("Overload above 30 % is red with its percentage; 30 % or less with a valid sampling is green")
    void overloadThreshold() {
        var red = assess(sampled(2.0, 1.4));
        assertThat(red.level()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(red.overloadPercent()).isEqualTo(42.9);
        assertThat(red.reasons()).singleElement()
                .satisfies(reason -> assertThat(reason.type()).isEqualTo(RiskReasonType.OVERLOAD));

        var withinTarget = assess(sampled(1.3, 1.0));
        assertThat(withinTarget.level()).isEqualTo(RiskLevel.OPTIMAL);
        assertThat(withinTarget.overloadPercent()).isEqualTo(30.0);
        assertThat(assess(sampled(1.0, 1.2)).overloadPercent()).isZero();
    }

    @Test
    @DisplayName("Registering the thinning settles the overload")
    void thinningSettlesTheOverload() {
        var signals = new PlotRiskSignals(true, 2.0, 1.4, true, List.of(), List.of(), List.of(), TODAY);

        var assessment = assess(signals);

        assertThat(assessment.level()).isEqualTo(RiskLevel.OPTIMAL);
        assertThat(assessment.overloadPercent()).isZero();
    }

    @Test
    @DisplayName("Without a valid sampling the plot is grey, never green, and has no overload")
    void withoutSamplingIsGrey() {
        var assessment = assess(new PlotRiskSignals(false, null, null, false, List.of(), List.of(), List.of(),
                TODAY));

        assertThat(assessment.level()).isEqualTo(RiskLevel.NO_DATA);
        assertThat(assessment.overloadPercent()).isNull();
        assertThat(assessment.reasons()).isEmpty();
    }

    @Test
    @DisplayName("An open critical incident is red and a warning one amber, sampled or not")
    void incidents() {
        var critical = assess(new PlotRiskSignals(false, null, null, false, List.of("HEAT_WAVE"), List.of(),
                List.of(), TODAY));
        assertThat(critical.level()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(critical.reasons().getFirst().incidentType()).isEqualTo("HEAT_WAVE");

        var warning = assess(new PlotRiskSignals(true, 1.0, 1.0, false, List.of(), List.of("HYDRIC_STRESS"),
                List.of(), TODAY));
        assertThat(warning.level()).isEqualTo(RiskLevel.MODERATE);
        assertThat(warning.reasons()).singleElement()
                .satisfies(reason -> assertThat(reason.type()).isEqualTo(RiskReasonType.WARNING_INCIDENT));
    }

    @Test
    @DisplayName("A forecast minimum of 1.5 °C or less today or tomorrow is red; later days do not count")
    void frostForecast() {
        var frost = assess(withForecast(new DailyMinimum(TODAY, 4.0), new DailyMinimum(TODAY.plusDays(1), 1.2)));
        assertThat(frost.level()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(frost.reasons()).singleElement().satisfies(reason -> {
            assertThat(reason.type()).isEqualTo(RiskReasonType.FROST_FORECAST);
            assertThat(reason.value()).isEqualTo(1.2);
            assertThat(reason.date()).isEqualTo(TODAY.plusDays(1));
        });

        assertThat(assess(withForecast(new DailyMinimum(TODAY, 1.5))).level()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(assess(withForecast(new DailyMinimum(TODAY, 1.6))).level()).isEqualTo(RiskLevel.OPTIMAL);
        assertThat(assess(withForecast(new DailyMinimum(TODAY.plusDays(2), -1.0))).level())
                .isEqualTo(RiskLevel.OPTIMAL);
        assertThat(assess(withForecast(new DailyMinimum(TODAY.minusDays(1), -1.0))).level())
                .isEqualTo(RiskLevel.OPTIMAL);
    }

    @Test
    @DisplayName("Red reasons come before a warning that also applies")
    void reasonsOrder() {
        var assessment = assess(new PlotRiskSignals(true, 2.0, 1.0, false, List.of(), List.of("HYDRIC_STRESS"),
                List.of(), TODAY));

        assertThat(assessment.level()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(assessment.reasons()).extracting(reason -> reason.type())
                .containsExactly(RiskReasonType.OVERLOAD, RiskReasonType.WARNING_INCIDENT);
    }

    @Test
    @DisplayName("Visit order: red by larger overload, amber, grey, green; on a tie the closest, unknown last")
    void visitOrder() {
        var redBig = new PlotRiskAssessment(RiskLevel.CRITICAL, List.of(), 43.0);
        var redSmall = new PlotRiskAssessment(RiskLevel.CRITICAL, List.of(), 34.0);
        var amber = new PlotRiskAssessment(RiskLevel.MODERATE, List.of(), 0.0);
        var grey = new PlotRiskAssessment(RiskLevel.NO_DATA, List.of(), null);
        var green = new PlotRiskAssessment(RiskLevel.OPTIMAL, List.of(), 0.0);
        record Entry(String name, PlotRiskAssessment assessment, Double distance) {
        }
        var entries = new ArrayList<>(List.of(new Entry("green", green, 0.1), new Entry("grey", grey, 0.1),
                new Entry("amber far", amber, 9.0), new Entry("amber unknown", amber, null),
                new Entry("amber near", amber, 1.0), new Entry("red small", redSmall, 0.1),
                new Entry("red big", redBig, 9.0)));

        entries.sort((first, second) -> TerritorialRiskAggregationService.comparePriority(first.assessment(),
                first.distance(), second.assessment(), second.distance()));

        assertThat(entries).extracting(Entry::name).containsExactly("red big", "red small", "amber near",
                "amber far", "amber unknown", "grey", "green");
    }

    @Test
    @DisplayName("Distances follow the Earth's surface and coordinates must be in range")
    void geoPoints() {
        var tacna = new GeoPoint(-18.0146, -70.2536);
        var yarada = new GeoPoint(-18.1, -70.4);

        assertThat(tacna.distanceKmTo(yarada)).isBetween(17.0, 19.0);
        assertThatThrownBy(() -> new GeoPoint(-91, 0)).hasMessage("territory.location.latitude.invalid");
        assertThatThrownBy(() -> new GeoPoint(0, 181)).hasMessage("territory.location.longitude.invalid");
    }

    private static PlotRiskAssessment assess(PlotRiskSignals signals) {
        return TerritorialRiskAggregationService.assess(signals);
    }

    private static PlotRiskSignals sampled(double pre, double target) {
        return new PlotRiskSignals(true, pre, target, false, List.of(), List.of(), List.of(), TODAY);
    }

    private static PlotRiskSignals withForecast(DailyMinimum... days) {
        return new PlotRiskSignals(true, 1.0, 1.0, false, List.of(), List.of(), List.of(days), TODAY);
    }
}
