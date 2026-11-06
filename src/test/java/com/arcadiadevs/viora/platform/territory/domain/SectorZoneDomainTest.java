package com.arcadiadevs.viora.platform.territory.domain;

import com.arcadiadevs.viora.platform.territory.domain.exceptions.PlotSectorNotPendingException;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignment;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.PlotSectorAssignmentSnapshot;
import com.arcadiadevs.viora.platform.territory.domain.model.aggregates.SectorZone;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.LicenseId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorAssignmentId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.PlotSectorStatus;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.ProducerId;
import com.arcadiadevs.viora.platform.territory.domain.model.valueobjects.SectorZoneName;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Sector zone and plot sector domain unit tests")
class SectorZoneDomainTest {

    private static final Instant NOW = Instant.parse("2026-11-06T15:00:00Z");

    private final LicenseId license = new LicenseId(UUID.randomUUID().toString());
    private final PlotId plot = new PlotId(UUID.randomUUID().toString());
    private final ProducerId producer = new ProducerId(UUID.randomUUID().toString());

    @Test
    @DisplayName("A name is trimmed with single spaces, and accents, case and spaces do not make another sector")
    void namesCompareWithoutAccentsOrCase() {
        var name = new SectorZoneName("  Cerro   Blanco ");

        assertThat(name.value()).isEqualTo("Cerro Blanco");
        assertThat(name.normalized()).isEqualTo("cerro blanco");
        assertThat(name.sameSectorAs(new SectorZoneName("CERRO blanco"))).isTrue();
        assertThat(new SectorZoneName("Sámá").sameSectorAs(new SectorZoneName("sama"))).isTrue();
        assertThat(name.sameSectorAs(new SectorZoneName("Yarada Los Palos"))).isFalse();
    }

    @Test
    @DisplayName("A name has between 2 and 60 characters")
    void nameLength() {
        assertThatThrownBy(() -> new SectorZoneName(" ")).hasMessage("territory.sector_zone.name.blank");
        assertThatThrownBy(() -> new SectorZoneName(null)).hasMessage("territory.sector_zone.name.blank");
        assertThatThrownBy(() -> new SectorZoneName("A")).hasMessage("territory.sector_zone.name.length");
        assertThatThrownBy(() -> new SectorZoneName("x".repeat(61))).hasMessage("territory.sector_zone.name.length");
        assertThat(new SectorZoneName("x".repeat(60)).value()).hasSize(60);
    }

    @Test
    @DisplayName("A sector belongs to one license and keeps its identity when renamed")
    void renameKeepsTheSector() {
        var zone = SectorZone.create(license, new SectorZoneName("Magollo"));
        var id = zone.snapshot().id();

        zone.rename(new SectorZoneName("Magollo Alto"));

        assertThat(zone.snapshot().id()).isEqualTo(id);
        assertThat(zone.snapshot().name().value()).isEqualTo("Magollo Alto");
        assertThat(zone.belongsTo(license)).isTrue();
        assertThat(zone.belongsTo(new LicenseId(UUID.randomUUID().toString()))).isFalse();
    }

    @Test
    @DisplayName("Choosing a sector of the list assigns the plot at once")
    void choosingAssigns() {
        var zone = SectorZone.create(license, new SectorZoneName("Los Palos"));

        var assignment = PlotSectorAssignment.start(license, plot, producer, zone, null, NOW);

        var snapshot = assignment.snapshot();
        assertThat(snapshot.status()).isEqualTo(PlotSectorStatus.ASSIGNED);
        assertThat(snapshot.sectorZoneId()).isEqualTo(zone.snapshot().id());
        assertThat(snapshot.proposedName()).isNull();
        assertThat(snapshot.updatedAt()).isEqualTo(NOW);
        assertThat(assignment.isPending()).isFalse();
    }

    @Test
    @DisplayName("Writing a sector leaves the plot «Por confirmar» until the manager resolves it, only once")
    void writingLeavesItPendingUntilResolved() {
        var assignment = PlotSectorAssignment.start(license, plot, producer, null,
                new SectorZoneName("Yarada Los Palos"), NOW);
        assertThat(assignment.isPending()).isTrue();
        assertThat(assignment.snapshot().proposedName().value()).isEqualTo("Yarada Los Palos");
        var zone = SectorZone.create(license, new SectorZoneName("Los Palos"));

        assignment.resolve(zone, NOW.plusSeconds(60));

        assertThat(assignment.snapshot().status()).isEqualTo(PlotSectorStatus.ASSIGNED);
        assertThat(assignment.snapshot().sectorZoneId()).isEqualTo(zone.snapshot().id());
        assertThat(assignment.snapshot().proposedName()).isNull();
        assertThatThrownBy(() -> assignment.resolve(zone, NOW.plusSeconds(120)))
                .isInstanceOf(PlotSectorNotPendingException.class)
                .hasMessage("territory.plot_sector.not_pending");
    }

    @Test
    @DisplayName("The member can change the sector later, to the list or back to a written name")
    void memberChangesTheSector() {
        var zone = SectorZone.create(license, new SectorZoneName("Sama"));
        var assignment = PlotSectorAssignment.start(license, plot, producer, zone, null, NOW);

        assignment.change(null, new SectorZoneName("Cerro Blanco"), NOW.plusSeconds(5));

        assertThat(assignment.isPending()).isTrue();
        assertThat(assignment.snapshot().sectorZoneId()).isNull();
    }

    @Test
    @DisplayName("Exactly one of a zone or a written name, and never a zone of another license")
    void choiceRules() {
        var zone = SectorZone.create(license, new SectorZoneName("Sama"));
        var foreign = SectorZone.create(new LicenseId(UUID.randomUUID().toString()), new SectorZoneName("Sama"));

        assertThatThrownBy(() -> PlotSectorAssignment.start(license, plot, producer, null, null, NOW))
                .hasMessage("territory.plot_sector.choice.one_of");
        assertThatThrownBy(() -> PlotSectorAssignment.start(license, plot, producer, zone,
                new SectorZoneName("Sama"), NOW)).hasMessage("territory.plot_sector.choice.one_of");
        assertThatThrownBy(() -> PlotSectorAssignment.start(license, plot, producer, foreign, null, NOW))
                .hasMessage("territory.plot_sector.zone_of_another_license");
    }

    @Test
    @DisplayName("A stored row that mixes both states is rejected")
    void reconstituteRejectsMixedStates() {
        var mixed = new PlotSectorAssignmentSnapshot(new PlotSectorAssignmentId(), license, plot, producer,
                PlotSectorStatus.PENDING, SectorZone.create(license, new SectorZoneName("Sama")).snapshot().id(),
                null, NOW, 0L);

        assertThatThrownBy(() -> PlotSectorAssignment.reconstitute(mixed))
                .hasMessage("territory.plot_sector.snapshot.incomplete");
    }
}
