package com.arcadiadevs.viora.platform.phenology.domain.model;

import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ChillSeasonDetails;
import com.arcadiadevs.viora.platform.phenology.domain.model.valueobjects.ChillSeasonState;
import com.arcadiadevs.viora.platform.phenology.domain.services.ErezDynamicModelCalculator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.Month;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ChillSeasonDetails Value Object and Season Domain Calculations Tests")
class ChillSeasonDetailsTest {

    @Test
    @DisplayName("Should create valid ChillSeasonDetails when all valid invariants are provided")
    void shouldCreateValidChillSeasonDetails() {
        var seasonStart = LocalDate.of(2026, Month.JUNE, 1);
        var completionDate = LocalDate.of(2026, Month.AUGUST, 18);
        var details = new ChillSeasonDetails(seasonStart, completionDate, 0, ChillSeasonState.COMPLETED);

        assertThat(details.seasonStart()).isEqualTo(seasonStart);
        assertThat(details.completionDate()).isEqualTo(completionDate);
        assertThat(details.idleDays()).isEqualTo(0);
        assertThat(details.seasonState()).isEqualTo(ChillSeasonState.COMPLETED);
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException with normalized i18n key when seasonStart is null")
    void shouldThrowWhenSeasonStartIsNull() {
        assertThatThrownBy(() -> new ChillSeasonDetails(null, null, 0, ChillSeasonState.IN_PROGRESS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phenology.season_start.null");
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException with normalized i18n key when seasonState is null")
    void shouldThrowWhenSeasonStateIsNull() {
        var seasonStart = LocalDate.of(2026, Month.JUNE, 1);
        assertThatThrownBy(() -> new ChillSeasonDetails(seasonStart, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("phenology.season_state.null");
    }

    @Test
    @DisplayName("Should default negative or null idleDays to 0")
    void shouldDefaultNegativeOrNullIdleDaysToZero() {
        var seasonStart = LocalDate.of(2026, Month.JUNE, 1);

        var detailsNegative = new ChillSeasonDetails(seasonStart, null, -5, ChillSeasonState.IN_PROGRESS);
        assertThat(detailsNegative.idleDays()).isEqualTo(0);

        var detailsNull = new ChillSeasonDetails(seasonStart, null, null, ChillSeasonState.IN_PROGRESS);
        assertThat(detailsNull.idleDays()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should determine COMPLETED season state when portions reach or exceed target threshold")
    void shouldDetermineCompletedSeasonState() {
        var dateWithinWinter = LocalDate.of(2026, Month.JULY, 15);
        var state = ErezDynamicModelCalculator.determineSeasonState(28.5, 27.0, dateWithinWinter);

        assertThat(state).isEqualTo(ChillSeasonState.COMPLETED);
    }

    @Test
    @DisplayName("Should determine IN_PROGRESS season state when within winter window and portions < threshold")
    void shouldDetermineInProgressSeasonState() {
        var dateWithinWinter = LocalDate.of(2026, Month.JULY, 15);
        var state = ErezDynamicModelCalculator.determineSeasonState(14.2, 27.0, dateWithinWinter);

        assertThat(state).isEqualTo(ChillSeasonState.IN_PROGRESS);
    }

    @Test
    @DisplayName("Should determine OFF_SEASON season state when outside winter window and portions < threshold")
    void shouldDetermineOffSeasonSeasonState() {
        var dateOutsideWinter = LocalDate.of(2026, Month.NOVEMBER, 10);
        var state = ErezDynamicModelCalculator.determineSeasonState(10.0, 27.0, dateOutsideWinter);

        assertThat(state).isEqualTo(ChillSeasonState.OFF_SEASON);
    }

    @Test
    @DisplayName("Should build complete ChillSeasonDetails correctly for completed campaign")
    void shouldBuildCompleteChillSeasonDetailsForCompletedCampaign() {
        var details = ErezDynamicModelCalculator.buildSeasonDetails(
                2026,
                28.5,
                27.0,
                LocalDate.of(2026, Month.SEPTEMBER, 1),
                0,
                LocalDate.of(2026, Month.AUGUST, 18)
        );

        assertThat(details.seasonStart()).isEqualTo(LocalDate.of(2026, Month.JUNE, 1));
        assertThat(details.completionDate()).isEqualTo(LocalDate.of(2026, Month.AUGUST, 18));
        assertThat(details.idleDays()).isEqualTo(0);
        assertThat(details.seasonState()).isEqualTo(ChillSeasonState.COMPLETED);
    }
}
