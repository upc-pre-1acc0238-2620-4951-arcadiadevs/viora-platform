package com.arcadiadevs.viora.platform.settlement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Agronomic dossier a producer sealed for a settled campaign (US30): the values its PDF states and the SHA-256 that
 * proves the PDF was not changed. Every value is null when the platform never had it.
 */
@Schema(name = "AgronomicDossierResource", description = "Sealed agronomic dossier of a settled campaign")
public record AgronomicDossierResource(
        @Schema(description = "Dossier UUID") String id,
        @Schema(description = "Plot UUID") String plotId,
        @Schema(description = "Owner producer UUID") String producerId,
        @Schema(description = "Sealed campaign", example = "2026") Integer campaignYear,
        @Schema(description = "Plot name", example = "La Yarada 02") String plotName,
        @Schema(description = "Olive variety", example = "SEVILLANA") String variety,
        @Schema(description = "Plot area in hectares", example = "2.5") Double areaHectares,
        @Schema(description = "Crop load before thinning") FruitLoad fruitLoad,
        @Schema(description = "Thinning prescription of the campaign") Prescription prescription,
        @Schema(description = "Thinning labor recorded") Thinning thinning,
        @Schema(description = "Settled harvest") Harvest harvest,
        @Schema(description = "Climate data behind the campaign") Climate climate,
        @Schema(description = "Harvest history the dossier includes") History history,
        @Schema(description = "SHA-256 of the stored PDF, 64 lowercase hexadecimal characters") String verificationHash,
        @Schema(description = "Instant of the seal") Instant sealedAt,
        @Schema(description = "Pages of the PDF", example = "2") Integer pageCount,
        @Schema(description = "Size of the PDF in bytes", example = "4812") Integer sizeBytes,
        @Schema(description = "Suggested file name", example = "Expediente_LaYarada02_2026.pdf") String fileName,
        @Schema(description = "Agronomist certification of the same campaign (ADR-002), null when it has none",
                nullable = true) Certification certification
) {

    /**
     * @param preThinningFruitsPerShoot mean load of the representative sampling
     * @param targetFruitsPerShoot      sustainable target
     * @param overTargetPercentage      fruit above the target, in percent
     * @param loadUnit                  always {@code FRUITS_PER_SHOOT}
     */
    public record FruitLoad(Double preThinningFruitsPerShoot, Double targetFruitsPerShoot, Double overTargetPercentage,
            String loadUnit) {
    }

    /**
     * @param removalPercentage prescribed removal
     * @param windowOpensOn     first recommended thinning date
     * @param windowClosesOn    latest recommended thinning date
     */
    public record Prescription(Double removalPercentage, LocalDate windowOpensOn, LocalDate windowClosesOn) {
    }

    /**
     * @param actualRemovalPercentage percentage actually removed
     * @param executedOn              date of the labor
     * @param compliance              {@code EXECUTED_ON_TIME}, {@code EXECUTED_LATE} or {@code NOT_RECORDED}
     */
    public record Thinning(Double actualRemovalPercentage, LocalDate executedOn, String compliance) {
    }

    /**
     * @param totalKg       settled kilograms
     * @param greenKg       green olives
     * @param blackKg       black olives
     * @param kgPerHectare  settled kilograms per hectare; null without area
     * @param receiptNumber settlement receipt number
     * @param weighedOn     weighing date at the mill
     */
    public record Harvest(Double totalKg, Double greenKg, Double blackKg, Double kgPerHectare, String receiptNumber,
            LocalDate weighedOn) {
    }

    /**
     * @param linkedNodes      climate nodes linked to the plot
     * @param daysWithReadings days with readings from the first of January to the settlement
     */
    public record Climate(Integer linkedNodes, Integer daysWithReadings) {
    }

    /**
     * @param fromYear  first harvested campaign included
     * @param toYear    last harvested campaign included
     * @param campaigns harvested campaigns included
     */
    public record History(Integer fromYear, Integer toYear, Integer campaigns) {
    }

    /**
     * @param certifiedBy      certifying professional
     * @param cipNumber        collegiate number
     * @param certifiedAt      instant of the certification
     * @param verificationHash SHA-256 of the certified PDF, a different document from the sealed one
     */
    public record Certification(String certifiedBy, String cipNumber, Instant certifiedAt, String verificationHash) {
    }
}
