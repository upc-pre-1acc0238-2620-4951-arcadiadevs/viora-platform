package com.arcadiadevs.viora.platform.assistant.domain.model.valueobjects;

import java.time.LocalDate;
import java.util.List;
import java.util.SortedMap;

/**
 * The figures of a plot an answer is written from. Each figure is already computed by the context that owns it; the
 * assistant only reads them. A figure the plot does not have yet is null (or empty), and the question that needs it
 * is not offered.
 *
 * @param campaignYear                current campaign year
 * @param areaHectares                area of the plot, to turn kilograms into tonnes per hectare
 * @param yieldsKg                    kilograms harvested per campaign year, ascending
 * @param bearingIndex                latest Biennial Bearing Index
 * @param bearingCategory             alternation band of that index
 * @param preThinningFruitsPerShoot   load of the representative sampling, before thinning
 * @param targetFruitsPerShoot        load the tree sustains, from the issued prescription
 * @param percentageToRemove          prescribed percentage of fruits to remove
 * @param windowOpensOn               first recommended thinning day
 * @param windowClosesOn              last recommended thinning day
 * @param sampledTrees                distinct trees sampled in the campaign
 */
public record AnswerFacts(
        int campaignYear,
        Double areaHectares,
        SortedMap<Integer, Double> yieldsKg,
        Double bearingIndex,
        String bearingCategory,
        Double preThinningFruitsPerShoot,
        Double targetFruitsPerShoot,
        Double percentageToRemove,
        LocalDate windowOpensOn,
        LocalDate windowClosesOn,
        int sampledTrees
) {

    /**
     * One harvested campaign in tonnes per hectare.
     *
     * @param year         campaign year
     * @param tonnesPerHa  tonnes per hectare
     */
    public record Campaign(int year, double tonnesPerHa) {
    }

    /**
     * The campaigns with a harvest, oldest first, in tonnes per hectare.
     *
     * @return the campaigns; empty while the plot has no harvest or no area to divide by
     */
    public List<Campaign> campaigns() {
        if (yieldsKg == null || areaHectares == null || areaHectares <= 0.0) {
            return List.of();
        }
        return yieldsKg.entrySet().stream()
                .map(entry -> new Campaign(entry.getKey(), entry.getValue() / areaHectares / 1000.0))
                .toList();
    }
}
